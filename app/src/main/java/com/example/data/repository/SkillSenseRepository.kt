package com.example.data.repository

import android.content.Context
import com.example.data.EvaluatedAnswer
import com.example.data.InterviewMode
import com.example.data.QuestionDifficulty
import com.example.data.SkillStatus
import com.example.data.UserRole
import com.example.data.local.IntegrityEventEntity
import com.example.data.local.InterviewQuestionEntity
import com.example.data.local.InterviewSessionEntity
import com.example.data.local.NotificationItemEntity
import com.example.data.local.SkillAssessmentEntity
import com.example.data.local.SkillSenseDatabase
import com.example.data.local.UserEntity
import com.example.service.AdaptiveInterviewEngine
import com.example.service.GeminiInterviewService
import com.example.service.IntegrityMonitoringService
import com.example.service.NotificationService
import com.example.service.QuestionHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class SkillSenseRepository(context: Context) {
    private val database = SkillSenseDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val interviewDao = database.interviewDao()
    private val notificationDao = database.notificationDao()

    val notificationService = NotificationService(context)
    val integrityMonitoringService = IntegrityMonitoringService(database, notificationService)
    val authRepository = AuthRepository(database)

    // Reactive streams
    fun getAllSessions(): Flow<List<InterviewSessionEntity>> = interviewDao.getAllSessions()
    fun getSessionsForCandidate(candidateId: String): Flow<List<InterviewSessionEntity>> =
        interviewDao.getSessionsForCandidate(candidateId)

    fun getSessionFlow(sessionId: String): Flow<InterviewSessionEntity?> =
        interviewDao.getSessionByIdFlow(sessionId)

    fun getQuestionsForSession(sessionId: String): Flow<List<InterviewQuestionEntity>> =
        interviewDao.getQuestionsForSessionFlow(sessionId)

    fun getSkillsForSession(sessionId: String): Flow<List<SkillAssessmentEntity>> =
        interviewDao.getSkillsForSessionFlow(sessionId)

    fun getAllSkills(): Flow<List<SkillAssessmentEntity>> =
        interviewDao.getAllSkillsFlow()

    fun getIntegrityEventsForSession(sessionId: String) =
        interviewDao.getIntegrityEventsForSessionFlow(sessionId)

    fun getNotifications(): Flow<List<NotificationItemEntity>> =
        notificationDao.getAllNotificationsFlow()

    fun getUnreadNotificationsCount(): Flow<Int> =
        notificationDao.getUnreadCountFlow()

    suspend fun getSession(sessionId: String): InterviewSessionEntity? = withContext(Dispatchers.IO) {
        interviewDao.getSessionById(sessionId)
    }

    suspend fun getQuestions(sessionId: String): List<InterviewQuestionEntity> = withContext(Dispatchers.IO) {
        interviewDao.getQuestionsForSession(sessionId)
    }

    suspend fun getSkills(sessionId: String): List<SkillAssessmentEntity> = withContext(Dispatchers.IO) {
        interviewDao.getSkillsForSession(sessionId)
    }

    suspend fun getIntegrityEvents(sessionId: String): List<IntegrityEventEntity> = withContext(Dispatchers.IO) {
        interviewDao.getIntegrityEventsForSession(sessionId)
    }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead()
    }

    suspend fun clearNotifications() = withContext(Dispatchers.IO) {
        notificationDao.clearAllNotifications()
    }

    suspend fun getUser(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    suspend fun saveUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    /**
     * Initializes a new Adaptive Interview Session with extracted competencies & initial question
     */
    suspend fun createInterviewSession(
        candidateId: String,
        candidateName: String,
        roleTitle: String,
        department: String,
        jobDescription: String,
        resumeSnippet: String,
        mode: InterviewMode,
        questionCount: Int = 4
    ): String = withContext(Dispatchers.IO) {
        val sessionId = UUID.randomUUID().toString()

        val session = InterviewSessionEntity(
            id = sessionId,
            candidateId = candidateId,
            candidateName = candidateName,
            roleTitle = roleTitle,
            roleDepartment = department,
            jobDescription = jobDescription,
            resumeSnippet = resumeSnippet,
            mode = mode,
            totalQuestions = questionCount,
            currentQuestionIndex = 0,
            isCompleted = false,
            overallScore = 0,
            summaryStrengths = "",
            summaryGaps = "",
            recruiterNotes = "",
            recruiterVerdict = "PENDING_REVIEW"
        )
        interviewDao.insertSession(session)

        // Seed initial competencies from role requirements and resume claims
        val roleObj = AdaptiveInterviewEngine.AVAILABLE_ROLES.firstOrNull { it.title == roleTitle }
        val competencies = roleObj?.keyCompetencies ?: listOf(
            "Core Technical Knowledge",
            "System Architecture",
            "Problem Solving",
            "Testing & Resilience"
        )

        val skillEntities = competencies.mapIndexed { index, comp ->
            val isClaimed = resumeSnippet.contains(comp.split(" ").first(), ignoreCase = true)
            SkillAssessmentEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                skillName = comp,
                domainCategory = when {
                    comp.contains("UI", ignoreCase = true) -> "UI & Layout"
                    comp.contains("Data", ignoreCase = true) || comp.contains("Room", ignoreCase = true) || comp.contains("Database", ignoreCase = true) -> "Persistence & Data"
                    comp.contains("Concurrency", ignoreCase = true) || comp.contains("Coroutines", ignoreCase = true) -> "Concurrency"
                    comp.contains("Performance", ignoreCase = true) || comp.contains("Memory", ignoreCase = true) -> "Performance"
                    else -> "Engineering Architecture"
                },
                status = if (isClaimed) SkillStatus.CLAIMED_UNVALIDATED else SkillStatus.NOT_ASSESSED,
                evidenceSnippet = if (isClaimed) "Claimed on resume; awaiting interview validation." else "Required for role; unassessed.",
                proficiencyPercent = if (isClaimed) 50 else 0
            )
        }
        interviewDao.insertSkills(skillEntities)

        // Generate the first adaptive question (Live Gemini AI when configured, rule-based catalog fallback)
        val firstCompetency = roleObj?.keyCompetencies?.firstOrNull() ?: "Core Architecture & Principles"
        val (firstQ, reason) = if (GeminiInterviewService.isAiConfigured()) {
            val aiResult = GeminiInterviewService.generateAdaptiveQuestion(
                roleTitle = roleTitle,
                jobDescription = jobDescription,
                resumeSnippet = resumeSnippet,
                questionIndex = 1,
                totalQuestions = questionCount,
                targetCompetency = firstCompetency,
                difficulty = QuestionDifficulty.INTERMEDIATE,
                previousContext = emptyList()
            )
            aiResult.getOrElse {
                AdaptiveInterviewEngine.selectAdaptiveQuestion(
                    roleTitle = roleTitle,
                    questionIndex = 1,
                    totalQuestions = questionCount,
                    previousScores = emptyList(),
                    resumeText = resumeSnippet,
                    testedCompetencies = emptySet()
                )
            }
        } else {
            AdaptiveInterviewEngine.selectAdaptiveQuestion(
                roleTitle = roleTitle,
                questionIndex = 1,
                totalQuestions = questionCount,
                previousScores = emptyList(),
                resumeText = resumeSnippet,
                testedCompetencies = emptySet()
            )
        }

        val firstQuestionEntity = InterviewQuestionEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            orderIndex = 1,
            questionText = firstQ.questionText,
            targetCompetency = firstQ.targetCompetency,
            difficulty = firstQ.difficulty,
            adaptiveReason = reason,
            candidateAnswer = "",
            answerDurationSeconds = 0,
            isEvaluated = false
        )
        interviewDao.insertQuestion(firstQuestionEntity)

        // Dispatch real-time notification
        notificationService.notifySessionStarted(sessionId, roleTitle)

        sessionId
    }

    /**
     * Submits candidate's answer to current question, evaluates evidence and uncertainty,
     * updates competency graph, and selects next adaptive question or finalizes report.
     */
    suspend fun submitAnswerAndEvaluate(
        sessionId: String,
        questionId: String,
        answer: String,
        durationSeconds: Int
    ): EvaluatedAnswer = withContext(Dispatchers.IO) {
        val session = interviewDao.getSessionById(sessionId) ?: throw IllegalStateException("Session not found")
        val questions = interviewDao.getQuestionsForSession(sessionId)
        val currentQ = questions.firstOrNull { it.id == questionId } ?: throw IllegalStateException("Question not found")

        // If question was already evaluated, return the saved evaluation safely
        if (currentQ.isEvaluated) {
            return@withContext EvaluatedAnswer(
                score = currentQ.score,
                correctness = currentQ.correctnessDetails,
                missingPoints = currentQ.missingDetails,
                supportingEvidence = currentQ.evidenceExtracted,
                uncertaintyLevel = currentQ.uncertaintyLevel,
                uncertaintyReason = currentQ.uncertaintyReason,
                skillUpdated = currentQ.targetCompetency,
                skillStatusResult = if (currentQ.score >= 70) SkillStatus.DEMONSTRATED else SkillStatus.NOT_DEMONSTRATED
            )
        }

        // Look up expected keywords from catalog
        val roleCatalog = AdaptiveInterviewEngine.getQuestionsForRole(session.roleTitle, session.resumeSnippet)
        val catalogItem = roleCatalog.firstOrNull { it.targetCompetency == currentQ.targetCompetency }
        val keywords = catalogItem?.keywordsExpected ?: listOf("architecture", "performance", "state", "scalability", "testing")

        // Check for typing anomalies (Integrity tracking)
        val wordCount = answer.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
        integrityMonitoringService.recordTypingBurstAnomaly(sessionId, durationSeconds, wordCount)

        // Perform Evidence-based AI evaluation (Live Gemini AI with fallback)
        val evaluation = if (GeminiInterviewService.isAiConfigured()) {
            val aiEvalResult = GeminiInterviewService.evaluateAnswerWithAi(
                questionText = currentQ.questionText,
                targetCompetency = currentQ.targetCompetency,
                candidateAnswer = answer,
                difficulty = currentQ.difficulty,
                expectedKeywords = keywords
            )
            aiEvalResult.getOrElse {
                AdaptiveInterviewEngine.evaluateAnswer(
                    questionText = currentQ.questionText,
                    targetCompetency = currentQ.targetCompetency,
                    answer = answer,
                    difficulty = currentQ.difficulty,
                    expectedKeywords = keywords
                )
            }
        } else {
            AdaptiveInterviewEngine.evaluateAnswer(
                questionText = currentQ.questionText,
                targetCompetency = currentQ.targetCompetency,
                answer = answer,
                difficulty = currentQ.difficulty,
                expectedKeywords = keywords
            )
        }

        // Update Question Entity
        val updatedQ = currentQ.copy(
            candidateAnswer = answer,
            answerDurationSeconds = durationSeconds,
            score = evaluation.score,
            correctnessDetails = evaluation.correctness,
            missingDetails = evaluation.missingPoints,
            evidenceExtracted = evaluation.supportingEvidence,
            uncertaintyLevel = evaluation.uncertaintyLevel,
            uncertaintyReason = evaluation.uncertaintyReason,
            isEvaluated = true
        )
        interviewDao.updateQuestion(updatedQ)

        // Update or insert the candidate's skill in the database
        val existingSkills = interviewDao.getSkillsForSession(sessionId)
        val matchingSkill = existingSkills.firstOrNull { it.skillName.equals(currentQ.targetCompetency, ignoreCase = true) }
        if (matchingSkill != null) {
            val updatedSkill = matchingSkill.copy(
                status = evaluation.skillStatusResult,
                evidenceSnippet = evaluation.supportingEvidence,
                proficiencyPercent = evaluation.score
            )
            interviewDao.updateSkill(updatedSkill)
        } else {
            // Competency was dynamically assessed: persist new skill assessment
            val newSkill = SkillAssessmentEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                skillName = currentQ.targetCompetency,
                domainCategory = when {
                    currentQ.targetCompetency.contains("UI", ignoreCase = true) -> "UI Architecture"
                    currentQ.targetCompetency.contains("Data", ignoreCase = true) || currentQ.targetCompetency.contains("Room", ignoreCase = true) -> "Persistence & Data"
                    currentQ.targetCompetency.contains("Concurrency", ignoreCase = true) || currentQ.targetCompetency.contains("Coroutines", ignoreCase = true) -> "Concurrency"
                    currentQ.targetCompetency.contains("Performance", ignoreCase = true) || currentQ.targetCompetency.contains("Memory", ignoreCase = true) -> "Performance"
                    else -> "Engineering Architecture"
                },
                status = evaluation.skillStatusResult,
                evidenceSnippet = evaluation.supportingEvidence,
                proficiencyPercent = evaluation.score
            )
            interviewDao.insertSkills(listOf(newSkill))
        }

        // Notify competency updated
        notificationService.notifyAnswerEvaluated(sessionId, currentQ.targetCompetency, evaluation.score)

        // Check if more questions are needed
        val allEvaluatedQuestions = interviewDao.getQuestionsForSession(sessionId).filter { it.isEvaluated }
        val nextIndex = allEvaluatedQuestions.size + 1

        if (nextIndex <= session.totalQuestions) {
            // Adaptive generation of next question
            val testedCompetencies = allEvaluatedQuestions.map { it.targetCompetency }.toSet()
            val previousScores = allEvaluatedQuestions.map { it.score }

            // Determine target competency for next question
            val roleObj = AdaptiveInterviewEngine.AVAILABLE_ROLES.firstOrNull { it.title == session.roleTitle }
            val nextCompetency = roleObj?.keyCompetencies?.firstOrNull { it !in testedCompetencies }
                ?: roleCatalog.firstOrNull { it.targetCompetency !in testedCompetencies }?.targetCompetency
                ?: currentQ.targetCompetency

            val avgScore = if (previousScores.isNotEmpty()) previousScores.average().toInt() else 75
            val nextDifficulty = when {
                avgScore >= 85 -> QuestionDifficulty.EXPERT
                avgScore >= 70 -> QuestionDifficulty.ADVANCED
                avgScore <= 55 -> QuestionDifficulty.FOUNDATIONAL
                else -> QuestionDifficulty.INTERMEDIATE
            }

            val (nextQTemplate, nextReason) = if (GeminiInterviewService.isAiConfigured()) {
                val history = allEvaluatedQuestions.map {
                    QuestionHistoryItem(
                        orderIndex = it.orderIndex,
                        questionText = it.questionText,
                        targetCompetency = it.targetCompetency,
                        candidateAnswer = it.candidateAnswer,
                        score = it.score
                    )
                }
                val aiResult = GeminiInterviewService.generateAdaptiveQuestion(
                    roleTitle = session.roleTitle,
                    jobDescription = session.jobDescription,
                    resumeSnippet = session.resumeSnippet,
                    questionIndex = nextIndex,
                    totalQuestions = session.totalQuestions,
                    targetCompetency = nextCompetency,
                    difficulty = nextDifficulty,
                    previousContext = history
                )
                aiResult.getOrElse {
                    AdaptiveInterviewEngine.selectAdaptiveQuestion(
                        roleTitle = session.roleTitle,
                        questionIndex = nextIndex,
                        totalQuestions = session.totalQuestions,
                        previousScores = previousScores,
                        resumeText = session.resumeSnippet,
                        testedCompetencies = testedCompetencies
                    )
                }
            } else {
                AdaptiveInterviewEngine.selectAdaptiveQuestion(
                    roleTitle = session.roleTitle,
                    questionIndex = nextIndex,
                    totalQuestions = session.totalQuestions,
                    previousScores = previousScores,
                    resumeText = session.resumeSnippet,
                    testedCompetencies = testedCompetencies
                )
            }

            val existingNext = interviewDao.getQuestionsForSession(sessionId).firstOrNull { it.orderIndex == nextIndex }
            if (existingNext == null) {
                val nextQuestionEntity = InterviewQuestionEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    orderIndex = nextIndex,
                    questionText = nextQTemplate.questionText,
                    targetCompetency = nextQTemplate.targetCompetency,
                    difficulty = nextQTemplate.difficulty,
                    adaptiveReason = nextReason,
                    candidateAnswer = "",
                    answerDurationSeconds = 0,
                    isEvaluated = false
                )
                interviewDao.insertQuestion(nextQuestionEntity)
            }

            // Update session current index
            interviewDao.updateSession(session.copy(currentQuestionIndex = nextIndex - 1))
        } else {
            // Finalize Interview Session and generate structured Assessment Report
            val finalScores = allEvaluatedQuestions.map { it.score }
            val avgScore = if (finalScores.isNotEmpty()) finalScores.average().toInt() else evaluation.score

            val strengths = allEvaluatedQuestions
                .filter { it.score >= 80 }
                .joinToString("; ") { "Demonstrated high competence in ${it.targetCompetency} (${it.score}%)" }
                .ifEmpty { "Demonstrated foundational competence across evaluated modules." }

            val gaps = allEvaluatedQuestions
                .filter { it.score < 75 }
                .joinToString("; ") { "Identified knowledge gap in ${it.targetCompetency}: ${it.missingDetails.take(90)}" }
                .ifEmpty { "No severe technical gaps detected during this evaluation session." }

            val completedSession = session.copy(
                currentQuestionIndex = session.totalQuestions,
                isCompleted = true,
                overallScore = avgScore,
                summaryStrengths = strengths,
                summaryGaps = gaps
            )
            interviewDao.updateSession(completedSession)

            // Notify report generated
            notificationService.notifyReportGenerated(sessionId, session.roleTitle, avgScore)
        }

        evaluation
    }

    /**
     * Updates recruiter notes, verdict, and review status for a session.
     */
    suspend fun updateRecruiterReview(
        sessionId: String,
        notes: String,
        verdict: String
    ) = withContext(Dispatchers.IO) {
        val session = interviewDao.getSessionById(sessionId) ?: return@withContext
        val updated = session.copy(
            recruiterNotes = notes,
            recruiterVerdict = verdict
        )
        interviewDao.updateSession(updated)

        notificationService.notifyRecruiterNotesAdded(sessionId, session.roleTitle, verdict)
    }
}
