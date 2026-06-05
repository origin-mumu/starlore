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

export function exportResumePdf(id: number) {
    return request.get<never, Blob>(`/resume/${id}/export-pdf`, {
        responseType: 'blob',
        timeout: 60000,
    })
}


