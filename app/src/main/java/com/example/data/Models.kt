package com.example.data

enum class UserRole {
    CANDIDATE,
    RECRUITER
}

enum class SkillStatus {
    DEMONSTRATED,
    CLAIMED_UNVALIDATED,
    NOT_DEMONSTRATED,
    NOT_ASSESSED
}

enum class IntegritySeverity {
    LOW,
    MEDIUM,
    HIGH
}

enum class QuestionDifficulty {
    FOUNDATIONAL,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

enum class InterviewMode {
    PRACTICE, // Instant per-question feedback
    FORMAL   // Feedback deferred to final assessment report, full integrity tracking
}

data class JobRole(
    val id: String,
    val title: String,
    val department: String,
    val description: String,
    val keyCompetencies: List<String>
)

data class ExtractedSkill(
    val name: String,
    val category: String, // e.g. "Core Engineering", "System Design", "Problem Solving"
    val isClaimedOnResume: Boolean,
    val isRequiredByRole: Boolean
)

data class EvaluatedAnswer(
    val score: Int, // 0 - 100
    val correctness: String,
    val missingPoints: String,
    val supportingEvidence: String,
    val uncertaintyLevel: String, // "Low", "Medium", "High"
    val uncertaintyReason: String,
    val skillUpdated: String,
    val skillStatusResult: SkillStatus
)
