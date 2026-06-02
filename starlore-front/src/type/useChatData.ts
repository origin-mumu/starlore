// useChatData.ts
import { ref } from 'vue'

export interface Message {
  id: number
  role: 'user' | 'ai'
  content: string
}

export const useChatData = (count = 1000) => {
  const messages = ref<Message[]>([])
  
  const generate = () => {
    const data: Message[] = []
    for (let i = 0; i < count; i++) {
      data.push({
        id: i,
        role: i % 2 === 0 ? 'user' : 'ai',
        // 随机生成不同长度的文本，模拟不定高度
        content: i % 2 === 0 
          ? '你好，这是一条测试消息。' 
          : '这是一条AI回复。' + '非常长'.repeat(Math.ceil(Math.random() * 50)) + '的消息，用于测试换行和高度计算。'
      })
    }
    messages.value = data
  }

  generate()
  return { messages }
}