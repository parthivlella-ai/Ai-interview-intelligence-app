package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EvaluatedAnswer
import com.example.data.InterviewMode
import com.example.data.UserRole
import com.example.data.local.InterviewQuestionEntity
import com.example.data.local.InterviewSessionEntity
import com.example.data.local.NotificationItemEntity
import com.example.data.local.SkillAssessmentEntity
import com.example.data.local.IntegrityEventEntity
import com.example.data.local.UserEntity
import com.example.data.repository.SkillSenseRepository
import com.example.service.AdaptiveInterviewEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    WELCOME,
    CANDIDATE_DASHBOARD,
    RESUME_JOB_SETUP,
    AI_INTERVIEW,
    FINAL_REPORT,
    RECRUITER_DASHBOARD
}

enum class CandidateTab {
    HOME,
    PRACTICE,
    SKILLS,
    REPORTS,
    PROFILE
}

enum class RecruiterTab {
    DASHBOARD,
    CANDIDATES,
    SESSIONS,
    ASSESSMENTS,
    PROFILE
}

enum class FocusedScreen {
    NONE,
    WELCOME,
    RESUME_JOB_SETUP,
    AI_INTERVIEW,
    FINAL_REPORT
}

enum class AuthScreen {
    WELCOME,
    LOGIN,
    REGISTER
}

data class UiState(
    val isAuthenticated: Boolean = false,
    val authenticatedUser: UserEntity? = null,
    val authScreen: AuthScreen = AuthScreen.WELCOME,
    val authErrorMessage: String? = null,
    val isAuthLoading: Boolean = false,

    val currentRole: UserRole = UserRole.CANDIDATE,
    val currentScreen: ScreenDestination = ScreenDestination.CANDIDATE_DASHBOARD,
    val candidateTab: CandidateTab = CandidateTab.HOME,
    val recruiterTab: RecruiterTab = RecruiterTab.DASHBOARD,
    val focusedScreen: FocusedScreen = FocusedScreen.NONE,
    val selectedSessionId: String? = null,
    val isEvaluatingAnswer: Boolean = false,
    val currentEvaluationResult: EvaluatedAnswer? = null,
    val showFeedbackDialog: Boolean = false,
    val showNotificationDrawer: Boolean = false,
    val isSpeechListening: Boolean = false
)

class SkillSenseViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SkillSenseRepository(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val allSessions: StateFlow<List<InterviewSessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Candidate-authorized sessions: candidates see only their own sessions (sample session belongs to candidate_default)
    val candidateSessions: StateFlow<List<InterviewSessionEntity>> = combine(
        allSessions,
        _uiState
    ) { sessions, state ->
        val user = state.authenticatedUser
        if (user != null && state.currentRole == UserRole.CANDIDATE) {
            sessions.filter { it.candidateId == user.id }
        } else {
            sessions
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSkills: StateFlow<List<SkillAssessmentEntity>> = repository.getAllSkills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotifications: StateFlow<List<NotificationItemEntity>> = repository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.getUnreadNotificationsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Current Session Observables
    private val _currentSessionQuestions = MutableStateFlow<List<InterviewQuestionEntity>>(emptyList())
    val currentSessionQuestions: StateFlow<List<InterviewQuestionEntity>> = _currentSessionQuestions.asStateFlow()

    private val _currentSessionSkills = MutableStateFlow<List<SkillAssessmentEntity>>(emptyList())
    val currentSessionSkills: StateFlow<List<SkillAssessmentEntity>> = _currentSessionSkills.asStateFlow()

    private val _currentSessionIntegrityEvents = MutableStateFlow<List<IntegrityEventEntity>>(emptyList())
    val currentSessionIntegrityEvents: StateFlow<List<IntegrityEventEntity>> = _currentSessionIntegrityEvents.asStateFlow()

    private val _currentSession = MutableStateFlow<InterviewSessionEntity?>(null)
    val currentSession: StateFlow<InterviewSessionEntity?> = _currentSession.asStateFlow()

    private var sessionObservationJob: Job? = null

    private fun observeSession(sessionId: String?) {
        sessionObservationJob?.cancel()
        if (sessionId == null) {
            _currentSession.value = null
            _currentSessionQuestions.value = emptyList()
            _currentSessionSkills.value = emptyList()
            _currentSessionIntegrityEvents.value = emptyList()
            return
        }

        sessionObservationJob = viewModelScope.launch {
            launch {
                repository.getSessionFlow(sessionId).collect { session ->
                    _currentSession.value = session
                }
            }
            launch {
                repository.getQuestionsForSession(sessionId).collect { questions ->
                    _currentSessionQuestions.value = questions
                }
            }
            launch {
                repository.getSkillsForSession(sessionId).collect { skills ->
                    _currentSessionSkills.value = skills
                }
            }
            launch {
                repository.getIntegrityEventsForSession(sessionId).collect { events ->
                    _currentSessionIntegrityEvents.value = events
                }
            }
        }
    }

    init {
        // 1. Restore authenticated session on app launch if available
        viewModelScope.launch {
            val session = repository.authRepository.getActiveSession()
            if (session != null) {
                val user = repository.getUser(session.userId)
                if (user != null) {
                    val userSessions = if (session.userRole == UserRole.CANDIDATE) {
                        repository.getSessionsForCandidate(user.id).firstOrNull() ?: emptyList()
                    } else {
                        repository.getAllSessions().firstOrNull() ?: emptyList()
                    }
                    val initialSessionId = userSessions.firstOrNull()?.id ?: if (session.userRole == UserRole.RECRUITER) "sample_session_101" else null

                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        authenticatedUser = user,
                        currentRole = session.userRole,
                        candidateTab = CandidateTab.HOME,
                        recruiterTab = RecruiterTab.DASHBOARD,
                        focusedScreen = FocusedScreen.NONE,
                        selectedSessionId = initialSessionId
                    )
                } else {
                    repository.authRepository.logout()
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = false,
                        authenticatedUser = null,
                        authScreen = AuthScreen.WELCOME,
                        selectedSessionId = null
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = false,
                    authenticatedUser = null,
                    authScreen = AuthScreen.WELCOME,
                    selectedSessionId = null
                )
            }
        }

        // 2. Observe selected session data with cancel-on-change to prevent leaks & race conditions
        viewModelScope.launch {
            _uiState
                .map { it.selectedSessionId }
                .distinctUntilChanged()
                .collect { sessionId ->
                    observeSession(sessionId)
                }
        }
    }

    // --- Authentication & Access Control ---

    fun navigateToAuth(screen: AuthScreen) {
        _uiState.value = _uiState.value.copy(
            authScreen = screen,
            authErrorMessage = null
        )
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAuthLoading = true, authErrorMessage = null)
            val result = repository.authRepository.login(email, password)
            result.onSuccess { user ->
                val userSessions = if (user.role == UserRole.CANDIDATE) {
                    repository.getSessionsForCandidate(user.id).firstOrNull() ?: emptyList()
                } else {
                    repository.getAllSessions().firstOrNull() ?: emptyList()
                }
                val initialSessionId = userSessions.firstOrNull()?.id ?: if (user.role == UserRole.RECRUITER) "sample_session_101" else null

                _uiState.value = _uiState.value.copy(
                    isAuthLoading = false,
                    isAuthenticated = true,
                    authenticatedUser = user,
                    currentRole = user.role,
                    candidateTab = CandidateTab.HOME,
                    recruiterTab = RecruiterTab.DASHBOARD,
                    focusedScreen = FocusedScreen.NONE,
                    authErrorMessage = null,
                    selectedSessionId = initialSessionId
                )
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    isAuthLoading = false,
                    authErrorMessage = ex.message ?: "Authentication failed. Please verify your credentials."
                )
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAuthLoading = true, authErrorMessage = null)
            val result = repository.authRepository.register(name, email, password, confirmPassword, role)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isAuthLoading = false,
                    isAuthenticated = true,
                    authenticatedUser = user,
                    currentRole = user.role,
                    candidateTab = CandidateTab.HOME,
                    recruiterTab = RecruiterTab.DASHBOARD,
                    focusedScreen = FocusedScreen.NONE,
                    authErrorMessage = null,
                    selectedSessionId = null
                )
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    isAuthLoading = false,
                    authErrorMessage = ex.message ?: "Account registration failed. Please check your inputs."
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.authRepository.logout()
            _uiState.value = _uiState.value.copy(
                isAuthenticated = false,
                authenticatedUser = null,
                authScreen = AuthScreen.WELCOME,
                authErrorMessage = null,
                focusedScreen = FocusedScreen.NONE,
                selectedSessionId = null
            )
            _currentSession.value = null
            _currentSessionQuestions.value = emptyList()
            _currentSessionSkills.value = emptyList()
            _currentSessionIntegrityEvents.value = emptyList()
        }
    }

    // --- Navigation Controls ---

    fun switchUserRole(role: UserRole) {
        _uiState.value = _uiState.value.copy(
            currentRole = role,
            focusedScreen = FocusedScreen.NONE,
            currentScreen = if (role == UserRole.CANDIDATE) ScreenDestination.CANDIDATE_DASHBOARD else ScreenDestination.RECRUITER_DASHBOARD
        )
    }

    fun selectCandidateTab(tab: CandidateTab) {
        _uiState.value = _uiState.value.copy(
            currentRole = UserRole.CANDIDATE,
            candidateTab = tab,
            focusedScreen = FocusedScreen.NONE,
            currentScreen = ScreenDestination.CANDIDATE_DASHBOARD
        )
    }

    fun selectRecruiterTab(tab: RecruiterTab) {
        _uiState.value = _uiState.value.copy(
            currentRole = UserRole.RECRUITER,
            recruiterTab = tab,
            focusedScreen = FocusedScreen.NONE,
            currentScreen = ScreenDestination.RECRUITER_DASHBOARD
        )
    }

    fun dismissFocusedScreen() {
        _uiState.value = _uiState.value.copy(
            focusedScreen = FocusedScreen.NONE,
            currentScreen = if (_uiState.value.currentRole == UserRole.CANDIDATE) ScreenDestination.CANDIDATE_DASHBOARD else ScreenDestination.RECRUITER_DASHBOARD
        )
    }

    fun navigateTo(screen: ScreenDestination) {
        when (screen) {
            ScreenDestination.WELCOME -> {
                _uiState.value = _uiState.value.copy(
                    focusedScreen = FocusedScreen.WELCOME,
                    currentScreen = ScreenDestination.WELCOME
                )
            }
            ScreenDestination.RESUME_JOB_SETUP -> {
                _uiState.value = _uiState.value.copy(
                    focusedScreen = FocusedScreen.RESUME_JOB_SETUP,
                    currentScreen = ScreenDestination.RESUME_JOB_SETUP
                )
            }
            ScreenDestination.AI_INTERVIEW -> {
                _uiState.value = _uiState.value.copy(
                    focusedScreen = FocusedScreen.AI_INTERVIEW,
                    currentScreen = ScreenDestination.AI_INTERVIEW
                )
            }
            ScreenDestination.FINAL_REPORT -> {
                _uiState.value = _uiState.value.copy(
                    focusedScreen = FocusedScreen.FINAL_REPORT,
                    currentScreen = ScreenDestination.FINAL_REPORT
                )
            }
            ScreenDestination.CANDIDATE_DASHBOARD -> {
                _uiState.value = _uiState.value.copy(
                    currentRole = UserRole.CANDIDATE,
                    candidateTab = CandidateTab.HOME,
                    focusedScreen = FocusedScreen.NONE,
                    currentScreen = ScreenDestination.CANDIDATE_DASHBOARD
                )
            }
            ScreenDestination.RECRUITER_DASHBOARD -> {
                _uiState.value = _uiState.value.copy(
                    currentRole = UserRole.RECRUITER,
                    recruiterTab = RecruiterTab.DASHBOARD,
                    focusedScreen = FocusedScreen.NONE,
                    currentScreen = ScreenDestination.RECRUITER_DASHBOARD
                )
            }
        }
    }

    fun selectSession(sessionId: String) {
        _uiState.value = _uiState.value.copy(selectedSessionId = sessionId)
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            if (session != null) {
                _currentSession.value = session
                _currentSessionQuestions.value = repository.getQuestions(sessionId)
                _currentSessionSkills.value = repository.getSkills(sessionId)
                _currentSessionIntegrityEvents.value = repository.getIntegrityEvents(sessionId)
            }
        }
    }

    fun openNotificationDrawer(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNotificationDrawer = show)
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    fun setSpeechListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(isSpeechListening = listening)
    }

    fun dismissFeedbackDialog() {
        _uiState.value = _uiState.value.copy(
            showFeedbackDialog = false,
            currentEvaluationResult = null
        )
    }

    // Start a new adaptive interview session
    fun startNewInterview(
        roleTitle: String,
        department: String,
        jobDescription: String,
        resumeText: String,
        mode: InterviewMode,
        questionCount: Int = 4
    ) {
        viewModelScope.launch {
            val user = _uiState.value.authenticatedUser
            val candidateId = user?.id ?: "candidate_default"
            val candidateName = user?.name ?: "Candidate"

            val newSessionId = repository.createInterviewSession(
                candidateId = candidateId,
                candidateName = candidateName,
                roleTitle = roleTitle,
                department = department,
                jobDescription = jobDescription,
                resumeSnippet = resumeText,
                mode = mode,
                questionCount = questionCount
            )

            // Immediately load the created session & questions from database
            val session = repository.getSession(newSessionId)
            val questions = repository.getQuestions(newSessionId)
            val skills = repository.getSkills(newSessionId)
            _currentSession.value = session
            _currentSessionQuestions.value = questions
            _currentSessionSkills.value = skills

            _uiState.value = _uiState.value.copy(
                selectedSessionId = newSessionId,
                focusedScreen = FocusedScreen.AI_INTERVIEW,
                currentScreen = ScreenDestination.AI_INTERVIEW
            )
        }
    }

    // Submit answer to the active question
    fun submitAnswer(
        questionId: String,
        answer: String,
        durationSeconds: Int
    ) {
        val sessionId = _uiState.value.selectedSessionId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEvaluatingAnswer = true)

            val evaluation = repository.submitAnswerAndEvaluate(
                sessionId = sessionId,
                questionId = questionId,
                answer = answer,
                durationSeconds = durationSeconds
            )

            // Immediately load the fresh state from Room DB to prevent async state races
            val updatedSession = repository.getSession(sessionId)
            val updatedQuestions = repository.getQuestions(sessionId)
            val updatedSkills = repository.getSkills(sessionId)
            _currentSession.value = updatedSession
            _currentSessionQuestions.value = updatedQuestions
            _currentSessionSkills.value = updatedSkills

            val isPractice = updatedSession?.mode == InterviewMode.PRACTICE
            val isAllCompleted = updatedSession?.isCompleted == true

            _uiState.value = _uiState.value.copy(
                isEvaluatingAnswer = false,
                currentEvaluationResult = evaluation,
                showFeedbackDialog = isPractice // Show instant feedback if practice mode
            )

            // If session reached completion, transition to final assessment report if formal mode or after user finishes
            if (isAllCompleted && !isPractice) {
                _uiState.value = _uiState.value.copy(
                    focusedScreen = FocusedScreen.FINAL_REPORT,
                    currentScreen = ScreenDestination.FINAL_REPORT
                )
            }
        }
    }

    // Integrity tracking event trigger
    fun onAppFocusLost() {
        val sessionId = _uiState.value.selectedSessionId ?: return
        if (_uiState.value.currentScreen == ScreenDestination.AI_INTERVIEW || _uiState.value.focusedScreen == FocusedScreen.AI_INTERVIEW) {
            repository.integrityMonitoringService.recordFocusLost(sessionId)
        }
    }

    fun onTextPasted(charCount: Int) {
        val sessionId = _uiState.value.selectedSessionId ?: return
        if ((_uiState.value.currentScreen == ScreenDestination.AI_INTERVIEW || _uiState.value.focusedScreen == FocusedScreen.AI_INTERVIEW) && charCount > 250) {
            repository.integrityMonitoringService.recordRapidPasteAnomaly(sessionId, charCount)
        }
    }

    fun onCameraStatusChange(reason: String) {
        val sessionId = _uiState.value.selectedSessionId ?: return
        if (_uiState.value.currentScreen == ScreenDestination.AI_INTERVIEW || _uiState.value.focusedScreen == FocusedScreen.AI_INTERVIEW) {
            repository.integrityMonitoringService.recordCameraFeedInterrupted(sessionId, reason)
        }
    }

    // Recruiter updates notes & verdict
    fun updateRecruiterReview(sessionId: String, notes: String, verdict: String) {
        viewModelScope.launch {
            repository.updateRecruiterReview(sessionId, notes, verdict)
        }
    }
}
