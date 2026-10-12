package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.IntegritySeverity
import com.example.data.InterviewMode
import com.example.data.QuestionDifficulty
import com.example.data.SkillStatus
import com.example.data.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val role: UserRole,
    val name: String,
    val email: String,
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val targetRole: String = "",
    val experienceYears: Int = 3,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "auth_sessions")
data class AuthSessionEntity(
    @PrimaryKey val id: String = "active_session",
    val userId: String,
    val userEmail: String,
    val userName: String,
    val userRole: UserRole,
    val loggedInAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "interview_sessions")
data class InterviewSessionEntity(
    @PrimaryKey val id: String,
    val candidateId: String,
    val candidateName: String,
    val roleTitle: String,
    val roleDepartment: String,
    val jobDescription: String,
    val resumeSnippet: String,
    val mode: InterviewMode,
    val totalQuestions: Int = 5,
    val currentQuestionIndex: Int = 0,
    val isCompleted: Boolean = false,
    val overallScore: Int = 0,
    val summaryStrengths: String = "",
    val summaryGaps: String = "",
    val recruiterNotes: String = "",
    val recruiterVerdict: String = "PENDING_REVIEW", // PENDING_REVIEW, ADVANCE_TO_ONSITE, FURTHER_EVALUATION, ARCHIVED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "interview_questions")
data class InterviewQuestionEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val orderIndex: Int,
    val questionText: String,
    val targetCompetency: String,
    val difficulty: QuestionDifficulty,
    val adaptiveReason: String,
    val candidateAnswer: String = "",
    val answerDurationSeconds: Int = 0,
    val score: Int = 0,
    val correctnessDetails: String = "",
    val missingDetails: String = "",
    val evidenceExtracted: String = "",
    val uncertaintyLevel: String = "Low",
    val uncertaintyReason: String = "",
    val isEvaluated: Boolean = false
)

@Entity(tableName = "skill_assessments")
data class SkillAssessmentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val skillName: String,
    val domainCategory: String, // e.g. "Architecture", "Engineering", "Soft Skills"
    val status: SkillStatus,
    val evidenceSnippet: String = "",
    val proficiencyPercent: Int = 0
)

@Entity(tableName = "integrity_events")
data class IntegrityEventEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val eventType: String, // "APP_BACKGROUNDED", "WINDOW_FOCUS_LOST", "RAPID_PASTE_DETECTED", "BURST_TYPING_ANOMALY"
    val timestamp: Long = System.currentTimeMillis(),
    val severity: IntegritySeverity,
    val confidence: Int, // e.g. 92%
    val description: String,
    val isConfirmedViolation: Boolean = false, // Distinguish detected event vs confirmed violation per PDF
    val recruiterReviewNotes: String = ""
)

@Entity(tableName = "notifications")
data class NotificationItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val notificationType: String, // "INTERVIEW_READY", "INTEGRITY_ALERT", "EVALUATION_DONE", "REPORT_GENERATED", "RECRUITER_FEEDBACK"
    val targetSessionId: String? = null
)
