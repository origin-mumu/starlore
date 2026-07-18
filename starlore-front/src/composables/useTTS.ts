import { ref } from 'vue'
import { getAuthToken } from '@/utils/authToken'

/**
 * TTS 语音合成与播放 composable
 * 支持两种方式播放：
 * 1. 后端 MiMo TTS API 返回的音频
 * 2. 浏览器内置 SpeechSynthesis（Web Speech API）作为兜底
 */

// 文本分句：按句子结束标点切分
const SENTENCE_END = /[。！？.!?\n]+/

export function useTTS() {
  const ttsEnabled = ref(false)
  const isSpeaking = ref(false)

  // 播放队列
  let audioQueue: Array<{ text: string }> = []
  let isPlaying = false
  let audioContext: AudioContext | null = null
  let currentSource: AudioBufferSourceNode | null = null

  // 浏览器内置 TTS 实例
  let speechSynth: SpeechSynthesis | null = null
  let currentUtterance: SpeechSynthesisUtterance | null = null

  // 预请求状态
  let prefetchedAudio: { text: string; arrayBuffer: ArrayBuffer } | null = null
  let prefetchAbortController: AbortController | null = null

  function getAudioContext(): AudioContext {
    if (!audioContext) {
      audioContext = new AudioContext()
    }
    return audioContext
  }

  /** 取消预请求 */
  function cancelPrefetch() {
    if (prefetchAbortController) {
      prefetchAbortController.abort()
      prefetchAbortController = null
    }
    prefetchedAudio = null
  }

  /** 纯获取 TTS 音频（不播放），用于预加载 */
  async function fetchTtsAudio(text: string): Promise<ArrayBuffer> {
    const token = getAuthToken()
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }

    const response = await fetch('/api/ai/tts', {
      method: 'POST',
      headers,
      body: JSON.stringify({ text }),
    })

    if (!response.ok) {
      throw new Error(`TTS API 请求失败: ${response.status}`)
    }

    const contentType = response.headers.get('content-type') || ''
    if (contentType.includes('application/json') || contentType.includes('text/')) {
      throw new Error('非音频响应，跳过预加载')
    }

    const arrayBuffer = await response.arrayBuffer()
    if (arrayBuffer.byteLength < 100) {
      throw new Error('音频数据过小')
    }
    return arrayBuffer
  }

  /** 发起下一句的预请求 */
  function startPrefetch(text: string) {
    cancelPrefetch()

    prefetchAbortController = new AbortController()
    const controller = prefetchAbortController

    ;(async () => {
      try {
        const arrayBuffer = await fetchTtsAudio(text)
        if (controller.signal.aborted) return
        prefetchedAudio = { text, arrayBuffer }
        console.log('[TTS] 🔄 预加载完成:', text.substring(0, 30) + '...')
      } catch (e: any) {
        if (e.name === 'AbortError') return
        console.warn('[TTS] 预请求失败:', e.message || e)
      } finally {
        if (prefetchAbortController === controller) {
          prefetchAbortController = null
        }
      }
    })()
  }

  /** 停止当前播放并清空队列 */
  function stopAndClear() {
    cancelPrefetch()
    // 停止所有浏览器 TTS
    if (speechSynth) {
      speechSynth.cancel()
    }
    // 停止 Web Audio 播放
    if (currentSource) {
      try {
        currentSource.stop()
      } catch { /* ignore */ }
      currentSource = null
    }
    audioQueue = []
    isPlaying = false
    isSpeaking.value = false
    currentUtterance = null
  }

  /** 通过后端 MiMo TTS API 获取音频并播放 */
  async function playViaApi(text: string): Promise<boolean> {
    try {
      const token = getAuthToken()
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      }

      const response = await fetch('/api/ai/tts', {
        method: 'POST',
        headers,
        body: JSON.stringify({ text }),
      })

      if (!response.ok) {
        console.warn('[TTS] API 请求失败:', response.status)
        return false
      }

      const contentType = response.headers.get('content-type') || ''

      // 如果是 JSON 响应（错误或文本类型兜底）
      if (contentType.includes('application/json') || contentType.includes('text/')) {
        const text = await response.text()
        try {
          const json = JSON.parse(text)
          if (json.error) {
            console.warn('[TTS] API 错误:', json.error)
            return false
          }
          if (json.type === 'text' && json.content) {
            playViaBrowserTTS(json.content)
            return true
          }
          return false
        } catch {
          // JSON 解析失败，忽略
          return false
        }
      }

      // 二进制音频响应
      const arrayBuffer = await response.arrayBuffer()
      if (arrayBuffer.byteLength < 100) {
        // 太小，可能是个错误JSON，尝试解析
        try {
          const text = new TextDecoder().decode(arrayBuffer)
          const json = JSON.parse(text)
          if (json.type === 'text' && json.content) {
            playViaBrowserTTS(json.content)
            return true
          }
        } catch { /* ignore */ }
        console.warn('[TTS] 音频数据过小:', arrayBuffer.byteLength, 'bytes')
        return false
      }

      await playAudioBuffer(arrayBuffer)
      return true
    } catch (e) {
      console.warn('[TTS] API 异常:', e)
      return false
    }
  }

  /** 使用 Web Audio API 播放音频 buffer */
  async function playAudioBuffer(arrayBuffer: ArrayBuffer): Promise<void> {
    const ctx = getAudioContext()
    // 如果 AudioContext 被暂停（浏览器策略），恢复它
    if (ctx.state === 'suspended') {
      await ctx.resume()
    }

    return new Promise((resolve, reject) => {
      try {
        ctx.decodeAudioData(arrayBuffer.slice(0), (audioBuffer) => {
          const source = ctx.createBufferSource()
          source.buffer = audioBuffer
          source.connect(ctx.destination)
          currentSource = source

          source.onended = () => {
            currentSource = null
            resolve()
          }

          source.start(0)
        }, (err) => {
          console.warn('[TTS] 音频解码失败:', err)
          reject(err)
        })
      } catch (e) {
        reject(e)
      }
    })
  }

  /** 浏览器内置 TTS 兜底播放 */
  function playViaBrowserTTS(text: string): void {
    if (!('speechSynthesis' in window)) {
      console.warn('[TTS] 浏览器不支持 SpeechSynthesis')
      return
    }

    speechSynth = window.speechSynthesis
    // 取消之前的播放
    speechSynth.cancel()

    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    utterance.rate = 1.0
    utterance.pitch = 1.0
    utterance.volume = 1.0
    currentUtterance = utterance

    utterance.onend = () => {
      currentUtterance = null
      processQueue()
    }

    utterance.onerror = (e) => {
      console.warn('[TTS] 浏览器 TTS 错误:', e)
      currentUtterance = null
      processQueue()
    }

    speechSynth.speak(utterance)
  }

  /** 处理播放队列 */
  async function processQueue() {
    if (isPlaying || audioQueue.length === 0) {
      isSpeaking.value = false
      return
    }

    isPlaying = true
    isSpeaking.value = true

    const item = audioQueue.shift()!
    const text = item.text.trim()
    if (!text) {
      isPlaying = false
      processQueue()
      return
    }

    // 1. 检查是否有预加载好的音频（优先使用）
    let cachedBuffer: ArrayBuffer | null = null
    if (prefetchedAudio && prefetchedAudio.text === text) {
      cachedBuffer = prefetchedAudio.arrayBuffer
      prefetchedAudio = null
      console.log('[TTS] ✅ 命中预加载:', text.substring(0, 30) + '...')
    }

    // 2. 如果队列里还有下一句，提前发起预请求（并行，不等结果）
    if (audioQueue.length > 0) {
      const nextText = audioQueue[0].text.trim()
      if (nextText && !(prefetchedAudio && prefetchedAudio.text === nextText)) {
        startPrefetch(nextText)
      }
    }

    // 3. 有预加载缓存 → 直接播放，跳过网络请求
    if (cachedBuffer) {
      try {
        await playAudioBuffer(cachedBuffer)
      } catch (e) {
        console.warn('[TTS] 预加载音频播放失败:', e)
      }
      isPlaying = false
      processQueue()
      return
    }

    // 4. 无缓存 → 走正常 API 流程（获取 + 播放）
    const apiSuccess = await playViaApi(text)
    if (!apiSuccess) {
      // API 失败，回退到浏览器内置 TTS
      console.log('[TTS] 回退到浏览器 TTS:', text.substring(0, 30) + '...')
      // playViaBrowserTTS 会通过回调调用 processQueue
      playViaBrowserTTS(text)
      return
    }

    // API 播放完成，继续下一个
    isPlaying = false
    processQueue()
  }

  /** 将文字加入播放队列（自动分句） */
  function enqueueText(text: string) {
    if (!ttsEnabled.value) return

    // 按句子分割
    const sentences = text.split(SENTENCE_END).filter(s => s.trim())
    for (const sentence of sentences) {
      const trimmed = sentence.trim()
      if (trimmed) {
        audioQueue.push({ text: trimmed })
      }
    }

    // 如果不在播放中，开始处理队列
    if (!isPlaying && !currentUtterance) {
      processQueue()
    }
  }

  /** 流式文本缓冲区：用于在流式接收时累积并自动分句播放 */
  let streamBuffer = ''

  function feedStreamChunk(chunk: string) {
    if (!ttsEnabled.value) return

    streamBuffer += chunk

    // 查找最近一个句子结束位置
    const matches = [...streamBuffer.matchAll(new RegExp(SENTENCE_END.source, 'g'))]
    if (matches.length > 0) {
      const lastMatch = matches[matches.length - 1]
      const splitIndex = lastMatch.index! + lastMatch[0].length
      const completeText = streamBuffer.substring(0, splitIndex)
      streamBuffer = streamBuffer.substring(splitIndex)

      // 将完整句子加入队列
      enqueueText(completeText)
    }
  }

  /** 流式传输结束，播放剩余缓冲 */
  function flushStreamBuffer() {
    if (!ttsEnabled.value) return
    if (streamBuffer.trim()) {
      enqueueText(streamBuffer)
      streamBuffer = ''
    }
  }

  /** 重置状态 */
  function reset() {
    stopAndClear()
    streamBuffer = ''
    audioQueue = []
    isPlaying = false
    isSpeaking.value = false
  }

  /** 切换 TTS 开关 */
  function toggleTTS() {
    ttsEnabled.value = !ttsEnabled.value
    if (!ttsEnabled.value) {
      reset()
    }
  }

  return {
    ttsEnabled,
    isSpeaking,
    toggleTTS,
    enqueueText,
    feedStreamChunk,
    flushStreamBuffer,
    reset,
    stopAndClear,
    playViaBrowserTTS,
    playViaApi,
  }
}
