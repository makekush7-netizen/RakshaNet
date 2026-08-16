package com.rakshanet.meshchat.courses

data class Lesson(
    val id: String,
    val title: String,
    val eyebrow: String,
    val pages: List<String>,
)

data class QuizQuestion(
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

data class CourseModule(
    val id: String,
    val title: String,
    val description: String,
    val lessons: List<Lesson>,
    val quiz: List<QuizQuestion>,
) {
    val quizStepId: String get() = "$id.quiz"
    val allStepIds: Set<String> get() = lessons.mapTo(mutableSetOf()) { it.id } + quizStepId
}

object CourseCatalog {
    val floodReadiness = CourseModule(
        id = "flood-readiness",
        title = "Flood Readiness",
        description = "Prepare early, move safely, and avoid the most dangerous flood mistakes.",
        lessons = listOf(
            Lesson(
                id = "flood-readiness.prepare",
                title = "Prepare before the rain",
                eyebrow = "Lesson 1 · Before a flood",
                pages = listOf(
                    "Keep drinking water, dry food, medicines, a torch, power bank, copies of important documents, and a basic first-aid kit together in a waterproof bag.",
                    "Know a safe route to higher ground. Agree on a family meeting point and keep emergency contacts written on paper in case a phone battery fails.",
                    "Charge phones when heavy rain is forecast. Move valuables and electrical items higher, but never risk your safety to protect property.",
                ),
            ),
            Lesson(
                id = "flood-readiness.respond",
                title = "Move safely when water rises",
                eyebrow = "Lesson 2 · During a flood",
                pages = listOf(
                    "Move to higher ground early when authorities or trusted local alerts advise it. Waiting for visible fast water can remove your safest exit route.",
                    "Never walk or drive through moving floodwater. Depth, current, open drains, electrical hazards, and damaged roads are difficult to judge from the surface.",
                    "Switch off electricity only if you can reach the main switch without entering water. Keep away from fallen wires and report them when communication is available.",
                ),
            ),
        ),
        quiz = listOf(
            QuizQuestion(
                "What is the safest response when water covers a road?",
                listOf("Cross slowly", "Wait or use a known safe route", "Follow the largest vehicle"),
                1,
                "Floodwater can hide current, open drains, and road damage. Turn around or use a verified safe route.",
            ),
            QuizQuestion(
                "Where should important documents be kept?",
                listOf("In a waterproof emergency bag", "Near the front door uncovered", "Inside an electrical cabinet"),
                0,
                "Waterproof copies in one ready-to-carry bag reduce delay during evacuation.",
            ),
            QuizQuestion(
                "When should a family move toward higher ground?",
                listOf("Only after water enters the house", "Early, after a credible warning", "After phone batteries are empty"),
                1,
                "Moving early keeps safer routes available and avoids panic in fast-changing conditions.",
            ),
        ),
    )
}

object CourseProgressCalculator {
    fun percent(module: CourseModule, completedStepIds: Set<String>): Int {
        if (module.allStepIds.isEmpty()) return 0
        val completed = module.allStepIds.count(completedStepIds::contains)
        return (completed * 100) / module.allStepIds.size
    }
}
