package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Active session persistence
    @Query("SELECT * FROM auth_sessions WHERE id = 'active_session' LIMIT 1")
    fun getActiveSessionFlow(): Flow<AuthSessionEntity?>

    @Query("SELECT * FROM auth_sessions WHERE id = 'active_session' LIMIT 1")
    suspend fun getActiveSession(): AuthSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveSession(session: AuthSessionEntity)

    @Query("DELETE FROM auth_sessions")
    suspend fun clearActiveSession()
}

@Dao
interface InterviewDao {
    @Query("SELECT * FROM interview_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<InterviewSessionEntity>>

    @Query("SELECT * FROM interview_sessions WHERE candidateId = :candidateId ORDER BY createdAt DESC")
    fun getSessionsForCandidate(candidateId: String): Flow<List<InterviewSessionEntity>>

    @Query("SELECT * FROM interview_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): InterviewSessionEntity?

    @Query("SELECT * FROM interview_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionByIdFlow(sessionId: String): Flow<InterviewSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InterviewSessionEntity)

    @Update
    suspend fun updateSession(session: InterviewSessionEntity)

    // Questions
    @Query("SELECT * FROM interview_questions WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getQuestionsForSessionFlow(sessionId: String): Flow<List<InterviewQuestionEntity>>

    @Query("SELECT * FROM interview_questions WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getQuestionsForSession(sessionId: String): List<InterviewQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<InterviewQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: InterviewQuestionEntity)

    @Update
    suspend fun updateQuestion(question: InterviewQuestionEntity)

    // Skill Assessments
    @Query("SELECT * FROM skill_assessments WHERE sessionId = :sessionId")
    fun getSkillsForSessionFlow(sessionId: String): Flow<List<SkillAssessmentEntity>>

    @Query("SELECT * FROM skill_assessments WHERE sessionId = :sessionId")
    suspend fun getSkillsForSession(sessionId: String): List<SkillAssessmentEntity>

    @Query("SELECT * FROM skill_assessments")
    fun getAllSkillsFlow(): Flow<List<SkillAssessmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkills(skills: List<SkillAssessmentEntity>)

    @Update
    suspend fun updateSkill(skill: SkillAssessmentEntity)

    // Integrity Events
    @Query("SELECT * FROM integrity_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getIntegrityEventsForSessionFlow(sessionId: String): Flow<List<IntegrityEventEntity>>

    @Query("SELECT * FROM integrity_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getIntegrityEventsForSession(sessionId: String): List<IntegrityEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegrityEvent(event: IntegrityEventEntity)

    @Update
    suspend fun updateIntegrityEvent(event: IntegrityEventEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationItemEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItemEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}
