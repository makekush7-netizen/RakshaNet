package com.rakshanet.meshchat.courses

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseProgressCalculatorTest {
    private val module = CourseCatalog.floodReadiness

    @Test fun `progress counts lessons and quiz as durable steps`() {
        assertEquals(0, CourseProgressCalculator.percent(module, emptySet()))
        assertEquals(33, CourseProgressCalculator.percent(module, setOf(module.lessons.first().id)))
        assertEquals(100, CourseProgressCalculator.percent(module, module.allStepIds))
    }

    @Test fun `unrelated completion does not affect module`() {
        assertEquals(0, CourseProgressCalculator.percent(module, setOf("other.lesson")))
    }
}
