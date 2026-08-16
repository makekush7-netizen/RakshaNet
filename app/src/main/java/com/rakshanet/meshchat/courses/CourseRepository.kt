package com.rakshanet.meshchat.courses

import com.rakshanet.meshchat.data.local.CourseProgressDao
import com.rakshanet.meshchat.data.local.CourseProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseRepository(private val dao: CourseProgressDao) {
    val completedStepIds: Flow<Set<String>> = dao.observeAll().map { rows -> rows.mapTo(mutableSetOf()) { it.stepId } }

    suspend fun completeStep(stepId: String, score: Int? = null, maxScore: Int? = null) {
        dao.upsert(CourseProgressEntity(stepId, System.currentTimeMillis(), score, maxScore))
    }
}
