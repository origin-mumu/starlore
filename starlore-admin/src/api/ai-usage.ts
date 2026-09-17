import { http } from '@/utils/request'
import type { AiUsageListVO, AiUsageQuery, AiUsageSummaryVO } from '@/types'

/** AI 调用明细分页列表 */
export function getAiUsageService(query: AiUsageQuery): Promise<AiUsageListVO> {
  return http.get('/admin/ai-usage', { params: query })
}

/** AI 用量汇总：今日概况、趋势、分布与排行 */
export async function getAiUsageSummaryService(): Promise<AiUsageSummaryVO> {
  const res = await http.get<{ data?: AiUsageSummaryVO }>('/admin/ai-usage/summary')
  const body = res as { data?: AiUsageSummaryVO }
  return body?.data ?? (res as unknown as AiUsageSummaryVO)
}
