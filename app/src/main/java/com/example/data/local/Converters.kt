package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.IntegritySeverity
import com.example.data.InterviewMode
import com.example.data.QuestionDifficulty
import com.example.data.SkillStatus
import com.example.data.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.CANDIDATE)

    @TypeConverter
    fun fromSkillStatus(value: SkillStatus): String = value.name

    @TypeConverter
    fun toSkillStatus(value: String): SkillStatus = runCatching { SkillStatus.valueOf(value) }.getOrDefault(SkillStatus.NOT_ASSESSED)

    @TypeConverter
    fun fromIntegritySeverity(value: IntegritySeverity): String = value.name

    @TypeConverter
    fun toIntegritySeverity(value: String): IntegritySeverity = runCatching { IntegritySeverity.valueOf(value) }.getOrDefault(IntegritySeverity.LOW)

    @TypeConverter
    fun fromQuestionDifficulty(value: QuestionDifficulty): String = value.name

    @TypeConverter
    fun toQuestionDifficulty(value: String): QuestionDifficulty = runCatching { QuestionDifficulty.valueOf(value) }.getOrDefault(QuestionDifficulty.INTERMEDIATE)

    @TypeConverter
    fun fromInterviewMode(value: InterviewMode): String = value.name

    @TypeConverter
    fun toInterviewMode(value: String): InterviewMode = runCatching { InterviewMode.valueOf(value) }.getOrDefault(InterviewMode.PRACTICE)
}
