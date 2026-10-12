package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.ContextWrapper
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.InterviewMode
import com.example.data.QuestionDifficulty
import com.example.data.local.InterviewQuestionEntity
import com.example.data.local.InterviewSessionEntity
import com.example.service.GeminiInterviewService
import com.example.ui.components.AudioPulseVisualizer
import com.example.ui.components.CameraPreviewView
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusClaimedUnvalidated
import com.example.ui.theme.StatusDemonstrated
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * TASK 5 — Redesigned ForkAI Active Interview Screen
 *
 * Implements the full interactive workflow:
 * Interview Begins → Question Displayed → Candidate Writes Answer → Answer Submitted →
 * Existing Evaluation Runs → Next Adaptive Question Appears → Interview Completes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInterviewScreen(
    session: InterviewSessionEntity?,
    questions: List<InterviewQuestionEntity>,
    isEvaluating: Boolean,
    onSubmitAnswer: (questionId: String, answer: String, durationSeconds: Int) -> Unit,
    onAppFocusLost: () -> Unit,
    onPasteDetected: (Int) -> Unit,
    onCameraStatusChange: (statusReason: String) -> Unit = {},
    onExitInterview: () -> Unit = {},
    onViewFinalReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Immersive Full-Screen Mode setup: Hides system bars during interview and restores on exit
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (activity != null) {
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Exit confirmation dialog state
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept system/hardware back press
    BackHandler(enabled = true) {
        showExitDialog = true
    }

    // Determine completion status
    val isSessionCompleted = session?.isCompleted == true || (questions.isNotEmpty() && questions.all { it.isEvaluated })

    // Determine currently active question (the first one not yet evaluated)
    val activeQuestion = questions.firstOrNull { !it.isEvaluated }
    val evaluatedCount = questions.count { it.isEvaluated }
    val totalQuestions = session?.totalQuestions ?: questions.size.coerceAtLeast(1)
    val currentQuestionIndex = activeQuestion?.orderIndex ?: (evaluatedCount + 1).coerceAtMost(totalQuestions)

    // Local answer text state preserved per question
    var answerText by remember(activeQuestion?.id) {
        mutableStateOf(activeQuestion?.candidateAnswer ?: "")
    }

    // Elapsed timer for active question
    var elapsedSeconds by remember(activeQuestion?.id) { mutableIntStateOf(0) }
    LaunchedEffect(activeQuestion?.id, isEvaluating, isSessionCompleted) {
        if (!isEvaluating && !isSessionCompleted && activeQuestion != null && !activeQuestion.isEvaluated) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // Submission error / retry state
    var submissionError by remember { mutableStateOf<String?>(null) }
    var isSubmittingLocally by remember { mutableStateOf(false) }

    // Reset local submission flag when evaluating state finishes
    LaunchedEffect(isEvaluating) {
        if (!isEvaluating) {
            isSubmittingLocally = false
        }
    }

    // Voice Input State
    var isRecordingAudio by remember { mutableStateOf(false) }
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isRecordingAudio = false }
                override fun onError(error: Int) { isRecordingAudio = false }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0]
                        answerText = if (answerText.isBlank()) recognizedText else "$answerText $recognizedText"
                    }
                    isRecordingAudio = false
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer = recognizer
        }

        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer?.startListening(intent)
            isRecordingAudio = true
        }
    }

    // Minimum answer validation
    val trimmedAnswer = answerText.trim()
    val isAnswerValid = trimmedAnswer.length >= 10
    val wordCount = if (trimmedAnswer.isEmpty()) 0 else trimmedAnswer.split("\\s+".toRegex()).count { it.isNotBlank() }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
            },
            title = {
                Text(
                    text = "Leave Active Interview?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Your answers and session progress are preserved in your local database. Leaving now will return you to your dashboard, and you can resume anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onExitInterview()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_exit_button")
                ) {
                    Text("Leave Interview")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    modifier = Modifier.testTag("cancel_exit_button")
                ) {
                    Text("Continue Interview")
                }
            },
            modifier = Modifier.testTag("confirm_exit_dialog")
        )
    }

    Scaffold(
        topBar = {
            // A. Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_interview_top_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { showExitDialog = true },
                            modifier = Modifier.testTag("exit_interview_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit active interview"
                            )
                        }
                        Column {
                            Text(
                                text = "AI Interview",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = session?.roleTitle ?: "Target Technical Role",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Elapsed Timer Badge
                    if (!isSessionCompleted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    modifier = Modifier.size(15.dp),
                                    tint = PrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_interview_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // If interview is fully completed, show the completion state
            if (isSessionCompleted) {
                InterviewCompletedSection(
                    session = session,
                    questions = questions,
                    onViewFinalReport = onViewFinalReport,
                    onReturnToDashboard = onExitInterview
                )
            } else if (activeQuestion != null) {
                // A1. Immersive Focus Mode Integrity Indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SecondaryTeal.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("immersive_focus_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SecondaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Immersive Focus Mode Active",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SecondaryTeal
                            )
                            Text(
                                text = "System bars minimized to maximize focus. App switches and input speed are logged to audit timeline for recruiter review.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // A2. Optional Camera Verification Feed
                CameraPreviewView(
                    onCameraStatusChange = { reason ->
                        if (session?.id != null) {
                            onCameraStatusChange(reason)
                        }
                    }
                )

                // B. Interview Progress Section
                InterviewProgressSection(
                    currentQuestionIndex = currentQuestionIndex,
                    totalQuestions = totalQuestions,
                    evaluatedCount = evaluatedCount,
                    sessionMode = session?.mode ?: InterviewMode.FORMAL
                )

                // C. Active Question Card
                QuestionCardSection(
                    question = activeQuestion,
                    orderIndex = currentQuestionIndex,
                    totalQuestions = totalQuestions
                )

                // D. Answer Editor Section
                AnswerEditorSection(
                    answerText = answerText,
                    onAnswerChange = { newText ->
                        // Check for rapid paste anomaly
                        if (newText.length - answerText.length > 200) {
                            onPasteDetected(newText.length - answerText.length)
                        }
                        answerText = newText
                        submissionError = null
                    },
                    wordCount = wordCount,
                    isAnswerValid = isAnswerValid,
                    isRecordingAudio = isRecordingAudio,
                    onToggleVoiceDictation = {
                        if (isRecordingAudio) {
                            speechRecognizer?.stopListening()
                            isRecordingAudio = false
                        } else {
                            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onInsertPrompt = {
                        if (answerText.isBlank()) {
                            answerText = "In my previous experience, I approached this by "
                        }
                    }
                )

                // Submission Error Banner if any
                if (submissionError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = submissionError ?: "Failed to evaluate answer. Your response is saved. Tap submit to retry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // E. Submit Action Button
                val isBusy = isEvaluating || isSubmittingLocally
                Button(
                    onClick = {
                        if (isAnswerValid && !isBusy) {
                            isSubmittingLocally = true
                            submissionError = null
                            try {
                                onSubmitAnswer(activeQuestion.id, trimmedAnswer, elapsedSeconds)
                            } catch (e: Exception) {
                                isSubmittingLocally = false
                                submissionError = "Submission failed: ${e.localizedMessage ?: "Unknown error"}. Your answer is preserved."
                            }
                        }
                    },
                    enabled = isAnswerValid && !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_answer_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Evaluating Evidence & Adapting...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentQuestionIndex >= totalQuestions) "Submit Final Answer" else "Submit Answer & Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Loading / Initializing Question Bank State
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Preparing Adaptive Question...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Analyzing resume context and calibrating competencies for ${session?.roleTitle ?: "target role"}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * B. Interview Progress Section
 */
@Composable
private fun InterviewProgressSection(
    currentQuestionIndex: Int,
    totalQuestions: Int,
    evaluatedCount: Int,
    sessionMode: InterviewMode,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question $currentQuestionIndex of $totalQuestions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (sessionMode == InterviewMode.FORMAL) PrimaryBlue.copy(alpha = 0.12f) else AccentViolet.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (sessionMode == InterviewMode.FORMAL) "Formal Mode" else "Practice Mode",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (sessionMode == InterviewMode.FORMAL) PrimaryBlue else AccentViolet,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Accurate progress bar based on evaluated questions
            val progressRatio = if (totalQuestions > 0) (evaluatedCount.toFloat() / totalQuestions).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("interview_progress_indicator"),
                color = PrimaryBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Integrity Monitoring Sub-bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (GeminiInterviewService.isAiConfigured()) StatusDemonstrated else StatusClaimedUnvalidated)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (GeminiInterviewService.isAiConfigured()) "Live AI (Gemini 3.5 Flash)" else "Rule-Based Fallback Engine",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (GeminiInterviewService.isAiConfigured()) StatusDemonstrated else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$evaluatedCount completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * C. Question Card Section
 */
@Composable
private fun QuestionCardSection(
    question: InterviewQuestionEntity,
    orderIndex: Int,
    totalQuestions: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
            .testTag("question_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Badges row: Competency & Difficulty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    modifier = Modifier.testTag("question_competency_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = question.targetCompetency,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Difficulty badge
                val (diffColor, diffBg) = when (question.difficulty) {
                    QuestionDifficulty.FOUNDATIONAL -> Pair(SecondaryTeal, SecondaryTeal.copy(alpha = 0.12f))
                    QuestionDifficulty.INTERMEDIATE -> Pair(PrimaryBlue, PrimaryBlue.copy(alpha = 0.12f))
                    QuestionDifficulty.ADVANCED -> Pair(StatusClaimedUnvalidated, StatusClaimedUnvalidated.copy(alpha = 0.15f))
                    QuestionDifficulty.EXPERT -> Pair(AccentViolet, AccentViolet.copy(alpha = 0.15f))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = diffBg,
                    modifier = Modifier.testTag("question_difficulty_badge")
                ) {
                    Text(
                        text = question.difficulty.name,
                        color = diffColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Question prompt text
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("question_text")
            )

            // Adaptive Rationale (Explains why AI selected this question)
            if (question.adaptiveReason.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Adaptive Rationale",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = question.adaptiveReason,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * D. Answer Editor Section
 */
@Composable
private fun AnswerEditorSection(
    answerText: String,
    onAnswerChange: (String) -> Unit,
    wordCount: Int,
    isAnswerValid: Boolean,
    isRecordingAudio: Boolean,
    onToggleVoiceDictation: () -> Unit,
    onInsertPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Technical Answer",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AudioPulseVisualizer(
                        isListening = isRecordingAudio,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    IconButton(
                        onClick = onToggleVoiceDictation,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("voice_dictation_button")
                    ) {
                        Icon(
                            imageVector = if (isRecordingAudio) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice dictation",
                            tint = if (isRecordingAudio) MaterialTheme.colorScheme.error else PrimaryBlue
                        )
                    }
                }
            }

            OutlinedTextField(
                value = answerText,
                onValueChange = onAnswerChange,
                placeholder = {
                    Text(
                        text = "Write your answer here with concrete technical evidence, architectural patterns, and real-world trade-offs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isAnswerValid) "Ready to submit" else "Min 10 characters required",
                            color = if (isAnswerValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "$wordCount words • ${answerText.length} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                isError = !isAnswerValid && answerText.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 220.dp)
                    .testTag("candidate_answer_input"),
                maxLines = 10
            )

            // Starter template chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AssistChip(
                    onClick = onInsertPrompt,
                    label = { Text("Insert structure prompt", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * F. Interview Completion UI
 */
@Composable
private fun InterviewCompletedSection(
    session: InterviewSessionEntity?,
    questions: List<InterviewQuestionEntity>,
    onViewFinalReport: () -> Unit,
    onReturnToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                PrimaryBlue.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .testTag("interview_completed_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(StatusDemonstrated.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = StatusDemonstrated,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Interview Completed!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All adaptive questions for ${session?.roleTitle ?: "your target role"} have been evaluated by ForkAI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Real Session Summary Metrics Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val evaluatedQuestionsCount = questions.count { it.isEvaluated }
                    val totalDurationSec = questions.sumOf { it.answerDurationSeconds }
                    val competenciesCovered = questions.map { it.targetCompetency }.distinct().size

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Questions Completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$evaluatedQuestionsCount / ${session?.totalQuestions ?: evaluatedQuestionsCount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Competencies Assessed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$competenciesCovered core topics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Response Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%02d:%02d", totalDurationSec / 60, totalDurationSec % 60), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Interview Mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (session?.mode == InterviewMode.FORMAL) "Formal Assessment" else "Practice Mode",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onViewFinalReport,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("view_final_report_button")
                ) {
                    Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Final Assessment Report", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onReturnToDashboard,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("return_to_dashboard_button")
                ) {
                    Text("Return to Dashboard", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private tailrec fun Activity?.findActivity(): Activity? = this

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
