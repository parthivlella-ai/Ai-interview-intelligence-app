package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.NotificationItemEntity
import com.example.data.local.SkillSenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class NotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "skillsense_realtime_channel"
        const val CHANNEL_NAME = "SkillSense Interview Updates"
        const val CHANNEL_DESCRIPTION = "Real-time alerts for interview sessions, integrity monitoring, feedback, and recruiter notes"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val database = SkillSenseDatabase.getDatabase(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun dispatchNotification(
        title: String,
        message: String,
        type: String,
        sessionId: String? = null
    ) {
        // 1. Persist notification to offline Room DB
        val notifEntity = NotificationItemEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            notificationType = type,
            targetSessionId = sessionId
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.notificationDao().insertNotification(notifEntity)
        }

        // 2. Post Android system status-bar notification if permission is granted
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_SESSION_ID", sessionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = (System.currentTimeMillis() % 100000).toInt()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            }
        } else {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }

    fun notifySessionStarted(sessionId: String, roleTitle: String) {
        dispatchNotification(
            title = "Adaptive Interview Started",
            message = "Your adaptive session for '$roleTitle' is live. Questions will adapt in real-time.",
            type = "INTERVIEW_READY",
            sessionId = sessionId
        )
    }

    fun notifyIntegrityAlert(sessionId: String, eventType: String, description: String) {
        dispatchNotification(
            title = "Integrity Intelligence Event Logged",
            message = "Event: $eventType. Note: Detected events require recruiter review and are not treated as automatic violations.",
            type = "INTEGRITY_ALERT",
            sessionId = sessionId
        )
    }

    fun notifyAnswerEvaluated(sessionId: String, competency: String, score: Int) {
        dispatchNotification(
            title = "Competency Updated: $score%",
            message = "Evaluated response for '$competency'. Competency map and evidence updated.",
            type = "EVALUATION_DONE",
            sessionId = sessionId
        )
    }

    fun notifyReportGenerated(sessionId: String, roleTitle: String, overallScore: Int) {
        dispatchNotification(
            title = "Assessment Report Ready: $overallScore/100",
            message = "Final evidence-based evaluation for $roleTitle generated. Skill-gap classification finalized.",
            type = "REPORT_GENERATED",
            sessionId = sessionId
        )
    }

    fun notifyRecruiterNotesAdded(sessionId: String, roleTitle: String, verdict: String) {
        dispatchNotification(
            title = "Recruiter Review Updated",
            message = "Recruiter has added notes and updated verdict to '${verdict.replace("_", " ")}' for $roleTitle.",
            type = "RECRUITER_FEEDBACK",
            sessionId = sessionId
        )
    }
}
