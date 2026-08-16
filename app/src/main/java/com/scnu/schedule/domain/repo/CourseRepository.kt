package com.scnu.schedule.domain.repo

import com.scnu.schedule.domain.model.Course
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    val courses: Flow<List<Course>>
    suspend fun upsert(course: Course)
    suspend fun delete(id: Long)
    suspend fun clearAll()
}
