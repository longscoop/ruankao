import { apiRequest } from '@/api/client'
import type { CourseDetailDto, CourseSummaryDto, KnowledgeDetailDto, VideoDetailDto } from '@/types/api'

export function listCourses(examId?: number): Promise<CourseSummaryDto[]> {
  const query = examId ? `?examId=${examId}` : ''
  return apiRequest<CourseSummaryDto[]>({ path: `/api/v1/courses${query}` })
}

export function getCourse(courseId: number): Promise<CourseDetailDto> {
  return apiRequest<CourseDetailDto>({ path: `/api/v1/courses/${courseId}` })
}

export function getVideo(videoId: number): Promise<VideoDetailDto> {
  return apiRequest<VideoDetailDto>({ path: `/api/v1/videos/${videoId}` })
}

export function updateVideoProgress(videoId: number, progressSeconds: number): Promise<void> {
  return apiRequest<void>({
    path: `/api/v1/videos/${videoId}/progress`,
    method: 'PUT',
    data: { progressSeconds: Math.max(0, Math.floor(progressSeconds)) },
  })
}

export function getKnowledge(knowledgeId: number): Promise<KnowledgeDetailDto> {
  return apiRequest<KnowledgeDetailDto>({ path: `/api/v1/knowledge/${knowledgeId}` })
}
