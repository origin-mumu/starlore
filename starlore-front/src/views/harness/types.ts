/**
 * Harness 云端内容生成智能体核心类型定义
 */

export interface HarnessSession {
  id: number
  user_id: number
  title: string
  model_id: string | null
  status: string
  pinned: boolean
  created_at: string
  updated_at: string
}

export interface HarnessArtifact {
  file_id: string
  name: string
  file_type: string
  size_str: string
  download_url: string
}

export interface HarnessToolCall {
  call_id: string
  tool: string
  label: string
  summary: string
  citations?: string[]
  status: 'running' | 'success' | 'error'
  step?: number
}

export interface HarnessStepDetail {
  step: number
  title?: string
  reasoning?: string
  scratchpad?: string
  tool_calls: HarnessToolCall[]
}

export interface HarnessMessage {
  id?: number
  session_id?: number
  role: 'user' | 'assistant' | 'system'
  content: string
  reasoning_content?: string
  tool_calls?: HarnessToolCall[]
  artifacts?: HarnessArtifact[]
  step_details?: HarnessStepDetail[]
  tokens_prompt?: number
  tokens_completion?: number
  duration_ms?: number
  created_at?: string
  images?: string[]
}

export interface HarnessModelItem {
  id: string
  name: string
  vendor: string
}

export interface HarnessTodoItem {
  content: string
  status: 'pending' | 'in_progress' | 'completed'
}

export interface StreamEventPayload {
  event:
    | 'step'
    | 'reasoning'
    | 'step_thought'
    | 'tool_start'
    | 'tool_done'
    | 'artifact'
    | 'content'
    | 'todo'
    | 'done'
    | 'error'
  data: any
}
