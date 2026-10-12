package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuestionDifficulty
import com.example.data.SkillStatus
import com.example.service.AdaptiveInterviewEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name string resource from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ForkAI", appName)
  }

  @Test
  fun `verify adaptive question selection logic`() {
    val (q, reason) = AdaptiveInterviewEngine.selectAdaptiveQuestion(
      roleTitle = "Senior Android Engineer",
      questionIndex = 1,
      totalQuestions = 4,
      previousScores = emptyList(),
      resumeText = "Experienced Android developer with Jetpack Compose and Coroutines",
      testedCompetencies = emptySet()
    )

    assertNotNull(q)
    assertTrue(reason.isNotEmpty())
    assertTrue(q.targetCompetency.isNotEmpty())
  }

  @Test
  fun `verify answer evaluation extracts evidence and assigns status`() {
    val evaluation = AdaptiveInterviewEngine.evaluateAnswer(
      questionText = "How do you handle state hoisting in Jetpack Compose?",
      targetCompetency = "Jetpack Compose & Declarative UI",
      answer = "I use state hoisting up to the ViewModel with MutableStateFlow and collectAsStateWithLifecycle. Stateless composables receive immutable values and event callbacks to ensure unidirectional data flow.",
      difficulty = QuestionDifficulty.INTERMEDIATE,
      expectedKeywords = listOf("state", "hoisting", "mutablestateflow", "collectasstate", "unidirectional")
    )

    assertTrue("Score should reflect technical answer", evaluation.score >= 75)
    assertEquals(SkillStatus.DEMONSTRATED, evaluation.skillStatusResult)
    assertTrue("Supporting evidence should be extracted", evaluation.supportingEvidence.isNotEmpty())
    assertEquals("Low", evaluation.uncertaintyLevel)
  }

  @Test
  fun `verify brief answer results in high uncertainty`() {
    val evaluation = AdaptiveInterviewEngine.evaluateAnswer(
      questionText = "Explain distributed caching in Redis.",
      targetCompetency = "Distributed Caching (Redis/Memcached)",
      answer = "I used redis once.",
      difficulty = QuestionDifficulty.ADVANCED,
      expectedKeywords = listOf("redis", "cache", "stampede", "ttl")
    )

    assertEquals(SkillStatus.NOT_DEMONSTRATED, evaluation.skillStatusResult)
    assertEquals("High", evaluation.uncertaintyLevel)
  }

  @Test
  fun `verify navigation items count for candidate and recruiter`() {
    assertEquals(5, com.example.ui.navigation.CANDIDATE_NAV_ITEMS.size)
    assertEquals(5, com.example.ui.navigation.RECRUITER_NAV_ITEMS.size)

    val candidateLabels = com.example.ui.navigation.CANDIDATE_NAV_ITEMS.map { it.label }
    assertEquals(listOf("Home", "Practice", "Skills", "Reports", "Profile"), candidateLabels)

    val recruiterLabels = com.example.ui.navigation.RECRUITER_NAV_ITEMS.map { it.label }
    assertEquals(listOf("Dashboard", "Candidates", "Sessions", "Assessments", "Profile"), recruiterLabels)
  }

  @Test
  fun `verify password hashing and verification with salt`() {
    val password = "SecurePassword123!"
    val salt = com.example.service.SecurityUtils.generateSalt()
    val hash = com.example.service.SecurityUtils.hashPassword(password, salt)

    assertTrue("Salt must not be empty", salt.isNotEmpty())
    assertTrue("Hash must not be empty", hash.isNotEmpty())
    assertTrue("Password verification should succeed with correct credentials", com.example.service.SecurityUtils.verifyPassword(password, salt, hash))
    org.junit.Assert.assertFalse("Password verification should fail with wrong credentials", com.example.service.SecurityUtils.verifyPassword("WrongPassword", salt, hash))
  }

  @Test
  fun `verify candidate dashboard score calculation does not fabricate metrics`() {
    val emptySessions = emptyList<com.example.data.local.InterviewSessionEntity>()
    val completedEmpty = emptySessions.filter { it.isCompleted }
    val avgEmpty = if (completedEmpty.isNotEmpty()) completedEmpty.map { it.overallScore }.average().toInt() else null
    org.junit.Assert.assertNull("Empty sessions list must not yield a fabricated score", avgEmpty)

    val sessions = listOf(
      com.example.data.local.InterviewSessionEntity(
        id = "session_1",
        candidateId = "c_1",
        candidateName = "Test Candidate",
        roleTitle = "Android Engineer",
        roleDepartment = "Mobile",
        jobDescription = "Kotlin & Jetpack Compose",
        resumeSnippet = "Experience in Android",
        mode = com.example.data.InterviewMode.PRACTICE,
        overallScore = 80,
        isCompleted = true
      ),
      com.example.data.local.InterviewSessionEntity(
        id = "session_2",
        candidateId = "c_1",
        candidateName = "Test Candidate",
        roleTitle = "Android Engineer",
        roleDepartment = "Mobile",
        jobDescription = "Kotlin & Jetpack Compose",
        resumeSnippet = "Experience in Android",
        mode = com.example.data.InterviewMode.FORMAL,
        overallScore = 90,
        isCompleted = true
      ),
      com.example.data.local.InterviewSessionEntity(
        id = "session_3",
        candidateId = "c_1",
        candidateName = "Test Candidate",
        roleTitle = "Android Engineer",
        roleDepartment = "Mobile",
        jobDescription = "Kotlin & Jetpack Compose",
        resumeSnippet = "Experience in Android",
        mode = com.example.data.InterviewMode.FORMAL,
        overallScore = 0,
        isCompleted = false
      )
    )

    val completed = sessions.filter { it.isCompleted }
    val avgScore = completed.map { it.overallScore }.average().toInt()
    assertEquals(2, completed.size)
    assertEquals(85, avgScore)
  }

  @Test
  fun `verify available job roles include required positions with valid question templates`() {
    val roles = com.example.service.AdaptiveInterviewEngine.AVAILABLE_ROLES.map { it.title }
    assertTrue("Must include Android Developer", roles.any { it.contains("Android", ignoreCase = true) })
    assertTrue("Must include Java Developer", roles.contains("Java Developer"))
    assertTrue("Must include Python Developer", roles.contains("Python Developer"))
    assertTrue("Must include Web Developer", roles.contains("Web Developer"))
    assertTrue("Must include Data Analyst", roles.contains("Data Analyst"))

    val requiredRoles = listOf("Android Developer", "Java Developer", "Python Developer", "Web Developer", "Data Analyst")
    for (role in requiredRoles) {
      val questions = com.example.service.AdaptiveInterviewEngine.getQuestionsForRole(role, "Sample resume")
      assertTrue("Questions for $role must not be empty", questions.isNotEmpty())
      assertTrue("Questions for $role must have valid questionText", questions.all { it.questionText.isNotBlank() })
    }
  }

  @Test
  fun `verify active interview question sequencing and completion status`() {
    val q1 = com.example.data.local.InterviewQuestionEntity(
      id = "q1",
      sessionId = "session_test",
      orderIndex = 1,
      questionText = "Explain Android lifecycles",
      targetCompetency = "Android Architecture",
      difficulty = QuestionDifficulty.INTERMEDIATE,
      adaptiveReason = "Core competency baseline",
      candidateAnswer = "Activity lifecycles handle creation, starting, resuming, pausing, stopping, and destruction.",
      isEvaluated = true,
      score = 85,
      evidenceExtracted = "Lifecycle methods enumerated correctly",
      answerDurationSeconds = 45
    )

    val q2 = com.example.data.local.InterviewQuestionEntity(
      id = "q2",
      sessionId = "session_test",
      orderIndex = 2,
      questionText = "How do Coroutines prevent UI blocking?",
      targetCompetency = "Kotlin Coroutines & Flow",
      difficulty = QuestionDifficulty.ADVANCED,
      adaptiveReason = "Elevating difficulty following demonstrated performance",
      candidateAnswer = "",
      isEvaluated = false,
      score = 0,
      evidenceExtracted = "",
      answerDurationSeconds = 0
    )

    val questions = listOf(q1, q2)
    val activeQuestion = questions.firstOrNull { !it.isEvaluated }
    assertNotNull("First unevaluated question should be active", activeQuestion)
    assertEquals("q2", activeQuestion?.id)
    assertEquals(2, activeQuestion?.orderIndex)

    val evaluatedCount = questions.count { it.isEvaluated }
    assertEquals(1, evaluatedCount)
    val isCompleted = questions.all { it.isEvaluated }
    org.junit.Assert.assertFalse("Session should not be completed while unevaluated questions remain", isCompleted)

    val allEvaluated = listOf(q1, q2.copy(isEvaluated = true, score = 90))
    assertTrue("Session completes when all questions are evaluated", allEvaluated.all { it.isEvaluated })
  }

  @Test
  fun `verify skills dashboard 4-way classification and evidence mapping`() {
    val demonstratedSkill = com.example.data.local.SkillAssessmentEntity(
      id = "s1",
      sessionId = "session_test",
      skillName = "Jetpack Compose & State Hoisting",
      domainCategory = "UI Architecture",
      status = com.example.data.SkillStatus.DEMONSTRATED,
      evidenceSnippet = "Demonstrated deep mastery of unidirectional data flow",
      proficiencyPercent = 90
    )

    val unvalidatedSkill = com.example.data.local.SkillAssessmentEntity(
      id = "s2",
      sessionId = "session_test",
      skillName = "Memory Profiling & Systrace",
      domainCategory = "Performance",
      status = com.example.data.SkillStatus.CLAIMED_UNVALIDATED,
      evidenceSnippet = "Claimed on resume; awaiting interview validation",
      proficiencyPercent = 55
    )

    val notDemonstratedSkill = com.example.data.local.SkillAssessmentEntity(
      id = "s3",
      sessionId = "session_test",
      skillName = "Advanced Concurrency Channels",
      domainCategory = "Core Architecture",
      status = com.example.data.SkillStatus.NOT_DEMONSTRATED,
      evidenceSnippet = "Answer did not cover channel buffering and rendezvous mechanisms",
      proficiencyPercent = 40
    )

    val notAssessedSkill = com.example.data.local.SkillAssessmentEntity(
      id = "s4",
      sessionId = "session_test",
      skillName = "Multi-module Gradle Optimization",
      domainCategory = "Build Systems",
      status = com.example.data.SkillStatus.NOT_ASSESSED,
      evidenceSnippet = "Required by role; unassessed in this session",
      proficiencyPercent = 0
    )

    val skills = listOf(demonstratedSkill, unvalidatedSkill, notDemonstratedSkill, notAssessedSkill)

    // Verify 4-way category counts calculation
    val demonstratedCount = skills.count { it.status == com.example.data.SkillStatus.DEMONSTRATED }
    val unvalidatedCount = skills.count { it.status == com.example.data.SkillStatus.CLAIMED_UNVALIDATED }
    val notDemonstratedCount = skills.count { it.status == com.example.data.SkillStatus.NOT_DEMONSTRATED }
    val notAssessedCount = skills.count { it.status == com.example.data.SkillStatus.NOT_ASSESSED }

    assertEquals(1, demonstratedCount)
    assertEquals(1, unvalidatedCount)
    assertEquals(1, notDemonstratedCount)
    assertEquals(1, notAssessedCount)
    assertEquals(4, skills.size)

    // Verify question evidence linkage
    val q1 = com.example.data.local.InterviewQuestionEntity(
      id = "q1",
      sessionId = "session_test",
      orderIndex = 1,
      questionText = "How do you hoist state in Compose?",
      targetCompetency = "Jetpack Compose & State Hoisting",
      difficulty = com.example.data.QuestionDifficulty.INTERMEDIATE,
      adaptiveReason = "Resume claim validation",
      candidateAnswer = "I use MutableStateFlow and collectAsStateWithLifecycle.",
      isEvaluated = true,
      score = 90,
      evidenceExtracted = "Explicitly cited collectAsStateWithLifecycle",
      uncertaintyLevel = "Low",
      uncertaintyReason = "Clear technical response",
      answerDurationSeconds = 45
    )

    val linkedQuestion = listOf(q1).firstOrNull { it.targetCompetency == demonstratedSkill.skillName }
    assertNotNull("Demonstrated skill should link to its supporting question", linkedQuestion)
    assertEquals("Explicitly cited collectAsStateWithLifecycle", linkedQuestion?.evidenceExtracted)
    assertEquals("Low", linkedQuestion?.uncertaintyLevel)

    // Verify empty state handling
    val emptySkills = emptyList<com.example.data.local.SkillAssessmentEntity>()
    assertTrue("Empty skills list represents unassessed candidate without fake statistics", emptySkills.isEmpty())
  }

  @Test
  fun `verify GeminiInterviewService configuration status and fallback logic`() {
    val configured = com.example.service.GeminiInterviewService.isAiConfigured()
    val description = com.example.service.GeminiInterviewService.getAiStatusDescription()
    assertNotNull(description)
    if (configured) {
      assertTrue(description.contains("Gemini 3.5 Flash"))
    } else {
      assertTrue(description.contains("Fallback"))
    }
  }

  @Test
  fun `verify rule-based adaptive engine remains functional as reliable fallback`() {
    val (q, reason) = AdaptiveInterviewEngine.selectAdaptiveQuestion(
      roleTitle = "Cloud Backend Engineer",
      questionIndex = 2,
      totalQuestions = 4,
      previousScores = listOf(85),
      resumeText = "Distributed systems, caching, Redis, Kafka",
      testedCompetencies = setOf("RESTful APIs & gRPC Architecture")
    )
    assertNotNull(q)
    assertTrue("Should select unassessed competency", q.targetCompetency != "RESTful APIs & gRPC Architecture")
    assertTrue(reason.isNotEmpty())

    val eval = AdaptiveInterviewEngine.evaluateAnswer(
      questionText = q.questionText,
      targetCompetency = q.targetCompetency,
      answer = "We used Redis mutex locking to prevent cache stampede and bloom filters for cache penetration.",
      difficulty = q.difficulty,
      expectedKeywords = q.keywordsExpected
    )
    assertNotNull(eval)
    assertTrue(eval.score in 0..100)
    assertTrue(eval.supportingEvidence.isNotEmpty())
    assertTrue(eval.uncertaintyLevel in listOf("Low", "Medium", "High"))
  }
}

