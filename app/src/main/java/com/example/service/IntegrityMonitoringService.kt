package com.example.service

import com.example.data.IntegritySeverity
import com.example.data.local.IntegrityEventEntity
import com.example.data.local.SkillSenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class IntegrityMonitoringService(
    private val database: SkillSenseDatabase,
    private val notificationService: NotificationService
) {
    var isMonitoringEnabled: Boolean = true

    fun recordFocusLost(sessionId: String) {
        if (!isMonitoringEnabled) return

        val event = IntegrityEventEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            eventType = "WINDOW_FOCUS_LOST",
            timestamp = System.currentTimeMillis(),
            severity = IntegritySeverity.MEDIUM,
            confidence = 92,
            description = "Candidate application lost window focus or was placed into background during active questioning.",
            isConfirmedViolation = false,
            recruiterReviewNotes = ""
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.interviewDao().insertIntegrityEvent(event)
            notificationService.notifyIntegrityAlert(
                sessionId = sessionId,
                eventType = "App Focus Shift Detected",
                description = "Focus loss event logged to audit timeline for recruiter review."
            )
        }
    }

    fun recordRapidPasteAnomaly(sessionId: String, charCount: Int) {
        if (!isMonitoringEnabled) return

        val event = IntegrityEventEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            eventType = "RAPID_PASTE_DETECTED",
            timestamp = System.currentTimeMillis(),
            severity = if (charCount > 400) IntegritySeverity.HIGH else IntegritySeverity.LOW,
            confidence = 88,
            description = "Rapid text insertion of $charCount characters detected in a single keystroke event.",
            isConfirmedViolation = false,
            recruiterReviewNotes = ""
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.interviewDao().insertIntegrityEvent(event)
            notificationService.notifyIntegrityAlert(
                sessionId = sessionId,
                eventType = "Rapid Input Insertion",
                description = "Paste event logged to audit timeline for recruiter review."
            )
        }
    }

    fun recordTypingBurstAnomaly(sessionId: String, durationSeconds: Int, wordCount: Int) {
        if (!isMonitoringEnabled) return
        if (durationSeconds < 3 && wordCount > 30) {
            val event = IntegrityEventEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                eventType = "BURST_TYPING_ANOMALY",
                timestamp = System.currentTimeMillis(),
                severity = IntegritySeverity.MEDIUM,
                confidence = 80,
                description = "High submission velocity: $wordCount words submitted in under $durationSeconds seconds.",
                isConfirmedViolation = false,
                recruiterReviewNotes = ""
            )

            CoroutineScope(Dispatchers.IO).launch {
                database.interviewDao().insertIntegrityEvent(event)
                notificationService.notifyIntegrityAlert(
                    sessionId = sessionId,
                    eventType = "High Input Velocity Detected",
                    description = "Submission velocity logged for recruiter evaluation."
                )
            }
        }
    }

    fun recordCameraFeedInterrupted(sessionId: String, reason: String) {
        if (!isMonitoringEnabled) return

        val event = IntegrityEventEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            eventType = "CAMERA_FEED_INTERRUPTED",
            timestamp = System.currentTimeMillis(),
            severity = IntegritySeverity.LOW,
            confidence = 85,
            description = "Optional verification camera feed status update: $reason.",
            isConfirmedViolation = false,
            recruiterReviewNotes = ""
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.interviewDao().insertIntegrityEvent(event)
            notificationService.notifyIntegrityAlert(
                sessionId = sessionId,
                eventType = "Camera Verification Status",
                description = "Camera status change logged to audit timeline."
            )
        }
    }
}
