package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.IntegritySeverity
import com.example.data.InterviewMode
import com.example.data.QuestionDifficulty
import com.example.data.SkillStatus
import com.example.data.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        AuthSessionEntity::class,
        InterviewSessionEntity::class,
        InterviewQuestionEntity::class,
        SkillAssessmentEntity::class,
        IntegrityEventEntity::class,
        NotificationItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SkillSenseDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun interviewDao(): InterviewDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: SkillSenseDatabase? = null

        fun getDatabase(context: Context): SkillSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkillSenseDatabase::class.java,
                    "skillsense_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }
        }

        private suspend fun seedInitialData(database: SkillSenseDatabase) {
            val userDao = database.userDao()
            val interviewDao = database.interviewDao()
            val notificationDao = database.notificationDao()

            // Seed Default Candidate (Password: Password123!)
            val candSalt = com.example.service.SecurityUtils.generateSalt()
            userDao.insertUser(
                UserEntity(
                    id = "candidate_default",
                    role = UserRole.CANDIDATE,
                    name = "Alex Rivera",
                    email = "alex.rivera@example.com",
                    passwordHash = com.example.service.SecurityUtils.hashPassword("Password123!", candSalt),
                    passwordSalt = candSalt,
                    targetRole = "Senior Android Engineer",
                    experienceYears = 4
                )
            )

            // Seed Default Recruiter (Password: Password123!)
            val recSalt = com.example.service.SecurityUtils.generateSalt()
            userDao.insertUser(
                UserEntity(
                    id = "recruiter_default",
                    role = UserRole.RECRUITER,
                    name = "Elena Rostova",
                    email = "elena.recruiter@techscale.io",
                    passwordHash = com.example.service.SecurityUtils.hashPassword("Password123!", recSalt),
                    passwordSalt = recSalt,
                    targetRole = "Lead Tech Recruiter",
                    experienceYears = 7
                )
            )

            // Seed a sample benchmark completed interview session to demonstrate final reports & recruiter review offline
            val sampleSessionId = "sample_session_101"
            interviewDao.insertSession(
                InterviewSessionEntity(
                    id = sampleSessionId,
                    candidateId = "candidate_default",
                    candidateName = "Alex Rivera",
                    roleTitle = "Senior Android Engineer",
                    roleDepartment = "Mobile Platform Core",
                    jobDescription = "Design modern reactive UI using Jetpack Compose, architect scalable coroutine flows, offline-first Room databases, and manage memory/performance across millions of devices.",
                    resumeSnippet = "Experienced Android Engineer with 4 years building high-traffic Kotlin apps. Proficient in Jetpack Compose, Coroutines, Flow, Room, Dagger/Hilt, CI/CD, and MVVM Clean Architecture.",
                    mode = InterviewMode.FORMAL,
                    totalQuestions = 4,
                    currentQuestionIndex = 4,
                    isCompleted = true,
                    overallScore = 86,
                    summaryStrengths = "Strong architecture command of Jetpack Compose unidirectional data flow and Kotlin Coroutines cancellation hierarchies. Provided precise code-level explanations.",
                    summaryGaps = "Lacked direct production evidence in low-level memory leak profiling with LeakCanary/Systrace when handling heavy background sync.",
                    recruiterNotes = "Candidate demonstrated exceptional mobile fundamentals. Ready for technical on-site with Mobile Staff Engineers.",
                    recruiterVerdict = "ADVANCE_TO_ONSITE",
                    createdAt = System.currentTimeMillis() - 86400000L // 1 day ago
                )
            )

            // Questions for sample session
            val q1 = InterviewQuestionEntity(
                id = "sample_q1",
                sessionId = sampleSessionId,
                orderIndex = 1,
                questionText = "How do you ensure state hoists correctly in Jetpack Compose to prevent unnecessary recompositions and maintain unidirectional data flow?",
                targetCompetency = "Jetpack Compose & State Management",
                difficulty = QuestionDifficulty.INTERMEDIATE,
                adaptiveReason = "Resume claims 4 years Compose expertise. Verifying foundational UI architecture baseline.",
                candidateAnswer = "I hoist state up to the ViewModel or screen-level composable using MutableStateFlow and collectAsStateWithLifecycle. Stateless sub-composables accept immutable value parameters and lambda callbacks, passing actions up.",
                answerDurationSeconds = 62,
                score = 92,
                correctnessDetails = "Accurately detailed stateless/stateful separation and lifecycle-aware state collection.",
                missingDetails = "Could have mentioned remember(key) and derivedStateOf for fine-grained calculation optimizations.",
                evidenceExtracted = "Explicitly cited collectAsStateWithLifecycle and lambda hoisting for unidirectional flow.",
                uncertaintyLevel = "Low",
                uncertaintyReason = "Clear conceptual and practical explanation provided.",
                isEvaluated = true
            )

            val q2 = InterviewQuestionEntity(
                id = "sample_q2",
                sessionId = sampleSessionId,
                orderIndex = 2,
                questionText = "Explain how structured concurrency in Kotlin Coroutines behaves when a child coroutine fails inside a CoroutineScope versus a supervisorScope.",
                targetCompetency = "Kotlin Coroutines & Concurrency",
                difficulty = QuestionDifficulty.ADVANCED,
                adaptiveReason = "Adapted to Advanced difficulty based on high score in previous architecture question.",
                candidateAnswer = "In standard CoroutineScope, an uncaught exception in any child cancels the parent job and all siblings. With supervisorScope, a child failure does not cancel its siblings or parent, allowing isolated error handling.",
                answerDurationSeconds = 78,
                score = 88,
                correctnessDetails = "Correctly contrasted standard hierarchical failure propagation with supervisorScope isolation.",
                missingDetails = "Did not mention CoroutineExceptionHandler placement nuances with async/await.",
                evidenceExtracted = "Demonstrated accurate understanding of cancellation propagation mechanisms.",
                uncertaintyLevel = "Low",
                uncertaintyReason = "Strong evidence of practical coroutine knowledge.",
                isEvaluated = true
            )

            val q3 = InterviewQuestionEntity(
                id = "sample_q3",
                sessionId = sampleSessionId,
                orderIndex = 3,
                questionText = "How do you design an offline-first data synchronization strategy using Room and remote APIs, ensuring conflict resolution and optimistic UI updates?",
                targetCompetency = "Offline Persistence & Room DB",
                difficulty = QuestionDifficulty.ADVANCED,
                adaptiveReason = "Job description requires offline-first architecture; validating resume claim of Room persistence.",
                candidateAnswer = "Room serves as single source of truth. UI observes Room Flow. When network writes occur, local DB is updated optimistically with a sync_status flag. Background WorkManager syncs with backend using timestamp or version vector conflict resolution.",
                answerDurationSeconds = 95,
                score = 90,
                correctnessDetails = "Comprehensive offline-first architecture proposal combining Room as single source of truth and WorkManager.",
                missingDetails = "Briefly skipped pagination details with Room Paging 3.",
                evidenceExtracted = "Proposed single source of truth pattern with Room Flow and optimistic status flags.",
                uncertaintyLevel = "Low",
                uncertaintyReason = "Well structured, high practical relevance.",
                isEvaluated = true
            )

            val q4 = InterviewQuestionEntity(
                id = "sample_q4",
                sessionId = sampleSessionId,
                orderIndex = 4,
                questionText = "What profiling tools and techniques do you use to diagnose memory leaks and jank during complex nested LazyColumn scrolling?",
                targetCompetency = "Android Performance & Memory Profiling",
                difficulty = QuestionDifficulty.EXPERT,
                adaptiveReason = "Deep probe into expert performance tuning to test upper competency limit.",
                candidateAnswer = "I check Compose Layout Inspector for recomposition counts and use Android Studio CPU profiler. For memory leaks I look at heap dumps.",
                answerDurationSeconds = 41,
                score = 74,
                correctnessDetails = "Identified Layout Inspector and CPU profiler.",
                missingDetails = "Lacked depth on Systrace / Perfetto trace markers, Baseline Profiles, and LeakCanary retain reference analysis.",
                evidenceExtracted = "General familiarity with tooling, but lacked granular optimization case studies.",
                uncertaintyLevel = "Medium",
                uncertaintyReason = "Answer was somewhat brief; evidence of production profiling was limited.",
                isEvaluated = true
            )

            interviewDao.insertQuestions(listOf(q1, q2, q3, q4))

            // Sample Skills
            interviewDao.insertSkills(
                listOf(
                    SkillAssessmentEntity(
                        id = "skill_1",
                        sessionId = sampleSessionId,
                        skillName = "Jetpack Compose & State Hoisting",
                        domainCategory = "UI Architecture",
                        status = SkillStatus.DEMONSTRATED,
                        evidenceSnippet = "Demonstrated deep mastery of unidirectional data flow and collectAsStateWithLifecycle.",
                        proficiencyPercent = 92
                    ),
                    SkillAssessmentEntity(
                        id = "skill_2",
                        sessionId = sampleSessionId,
                        skillName = "Kotlin Coroutines & Concurrency",
                        domainCategory = "Core Engineering",
                        status = SkillStatus.DEMONSTRATED,
                        evidenceSnippet = "Clear differentiation of CoroutineScope vs supervisorScope cancellation semantics.",
                        proficiencyPercent = 88
                    ),
                    SkillAssessmentEntity(
                        id = "skill_3",
                        sessionId = sampleSessionId,
                        skillName = "Offline Persistence & Room DB",
                        domainCategory = "Data Architecture",
                        status = SkillStatus.DEMONSTRATED,
                        evidenceSnippet = "Single source of truth with Room Flow and WorkManager background sync.",
                        proficiencyPercent = 90
                    ),
                    SkillAssessmentEntity(
                        id = "skill_4",
                        sessionId = sampleSessionId,
                        skillName = "Deep Memory Profiling & Systrace",
                        domainCategory = "Performance",
                        status = SkillStatus.CLAIMED_UNVALIDATED,
                        evidenceSnippet = "Resume claimed advanced profiling, but candidate response was brief without Perfetto or LeakCanary examples.",
                        proficiencyPercent = 58
                    ),
                    SkillAssessmentEntity(
                        id = "skill_5",
                        sessionId = sampleSessionId,
                        skillName = "Multi-module Gradle Optimization",
                        domainCategory = "Build Systems",
                        status = SkillStatus.NOT_ASSESSED,
                        evidenceSnippet = "Listed on job role requirements but not tested in this 4-question session.",
                        proficiencyPercent = 0
                    )
                )
            )

            // Sample Integrity Event (to showcase Section 5.3 & Recruiter audit timeline)
            interviewDao.insertIntegrityEvent(
                IntegrityEventEntity(
                    id = "event_sample_1",
                    sessionId = sampleSessionId,
                    eventType = "WINDOW_FOCUS_LOST",
                    timestamp = System.currentTimeMillis() - 86350000L,
                    severity = IntegritySeverity.MEDIUM,
                    confidence = 94,
                    description = "Application temporarily lost window focus for 3.2 seconds during Question 3 answer drafting.",
                    isConfirmedViolation = false,
                    recruiterReviewNotes = "Recruiter noted: Candidate likely received an incoming system notification. Verified benign."
                )
            )

            // Sample Notifications
            notificationDao.insertNotification(
                NotificationItemEntity(
                    id = "notif_welcome",
                    title = "ForkAI Activated",
                    message = "Offline-first database initialized. Ready for adaptive interview sessions with real-time feedback.",
                    timestamp = System.currentTimeMillis() - 90000000L,
                    isRead = true,
                    notificationType = "INTERVIEW_READY"
                )
            )

            notificationDao.insertNotification(
                NotificationItemEntity(
                    id = "notif_report",
                    title = "Assessment Report Available",
                    message = "Senior Android Engineer interview report for Alex Rivera is finalized with 4 competency evaluations.",
                    timestamp = System.currentTimeMillis() - 86300000L,
                    isRead = false,
                    notificationType = "REPORT_GENERATED",
                    targetSessionId = sampleSessionId
                )
            )
        }
    }
}
