package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.InterviewMode
import com.example.data.SkillStatus
import com.example.data.UserRole
import com.example.data.repository.SkillSenseRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InterviewWorkflowPersistenceTest {

    @Test
    fun `verify end to end interview persistence workflow`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SkillSenseRepository(context)
        val authRepo = repository.authRepository

        // Operation 1: Candidate Registration and Login Persistence
        val regResult = authRepo.register(
            name = "Maria Santos",
            email = "maria.santos@example.com",
            password = "Password123!",
            confirmPassword = "Password123!",
            role = UserRole.CANDIDATE
        )
        assertTrue("Registration must succeed", regResult.isSuccess)
        val candidate = regResult.getOrThrow()
        assertEquals("Maria Santos", candidate.name)

        val activeAuth = authRepo.getActiveSession()
        assertNotNull("Active session must be saved in Room", activeAuth)
        assertEquals(candidate.id, activeAuth?.userId)

        // Operation 2 & 3: Resume setup & Interview Session Creation
        val roleTitle = "Senior Android Engineer"
        val department = "Mobile Architecture"
        val jobDesc = "Architect Jetpack Compose reactive UIs, manage Kotlin Coroutines concurrency, and maintain Room database offline sync."
        val resumeText = "Senior Android Developer with 5 years experience in Kotlin, Jetpack Compose, Coroutines, StateFlow, Room, and Clean Architecture."

        val sessionId = repository.createInterviewSession(
            candidateId = candidate.id,
            candidateName = candidate.name,
            roleTitle = roleTitle,
            department = department,
            jobDescription = jobDesc,
            resumeSnippet = resumeText,
            mode = InterviewMode.FORMAL,
            questionCount = 3
        )
        assertTrue("Session ID must be generated", sessionId.isNotEmpty())

        val createdSession = repository.getSession(sessionId)
        assertNotNull("Created session must exist in Room", createdSession)
        assertEquals(candidate.id, createdSession?.candidateId)
        assertEquals(false, createdSession?.isCompleted)

        // Operation 4: Initial Question Generation & Skill Seeding
        val initialQuestions = repository.getQuestions(sessionId)
        assertEquals("Initial question must be seeded", 1, initialQuestions.size)
        val q1 = initialQuestions.first()
        assertEquals(1, q1.orderIndex)
        assertEquals(false, q1.isEvaluated)

        val initialSkills = repository.getSkills(sessionId)
        assertTrue("Initial skills must be seeded in Room", initialSkills.isNotEmpty())

        // Operation 5 & 6: Answer Submission, Evidence Extraction, and Skill Evidence Persistence
        val q1Answer = "I hoist state to the ViewModel using MutableStateFlow and collectAsStateWithLifecycle. Composables accept immutable state parameters and pass user event callbacks up to ensure unidirectional data flow."
        val eval1 = repository.submitAnswerAndEvaluate(
            sessionId = sessionId,
            questionId = q1.id,
            answer = q1Answer,
            durationSeconds = 45
        )
        assertTrue("Answer score should reflect technical accuracy", eval1.score >= 70)
        assertTrue("Supporting evidence must be extracted", eval1.supportingEvidence.isNotEmpty())

        // Verify Question 1 is saved with answer and evidence in Room
        val savedQuestionsQ1 = repository.getQuestions(sessionId)
        val updatedQ1 = savedQuestionsQ1.first { it.id == q1.id }
        assertTrue("Q1 must be marked as evaluated in Room", updatedQ1.isEvaluated)
        assertEquals(q1Answer, updatedQ1.candidateAnswer)
        assertTrue("Q1 evidence must be saved in Room", updatedQ1.evidenceExtracted.isNotEmpty())

        // Verify Question 2 was adaptively generated and saved
        assertEquals("Second adaptive question must be added to Room", 2, savedQuestionsQ1.size)
        val q2 = savedQuestionsQ1.first { it.orderIndex == 2 }
        assertEquals(false, q2.isEvaluated)

        // Submit Question 2 Answer
        val q2Answer = "In Kotlin Coroutines, standard CoroutineScope propagates failures up cancelling siblings, whereas supervisorScope isolates child failures allowing independent error handling."
        val eval2 = repository.submitAnswerAndEvaluate(
            sessionId = sessionId,
            questionId = q2.id,
            answer = q2Answer,
            durationSeconds = 50
        )
        assertTrue("Q2 score should reflect coroutine answer", eval2.score >= 70)

        // Submit Question 3 Answer (Final Question)
        val savedQuestionsQ2 = repository.getQuestions(sessionId)
        val q3 = savedQuestionsQ2.first { it.orderIndex == 3 }
        val q3Answer = "Room serves as single source of truth. UI observes Room Flow. Network writes update Room with sync_status flag, and background WorkManager syncs with remote server."
        val eval3 = repository.submitAnswerAndEvaluate(
            sessionId = sessionId,
            questionId = q3.id,
            answer = q3Answer,
            durationSeconds = 60
        )
        assertTrue("Q3 evaluation completed", eval3.score > 0)

        // Operation 7: Interview Completion and Report Retrieval
        val finalSession = repository.getSession(sessionId)
        assertNotNull("Session must exist", finalSession)
        assertEquals("Session must be marked completed after final answer", true, finalSession?.isCompleted)
        assertTrue("Overall score must be computed", (finalSession?.overallScore ?: 0) > 0)
        assertTrue("Summary strengths must be populated", finalSession?.summaryStrengths?.isNotEmpty() == true)

        val finalQuestions = repository.getQuestions(sessionId)
        assertEquals("All 3 questions evaluated", 3, finalQuestions.size)
        assertTrue("All questions must be evaluated", finalQuestions.all { it.isEvaluated })

        val finalSkills = repository.getSkills(sessionId)
        assertTrue("Assessed skills must exist", finalSkills.isNotEmpty())

        // Operation 8: Closing and Reopening the App without losing data
        // Simulate app restart by re-instantiating repository with application context
        val newAppRepository = SkillSenseRepository(context)

        // Verify user session restoration
        val restoredAuthSession = newAppRepository.authRepository.getActiveSession()
        assertNotNull("Auth session must persist across app restart", restoredAuthSession)
        assertEquals(candidate.id, restoredAuthSession?.userId)

        // Verify interview session, questions, answers, and report persist completely
        val restoredSession = newAppRepository.getSession(sessionId)
        assertNotNull("Completed session must persist in Room", restoredSession)
        assertEquals(true, restoredSession?.isCompleted)
        assertEquals(finalSession?.overallScore, restoredSession?.overallScore)

        val restoredQuestions = newAppRepository.getQuestions(sessionId)
        assertEquals(3, restoredQuestions.size)
        assertTrue("Saved candidate answers must be restored", restoredQuestions.all { it.candidateAnswer.isNotEmpty() })

        val restoredSkills = newAppRepository.getSkills(sessionId)
        assertTrue("Saved skill assessments must be restored", restoredSkills.isNotEmpty())
    }
}
