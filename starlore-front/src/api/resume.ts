import request from '@/utils/request'

export interface ResumeData {
    id?: number
    title?: string
    template?: string
    name?: string
    jobTitle?: string
    phone?: string
    email?: string
    photoUrl?: string
    content?: string
    status?: string
    createdAt?: string
    updatedAt?: string
}

export function listResumes() {
    return request.get<never, { data: ResumeData[] }>('/resume/list')
}

export function getResume(id: number) {
    return request.get<never, { data: ResumeData }>(`/resume/${id}`)
}

export function createResume(params: Partial<ResumeData>) {
    return request.post<never, { data: ResumeData; message: string }>('/resume/create', params)
}

export function updateResume(params: Partial<ResumeData>) {
    return request.put<never, { data: ResumeData; message: string }>('/resume/update', params)
}

export function deleteResume(id: number) {
    return request.delete<never, { message: string }>(`/resume/${id}`)
}

export function uploadImage(file: File) {
    const form = new FormData()
    form.append('file', file)
    return request.post<never, { data: { url: string }; message: string }>('/upload/image', form, {
        timeout: 30000,
    })
}

async function readBlobError(blob: Blob): Promise<string> {
    try {
        const payload = JSON.parse(await blob.text()) as {
            detail?: string
            message?: string
            error?: string
        }
        return payload.detail || payload.message || payload.error || 'PDF 导出失败'
    } catch {
        return 'PDF 服务返回了无效响应'
    }
}

export async function exportResumePdf(id: number): Promise<Blob> {
    try {
        const blob = await request.get<never, Blob>(`/resume/${id}/export-pdf`, {
            responseType: 'blob',
            timeout: 60000,
        })
        if (blob.type !== 'application/pdf') {
            throw new Error(await readBlobError(blob))
        }
        return blob
    } catch (error: unknown) {
        const responseBlob = (error as { response?: { data?: unknown } }).response?.data
        if (responseBlob instanceof Blob) {
            throw new Error(await readBlobError(responseBlob))
        }
        throw error
    }
}


