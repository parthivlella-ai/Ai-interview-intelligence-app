package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UserRole
import com.example.data.local.InterviewSessionEntity
import com.example.ui.components.SkillSenseTopAppBar
import com.example.ui.navigation.CandidateBottomNavBar
import com.example.ui.navigation.RecruiterBottomNavBar
import com.example.ui.screens.AiInterviewScreen
import com.example.ui.screens.AnswerFeedbackDialog
import com.example.ui.screens.CandidateDashboardScreen
import com.example.ui.screens.CandidatePracticeScreen
import com.example.ui.screens.CandidateProfileScreen
import com.example.ui.screens.CandidateReportsScreen
import com.example.ui.screens.CandidateSkillsScreen
import com.example.ui.screens.FinalAssessmentReportScreen
import com.example.ui.screens.NotificationCenterDialog
import com.example.ui.screens.RecruiterAssessmentsScreen
import com.example.ui.screens.RecruiterCandidatesScreen
import com.example.ui.screens.RecruiterDashboardScreen
import com.example.ui.screens.RecruiterProfileScreen
import com.example.ui.screens.RecruiterReviewDialog
import com.example.ui.screens.RecruiterSessionsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ResumeJobSetupScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuthScreen
import com.example.viewmodel.CandidateTab
import com.example.viewmodel.FocusedScreen
import com.example.viewmodel.RecruiterTab
import com.example.viewmodel.ScreenDestination
import com.example.viewmodel.SkillSenseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SkillSenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SkillSenseApp(
                    viewModel = viewModel,
                    onExitApp = { finish() }
                )
            }
        }
    }
}

@Composable
fun SkillSenseApp(
    viewModel: SkillSenseViewModel,
    onExitApp: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val candidateSessions by viewModel.candidateSessions.collectAsStateWithLifecycle()
    val allSkills by viewModel.allSkills.collectAsStateWithLifecycle()
    val allNotifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    val unreadNotifCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val questions by viewModel.currentSessionQuestions.collectAsStateWithLifecycle()
    val skills by viewModel.currentSessionSkills.collectAsStateWithLifecycle()
    val integrityEvents by viewModel.currentSessionIntegrityEvents.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var sessionToReview by remember { mutableStateOf<InterviewSessionEntity?>(null) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission handled gracefully */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Lifecycle observer for integrity monitoring during active interview
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uiState.focusedScreen, uiState.currentScreen) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (uiState.focusedScreen == FocusedScreen.AI_INTERVIEW || uiState.currentScreen == ScreenDestination.AI_INTERVIEW) {
                    viewModel.onAppFocusLost()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Comprehensive Material 3 Predictive Back Handling
    BackHandler(enabled = true) {
        when {
            // Unauthenticated state: back returns from login/register to welcome, or exits from welcome
            !uiState.isAuthenticated -> {
                when (uiState.authScreen) {
                    AuthScreen.LOGIN, AuthScreen.REGISTER -> {
                        viewModel.navigateToAuth(AuthScreen.WELCOME)
                    }
                    AuthScreen.WELCOME -> {
                        onExitApp()
                    }
                }
            }
            // 1. In a focused workflow (Setup, Active Interview, Final Report): dismiss back to main tab without losing session state
            uiState.focusedScreen != FocusedScreen.NONE -> {
                viewModel.dismissFocusedScreen()
            }
            // 2. Candidate secondary tab: back returns to Home tab
            uiState.currentRole == UserRole.CANDIDATE && uiState.candidateTab != CandidateTab.HOME -> {
                viewModel.selectCandidateTab(CandidateTab.HOME)
            }
            // 3. Recruiter secondary tab: back returns to Dashboard tab
            uiState.currentRole == UserRole.RECRUITER && uiState.recruiterTab != RecruiterTab.DASHBOARD -> {
                viewModel.selectRecruiterTab(RecruiterTab.DASHBOARD)
            }
            // 4. Primary dashboard tabs: exit app (prevents unexpectedly falling back to login screen without explicit logout)
            else -> {
                onExitApp()
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isAuthenticated && uiState.focusedScreen != FocusedScreen.WELCOME) {
                SkillSenseTopAppBar(
                    currentRole = uiState.currentRole,
                    unreadNotifCount = unreadNotifCount,
                    onRoleToggle = {
                        val nextRole = if (uiState.currentRole == UserRole.CANDIDATE) UserRole.RECRUITER else UserRole.CANDIDATE
                        viewModel.switchUserRole(nextRole)
                    },
                    onNotificationClick = {
                        viewModel.openNotificationDrawer(true)
                    },
                    onLogoutClick = {
                        viewModel.logout()
                    }
                )
            }
        },
        bottomBar = {
            // Keep bottom navigation visible on the main sections.
            // Hide it during unauthenticated auth screens (Welcome, Login, Register) and focused workflows (Setup, Interview, Report) per requirements.
            if (uiState.isAuthenticated && uiState.focusedScreen == FocusedScreen.NONE) {
                if (uiState.currentRole == UserRole.CANDIDATE) {
                    CandidateBottomNavBar(
                        selectedTab = uiState.candidateTab,
                        onTabSelected = { tab -> viewModel.selectCandidateTab(tab) }
                    )
                } else {
                    RecruiterBottomNavBar(
                        selectedTab = uiState.recruiterTab,
                        onTabSelected = { tab -> viewModel.selectRecruiterTab(tab) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Routing
            if (!uiState.isAuthenticated) {
                // 1. Unauthenticated User Experience (Welcome -> Login / Register)
                when (uiState.authScreen) {
                    AuthScreen.WELCOME -> {
                        WelcomeScreen(
                            onGetStarted = { viewModel.navigateToAuth(AuthScreen.REGISTER) },
                            onSignIn = { viewModel.navigateToAuth(AuthScreen.LOGIN) }
                        )
                    }

                    AuthScreen.LOGIN -> {
                        LoginScreen(
                            isLoading = uiState.isAuthLoading,
                            errorMessage = uiState.authErrorMessage,
                            onLogin = { email, password ->
                                viewModel.login(email, password)
                            },
                            onNavigateToRegister = {
                                viewModel.navigateToAuth(AuthScreen.REGISTER)
                            },
                            onBackToWelcome = {
                                viewModel.navigateToAuth(AuthScreen.WELCOME)
                            }
                        )
                    }

                    AuthScreen.REGISTER -> {
                        RegisterScreen(
                            isLoading = uiState.isAuthLoading,
                            errorMessage = uiState.authErrorMessage,
                            onRegister = { name, email, password, confirmPassword, role ->
                                viewModel.register(name, email, password, confirmPassword, role)
                            },
                            onNavigateToLogin = {
                                viewModel.navigateToAuth(AuthScreen.LOGIN)
                            },
                            onBackToWelcome = {
                                viewModel.navigateToAuth(AuthScreen.WELCOME)
                            }
                        )
                    }
                }
            } else if (uiState.focusedScreen != FocusedScreen.NONE) {
                // 2. Focused Workflows
                when (uiState.focusedScreen) {
                    FocusedScreen.WELCOME -> {
                        WelcomeScreen(
                            onGetStarted = { viewModel.dismissFocusedScreen() },
                            onSignIn = { viewModel.dismissFocusedScreen() }
                        )
                    }

                    FocusedScreen.RESUME_JOB_SETUP -> {
                        ResumeJobSetupScreen(
                            onBack = { viewModel.dismissFocusedScreen() },
                            onStartInterview = { roleTitle, department, jobDescription, resumeText, mode, questionCount ->
                                viewModel.startNewInterview(
                                    roleTitle = roleTitle,
                                    department = department,
                                    jobDescription = jobDescription,
                                    resumeText = resumeText,
                                    mode = mode,
                                    questionCount = questionCount
                                )
                            }
                        )
                    }

                    FocusedScreen.AI_INTERVIEW -> {
                        AiInterviewScreen(
                            session = currentSession,
                            questions = questions,
                            isEvaluating = uiState.isEvaluatingAnswer,
                            onSubmitAnswer = { qId, ans, duration ->
                                viewModel.submitAnswer(qId, ans, duration)
                            },
                            onAppFocusLost = {
                                viewModel.onAppFocusLost()
                            },
                            onPasteDetected = { charCount ->
                                viewModel.onTextPasted(charCount)
                            },
                            onExitInterview = {
                                viewModel.dismissFocusedScreen()
                            },
                            onViewFinalReport = {
                                viewModel.navigateTo(ScreenDestination.FINAL_REPORT)
                            }
                        )
                    }

                    FocusedScreen.FINAL_REPORT -> {
                        FinalAssessmentReportScreen(
                            session = currentSession,
                            questions = questions,
                            skills = skills,
                            integrityEvents = integrityEvents,
                            onBack = { viewModel.dismissFocusedScreen() },
                            authorizedCandidateId = if (uiState.currentRole == UserRole.CANDIDATE) uiState.authenticatedUser?.id else null
                        )
                    }

                    FocusedScreen.NONE -> { /* Handled in main tabs below */ }
                }
            } else {
                // Persistent Tabbed Destinations
                if (uiState.currentRole == UserRole.CANDIDATE) {
                    // Candidate Area (5 destinations)
                    when (uiState.candidateTab) {
                        CandidateTab.HOME -> {
                            CandidateDashboardScreen(
                                sessions = candidateSessions,
                                candidateName = uiState.authenticatedUser?.name,
                                skills = skills,
                                onStartNewInterview = {
                                    viewModel.navigateTo(ScreenDestination.RESUME_JOB_SETUP)
                                },
                                onSetupResume = {
                                    viewModel.navigateTo(ScreenDestination.RESUME_JOB_SETUP)
                                },
                                onViewReport = { sessionId ->
                                    viewModel.selectSession(sessionId)
                                    viewModel.navigateTo(ScreenDestination.FINAL_REPORT)
                                },
                                onNavigateToSkills = {
                                    viewModel.selectCandidateTab(CandidateTab.SKILLS)
                                }
                            )
                        }

                        CandidateTab.PRACTICE -> {
                            CandidatePracticeScreen(
                                onStartPracticeSession = {
                                    viewModel.navigateTo(ScreenDestination.RESUME_JOB_SETUP)
                                }
                            )
                        }

                        CandidateTab.SKILLS -> {
                            CandidateSkillsScreen(
                                skills = skills,
                                candidateName = uiState.authenticatedUser?.name,
                                questions = questions,
                                onStartEvaluation = {
                                    viewModel.navigateTo(ScreenDestination.RESUME_JOB_SETUP)
                                }
                            )
                        }

                        CandidateTab.REPORTS -> {
                            CandidateReportsScreen(
                                sessions = candidateSessions,
                                onViewReport = { sessionId ->
                                    viewModel.selectSession(sessionId)
                                    viewModel.navigateTo(ScreenDestination.FINAL_REPORT)
                                },
                                onStartNewInterview = {
                                    viewModel.navigateTo(ScreenDestination.RESUME_JOB_SETUP)
                                }
                            )
                        }

                        CandidateTab.PROFILE -> {
                            CandidateProfileScreen(
                                userName = uiState.authenticatedUser?.name ?: "Alex Rivera",
                                userEmail = uiState.authenticatedUser?.email ?: "alex.rivera@example.com",
                                onLogout = { viewModel.logout() }
                            )
                        }
                    }
                } else {
                    // Recruiter Area (5 destinations)
                    when (uiState.recruiterTab) {
                        RecruiterTab.DASHBOARD -> {
                            RecruiterDashboardScreen(
                                sessions = allSessions,
                                onInspectReport = { sessionId ->
                                    viewModel.selectSession(sessionId)
                                    viewModel.navigateTo(ScreenDestination.FINAL_REPORT)
                                },
                                onSaveRecruiterReview = { sessionId, notes, verdict ->
                                    viewModel.updateRecruiterReview(sessionId, notes, verdict)
                                },
                                onCreateNewSession = {
                                    viewModel.navigateTo(ScreenDestination.RESUME_JOB_SETUP)
                                }
                            )
                        }

                        RecruiterTab.CANDIDATES -> {
                            RecruiterCandidatesScreen(
                                sessions = allSessions,
                                allSkills = allSkills,
                                onInspectReport = { sessionId ->
                                    viewModel.selectSession(sessionId)
                                    viewModel.navigateTo(ScreenDestination.FINAL_REPORT)
                                },
                                onOpenReview = { session ->
                                    sessionToReview = session
                                }
                            )
                        }

                        RecruiterTab.SESSIONS -> {
                            RecruiterSessionsScreen(
                                sessions = allSessions,
                                onInspectReport = { sessionId ->
                                    viewModel.selectSession(sessionId)
                                    viewModel.navigateTo(ScreenDestination.FINAL_REPORT)
                                }
                            )
                        }

                        RecruiterTab.ASSESSMENTS -> {
                            RecruiterAssessmentsScreen(
                                sessions = allSessions,
                                onOpenReview = { session ->
                                    sessionToReview = session
                                }
                            )
                        }

                        RecruiterTab.PROFILE -> {
                            RecruiterProfileScreen(
                                recruiterName = uiState.authenticatedUser?.name ?: "Elena Rostova",
                                recruiterEmail = uiState.authenticatedUser?.email ?: "elena.recruiter@techscale.io",
                                onLogout = { viewModel.logout() }
                            )
                        }
                    }
                }
            }

            // Practice Mode Feedback Dialog
            if (uiState.showFeedbackDialog && uiState.currentEvaluationResult != null) {
                AnswerFeedbackDialog(
                    evaluation = uiState.currentEvaluationResult,
                    onDismiss = { viewModel.dismissFeedbackDialog() },
                    onContinueNext = {
                        viewModel.dismissFeedbackDialog()
                        if (currentSession?.isCompleted == true) {
                            viewModel.navigateTo(ScreenDestination.FINAL_REPORT)
                        }
                    }
                )
            }

            // Real-time Notification Center Modal
            if (uiState.showNotificationDrawer) {
                NotificationCenterDialog(
                    notifications = allNotifications,
                    onDismiss = { viewModel.openNotificationDrawer(false) },
                    onNotificationClick = { notif ->
                        viewModel.markNotificationAsRead(notif.id)
                        if (notif.targetSessionId != null) {
                            viewModel.selectSession(notif.targetSessionId)
                            viewModel.navigateTo(ScreenDestination.FINAL_REPORT)
                            viewModel.openNotificationDrawer(false)
                        }
                    },
                    onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
                    onClearAll = { viewModel.clearAllNotifications() }
                )
            }

            // Recruiter Assessment Review Modal from candidates or assessments tab
            sessionToReview?.let { targetSession ->
                RecruiterReviewDialog(
                    session = targetSession,
                    onDismiss = { sessionToReview = null },
                    onSave = { sessionId, notes, verdict ->
                        viewModel.updateRecruiterReview(sessionId, notes, verdict)
                        sessionToReview = null
                    }
                )
            }
        }
    }
}
