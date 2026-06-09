import request from '@/utils/request'

export interface Project {
    id: number
    name: string
    description: string | null
    url: string | null
    image: string | null
    sortOrder: number | null
    createdAt: string
    updatedAt: string
}

export interface ProjectParams {
    name: string
    description?: string
    url?: string
    image?: string
    sortOrder?: number
}

export function listProjectsService() {
    return request.get<never, { data: Project[] }>('/projects')
}

export function createProjectService(params: ProjectParams) {
    return request.post<never, { data: Project; message: string }>('/projects', params)
}

export function updateProjectService(id: number, params: ProjectParams) {
    return request.put<never, { data: Project; message: string }>(`/projects/${id}`, params)
}

export function deleteProjectService(id: number) {
    return request.delete<never, { message: string }>(`/projects/${id}`)
}
