package com.rakshanet.meshchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rakshanet.meshchat.courses.CourseCatalog
import com.rakshanet.meshchat.courses.CourseProgressCalculator
import com.rakshanet.meshchat.courses.CourseRepository
import com.rakshanet.meshchat.ui.theme.ConnectTint
import com.rakshanet.meshchat.ui.theme.CourseTint
import com.rakshanet.meshchat.ui.theme.Navy
import com.rakshanet.meshchat.ui.theme.Teal
import kotlinx.coroutines.launch

private enum class CourseView { DASHBOARD, MODULE, LESSON, QUIZ, RESULT }

@Composable
fun CourseScreen(repository: CourseRepository) {
    val module = CourseCatalog.floodReadiness
    val completedSteps by repository.completedStepIds.collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()
    var viewName by rememberSaveable { mutableStateOf(CourseView.DASHBOARD.name) }
    var lessonIndex by rememberSaveable { mutableIntStateOf(0) }
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedAnswer by rememberSaveable { mutableStateOf<Int?>(null) }
    var correctAnswers by rememberSaveable { mutableIntStateOf(0) }
    val view = runCatching { CourseView.valueOf(viewName) }.getOrDefault(CourseView.DASHBOARD)

    when (view) {
        CourseView.DASHBOARD -> CourseDashboard(
            progress = CourseProgressCalculator.percent(module, completedSteps),
            onOpenFlood = { viewName = CourseView.MODULE.name },
        )
        CourseView.MODULE -> CourseModuleScreen(
            completedSteps = completedSteps,
            onBack = { viewName = CourseView.DASHBOARD.name },
            onOpenLesson = { index -> lessonIndex = index; pageIndex = 0; viewName = CourseView.LESSON.name },
            onOpenQuiz = { questionIndex = 0; selectedAnswer = null; correctAnswers = 0; viewName = CourseView.QUIZ.name },
        )
        CourseView.LESSON -> {
            val lesson = module.lessons[lessonIndex]
            LessonScreen(
                eyebrow = lesson.eyebrow,
                title = lesson.title,
                body = lesson.pages[pageIndex],
                page = pageIndex + 1,
                pageCount = lesson.pages.size,
                onBack = { if (pageIndex > 0) pageIndex-- else viewName = CourseView.MODULE.name },
                onContinue = {
                    if (pageIndex < lesson.pages.lastIndex) pageIndex++ else scope.launch {
                        repository.completeStep(lesson.id)
                        viewName = CourseView.MODULE.name
                    }
                },
            )
        }
        CourseView.QUIZ -> {
            val question = module.quiz[questionIndex]
            QuizScreen(
                questionNumber = questionIndex + 1,
                questionCount = module.quiz.size,
                prompt = question.prompt,
                choices = question.choices,
                correctIndex = question.correctIndex,
                explanation = question.explanation,
                selectedAnswer = selectedAnswer,
                onSelect = { choice ->
                    if (selectedAnswer == null) {
                        selectedAnswer = choice
                        if (choice == question.correctIndex) correctAnswers++
                    }
                },
                onNext = {
                    if (questionIndex < module.quiz.lastIndex) {
                        questionIndex++
                        selectedAnswer = null
                    } else scope.launch {
                        repository.completeStep(module.quizStepId, correctAnswers, module.quiz.size)
                        viewName = CourseView.RESULT.name
                    }
                },
                onExit = { viewName = CourseView.MODULE.name },
            )
        }
        CourseView.RESULT -> ResultScreen(
            score = correctAnswers,
            total = module.quiz.size,
            onDone = { viewName = CourseView.DASHBOARD.name },
        )
    }
}

@Composable
private fun CourseDashboard(progress: Int, onOpenFlood: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Courses", modifier = Modifier.padding(top = 20.dp), style = MaterialTheme.typography.headlineLarge)
            Text("Small lessons. Safer decisions.", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Surface(Modifier.fillMaxWidth().clickable(onClick = onOpenFlood), shape = RoundedCornerShape(28.dp), color = CourseTint) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).background(Navy, CircleShape), contentAlignment = Alignment.Center) {
                            Text(if (progress == 100) "✓" else "1", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text("Flood Readiness", style = MaterialTheme.typography.titleLarge, color = Navy)
                            Text("2 lessons + a short quiz")
                        }
                        Text("›", style = MaterialTheme.typography.headlineMedium, color = Navy)
                    }
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        color = Teal,
                    )
                    Text("$progress% complete", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold, color = Teal)
                }
            }
        }
        item { LockedCourseNode("Earthquake Readiness", "Coming soon") }
        item { LockedCourseNode("Storm Readiness", "Coming soon") }
        item { LockedCourseNode("Community Ambassador", "Future pathway") }
    }
}

@Composable
private fun LockedCourseNode(title: String, badge: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.72f)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) { Text("LOCK", style = MaterialTheme.typography.labelSmall) }
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(badge, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CourseModuleScreen(
    completedSteps: Set<String>,
    onBack: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    onOpenQuiz: () -> Unit,
) {
    val module = CourseCatalog.floodReadiness
    val lessonsComplete = module.lessons.all { it.id in completedSteps }
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onBack) { Text("‹ Courses") } }
        item {
            Text(module.title, style = MaterialTheme.typography.headlineLarge)
            Text(module.description, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge)
        }
        items(module.lessons.size) { index ->
            val lesson = module.lessons[index]
            val complete = lesson.id in completedSteps
            Surface(Modifier.fillMaxWidth().clickable { onOpenLesson(index) }, shape = RoundedCornerShape(22.dp), color = if (complete) ConnectTint else Color.White) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (complete) "✓" else "${index + 1}", fontWeight = FontWeight.Bold, color = Teal)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                        Text(if (complete) "Completed" else "${lesson.pages.size} cards")
                    }
                    Text("›", color = Navy)
                }
            }
        }
        item {
            Button(onClick = onOpenQuiz, enabled = lessonsComplete, modifier = Modifier.fillMaxWidth()) {
                Text(if (lessonsComplete) "Take the readiness quiz" else "Complete both lessons to unlock quiz")
            }
        }
    }
}

@Composable
private fun LessonScreen(
    eyebrow: String,
    title: String,
    body: String,
    page: Int,
    pageCount: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        Text(eyebrow, color = Teal, fontWeight = FontWeight.SemiBold)
        Text(title, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.headlineLarge)
        Surface(Modifier.fillMaxWidth().weight(1f).padding(vertical = 24.dp), shape = RoundedCornerShape(30.dp), color = Color.White) {
            Column(Modifier.padding(26.dp), verticalArrangement = Arrangement.Center) {
                Text(body, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Text("Card $page of $pageCount", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Text(if (page == pageCount) "Complete lesson" else "Continue")
        }
    }
}

@Composable
private fun QuizScreen(
    questionNumber: Int,
    questionCount: Int,
    prompt: String,
    choices: List<String>,
    correctIndex: Int,
    explanation: String,
    selectedAnswer: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        TextButton(onClick = onExit) { Text("‹ Exit quiz") }
        Text("Question $questionNumber of $questionCount", color = Teal, fontWeight = FontWeight.SemiBold)
        Text(prompt, modifier = Modifier.padding(top = 10.dp, bottom = 22.dp), style = MaterialTheme.typography.headlineSmall)
        choices.forEachIndexed { index, choice ->
            val chosen = selectedAnswer == index
            val correct = selectedAnswer != null && index == correctIndex
            OutlinedButton(
                onClick = { onSelect(index) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = when { correct -> ConnectTint; chosen -> CourseTint; else -> Color.Transparent },
                ),
            ) { Text(choice) }
        }
        if (selectedAnswer != null) {
            Surface(Modifier.fillMaxWidth().padding(top = 18.dp), shape = RoundedCornerShape(20.dp), color = Color.White) {
                Text(explanation, Modifier.padding(16.dp))
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(if (questionNumber == questionCount) "Finish quiz" else "Next question") }
        }
    }
}

@Composable
private fun ResultScreen(score: Int, total: Int, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Flood Ready", style = MaterialTheme.typography.headlineLarge)
        Text("$score / $total correct", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleLarge, color = Teal)
        Text("Your progress is saved on this phone and remains available offline.", modifier = Modifier.padding(top = 12.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) { Text("Back to courses") }
    }
}
