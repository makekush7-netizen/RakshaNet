package com.rakshanet.meshchat.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rakshanet.meshchat.R
import com.rakshanet.meshchat.courses.CourseCatalog
import com.rakshanet.meshchat.courses.CourseProgressCalculator
import com.rakshanet.meshchat.courses.CourseRepository
import com.rakshanet.meshchat.ui.theme.*
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
        CourseView.DASHBOARD -> CourseDashboard(CourseProgressCalculator.percent(module, completedSteps)) { viewName = CourseView.MODULE.name }
        CourseView.MODULE -> CoursePath(
            completedSteps,
            onBack = { viewName = CourseView.DASHBOARD.name },
            onOpenLesson = { lessonIndex = it; pageIndex = 0; viewName = CourseView.LESSON.name },
            onOpenQuiz = { questionIndex = 0; selectedAnswer = null; correctAnswers = 0; viewName = CourseView.QUIZ.name },
        )
        CourseView.LESSON -> {
            val lesson = module.lessons[lessonIndex]
            LessonScreen(
                lessonIndex, lesson.eyebrow, lesson.title, lesson.pages[pageIndex], pageIndex + 1, lesson.pages.size,
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
                questionIndex + 1, module.quiz.size, question.prompt, question.choices, question.correctIndex,
                question.explanation, selectedAnswer,
                onSelect = { if (selectedAnswer == null) { selectedAnswer = it; if (it == question.correctIndex) correctAnswers++ } },
                onNext = {
                    if (questionIndex < module.quiz.lastIndex) { questionIndex++; selectedAnswer = null }
                    else scope.launch { repository.completeStep(module.quizStepId, correctAnswers, module.quiz.size); viewName = CourseView.RESULT.name }
                },
                onExit = { viewName = CourseView.MODULE.name },
            )
        }
        CourseView.RESULT -> ResultScreen(correctAnswers, module.quiz.size) { viewName = CourseView.DASHBOARD.name }
    }
}

@Composable
private fun CourseDashboard(progress: Int, onOpenFlood: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Learn", style = MaterialTheme.typography.headlineLarge); Text("Practical skills, one short challenge at a time.", style = MaterialTheme.typography.bodyMedium) }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(progress = { progress / 100f }, modifier = Modifier.size(50.dp), color = RakshaGreen, trackColor = Mint)
                    Column(Modifier.weight(1f).padding(start = 13.dp)) { Text("Flood readiness", style = MaterialTheme.typography.titleMedium); Text("$progress% complete · progress saved offline", style = MaterialTheme.typography.bodySmall) }
                    Icon(Icons.Outlined.EmojiEvents, null, tint = Sun)
                }
            }
        }
        item { CourseHero(onOpenFlood) }
        item { Text("Next preparedness paths", style = MaterialTheme.typography.titleLarge) }
        item { ComingSoonCard(R.drawable.earthquake_course, "Earthquake readiness", "Drop, cover, hold—and prepare your home.") }
        item { ComingSoonCard(R.drawable.cyclone_course, "Cyclone & severe storms", "Protect openings, prepare supplies, follow warnings.") }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MintSoft, border = androidx.compose.foundation.BorderStroke(1.dp, Mint)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Verified, null, tint = RakshaGreenDark)
                    Column(Modifier.padding(start = 12.dp)) { Text("Ambassador path", style = MaterialTheme.typography.titleMedium); Text("Advanced community skills · coming soon", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item { Text("Safety content is adapted from NDRF, NDMA and WHO guidance. Open each lesson for its source note.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun CourseHero(onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(26.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
        Column {
            Image(painterResource(R.drawable.flood_preparedness_hero), null, Modifier.fillMaxWidth().aspectRatio(1.8f).clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = Mint) { Text("PLAYABLE NOW", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = RakshaGreenDark) }
                    Spacer(Modifier.weight(1f)); Text("4 lessons · 5 challenges", style = MaterialTheme.typography.labelSmall)
                }
                Text("Flood readiness", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleLarge)
                Text("Prepare early, move safely, verify updates, and help without taking unnecessary risks.", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onClick, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Continue journey"); Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.ChevronRight, null) }
            }
        }
    }
}

@Composable
private fun ComingSoonCard(@DrawableRes image: Int, title: String, subtitle: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(image), null, Modifier.size(90.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f).padding(horizontal = 13.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall); Text("COMING SOON", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall, color = RakshaGreenDark) }
            Icon(Icons.Filled.Lock, null, tint = MutedInk)
        }
    }
}

@Composable
private fun CoursePath(completed: Set<String>, onBack: () -> Unit, onOpenLesson: (Int) -> Unit, onOpenQuiz: () -> Unit) {
    val module = CourseCatalog.floodReadiness
    val lessonsComplete = module.lessons.all { it.id in completed }
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }; Text("Flood readiness", style = MaterialTheme.typography.headlineLarge); Text("Build skills. Protect lives.", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(10.dp)) }
        items(module.lessons.size) { index ->
            val lesson = module.lessons[index]
            PathNode(index + 1, lesson.title, lesson.eyebrow.substringAfter("· "), lesson.id in completed, index % 2 == 1) { onOpenLesson(index) }
        }
        item { PathNode(5, "Readiness challenge", "5 scenario questions", module.quizStepId in completed, false, enabled = lessonsComplete, onClick = onOpenQuiz) }
        item { Spacer(Modifier.height(12.dp)); Text("Sources", style = MaterialTheme.typography.titleMedium); Text("NDRF Flood Safety DOs & DON'Ts · NDMA SACHET · WHO flood health guidance", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun PathNode(number: Int, title: String, subtitle: String, complete: Boolean, offset: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = if (offset) 54.dp else 0.dp, end = if (offset) 0.dp else 34.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(48.dp), shape = CircleShape, color = when { complete -> RakshaGreen; enabled -> Sun; else -> CardBorder }) {
            Box(contentAlignment = Alignment.Center) { if (complete) Icon(Icons.Filled.Check, null, tint = Color.White) else if (enabled) Text("$number", fontWeight = FontWeight.Bold, color = Ink) else Icon(Icons.Filled.Lock, null, tint = MutedInk) }
        }
        Surface(Modifier.weight(1f).padding(start = 10.dp).clickable(enabled = enabled, onClick = onClick), shape = RoundedCornerShape(20.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, if (complete) Mint else CardBorder)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.MenuBook, null, tint = if (complete) RakshaGreenDark else Ink); Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(if (complete) "Completed" else subtitle, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Outlined.ChevronRight, null) }
        }
    }
}

@Composable
private fun LessonScreen(index: Int, eyebrow: String, title: String, body: String, page: Int, pageCount: Int, onBack: () -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().height(220.dp)) {
            Image(painterResource(if (index == 0 || index == 2) R.drawable.flood_preparedness_hero else R.drawable.flood_drill_hero), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            IconButton(onClick = onBack, Modifier.padding(12.dp).background(Color.White.copy(alpha = .92f), CircleShape)) { Icon(Icons.Outlined.ArrowBack, "Back") }
        }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row { Text(eyebrow, color = RakshaGreenDark, style = MaterialTheme.typography.labelLarge); Spacer(Modifier.weight(1f)); Text("$page / $pageCount", style = MaterialTheme.typography.labelLarge) }
            Text(title, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.headlineMedium)
            Surface(Modifier.fillMaxWidth().weight(1f).padding(vertical = 18.dp), shape = RoundedCornerShape(26.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.Center) { Text(body, style = MaterialTheme.typography.bodyLarge); Surface(Modifier.padding(top = 18.dp), shape = RoundedCornerShape(14.dp), color = MintSoft) { Text("Try this: explain the step to someone at home in one sentence.", Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) } }
            }
            Text("Source: NDRF / NDMA / WHO flood safety guidance", style = MaterialTheme.typography.labelSmall)
            Button(onClick = onContinue, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text(if (page == pageCount) "Complete lesson" else "Continue") }
        }
    }
}

@Composable
private fun QuizScreen(number: Int, count: Int, prompt: String, choices: List<String>, correctIndex: Int, explanation: String, selected: Int?, onSelect: (Int) -> Unit, onNext: () -> Unit, onExit: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        IconButton(onClick = onExit) { Icon(Icons.Outlined.ArrowBack, "Exit") }
        LinearProgressIndicator(progress = { number / count.toFloat() }, Modifier.fillMaxWidth(), color = RakshaGreen, trackColor = Mint)
        Text("Challenge $number of $count", Modifier.padding(top = 18.dp), color = RakshaGreenDark, style = MaterialTheme.typography.labelLarge)
        Text(prompt, Modifier.padding(top = 9.dp, bottom = 18.dp), style = MaterialTheme.typography.headlineSmall)
        choices.forEachIndexed { index, choice ->
            val chosen = selected == index
            val correct = selected != null && index == correctIndex
            Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(enabled = selected == null) { onSelect(index) }, shape = RoundedCornerShape(18.dp), color = when { correct -> Mint; chosen -> Color(0xFFFFE3DC); else -> Color.White }, border = androidx.compose.foundation.BorderStroke(1.dp, when { correct -> RakshaGreen; chosen -> EmergencyRed; else -> CardBorder })) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(30.dp), shape = CircleShape, color = MaterialTheme.colorScheme.background) { Box(contentAlignment = Alignment.Center) { Text("${index + 1}", style = MaterialTheme.typography.labelLarge) } }; Text(choice, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge) }
            }
        }
        if (selected != null) {
            Surface(Modifier.fillMaxWidth().padding(top = 16.dp), shape = RoundedCornerShape(18.dp), color = if (selected == correctIndex) MintSoft else Color(0xFFFFF5CF)) { Text(explanation, Modifier.padding(15.dp), style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.weight(1f)); Button(onClick = onNext, Modifier.fillMaxWidth()) { Text(if (number == count) "See result" else "Next challenge") }
        }
    }
}

@Composable
private fun ResultScreen(score: Int, total: Int, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(Modifier.size(90.dp), shape = CircleShape, color = Mint) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EmojiEvents, null, tint = RakshaGreenDark, modifier = Modifier.size(45.dp)) } }
        Text("Flood ready", Modifier.padding(top = 20.dp), style = MaterialTheme.typography.headlineLarge)
        Text("$score of $total correct", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge, color = RakshaGreenDark)
        Text("Your progress is saved on this phone and stays available offline.", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onDone, Modifier.fillMaxWidth().padding(top = 26.dp)) { Text("Back to learning path") }
    }
}
