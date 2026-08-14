package com.scnu.schedule.data.repository

import com.scnu.schedule.data.db.CourseDao
import com.scnu.schedule.data.db.CourseEntity
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.repo.CourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepositoryImpl @Inject constructor(private val dao: CourseDao) : CourseRepository {
    override val courses: Flow<List<Course>> = dao.observeAll().map { list ->
        list.map { Course(it.id, it.name, it.teacher, it.location, it.dayOfWeek, it.startPeriod, it.endPeriod, it.weekPattern, it.colorIndex) }
    }
    override suspend fun upsert(course: Course) {
        dao.upsert(CourseEntity(course.id, course.name, course.teacher, course.location, course.dayOfWeek, course.startPeriod, course.endPeriod, course.weekPattern, course.colorIndex))
    }
    override suspend fun delete(id: Long) = dao.delete(id)
}
