package com.ledgeros.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ledgeros.app.MainActivity
import com.ledgeros.app.R
import com.ledgeros.app.data.local.LedgerDatabase
import com.ledgeros.app.data.local.toModel
import com.ledgeros.app.model.ComplianceStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Runs daily via WorkManager.
 * Fires a notification for every compliance task that is not Done and due within 7 days.
 */
class BasNotificationWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = LedgerDatabase.getInstance(context).ledgerDao()
        val today = LocalDate.now()

        dao.complianceTasks()
            .map { it.toModel() }
            .filter { it.status != ComplianceStatus.Done }
            .forEach { task ->
                val daysUntil = ChronoUnit.DAYS.between(today, task.dueDate)
                if (daysUntil in 0..7) {
                    val urgency = when (daysUntil) {
                        0L -> "due TODAY"
                        1L -> "due tomorrow"
                        else -> "due in $daysUntil days"
                    }
                    postNotification(
                        id = task.id.hashCode(),
                        title = "${task.taskType.label} $urgency",
                        body = "Tap to review your compliance obligations in LedgerOS.",
                    )
                }
            }

        return Result.success()
    }

    private fun postNotification(id: Int, title: String, body: String) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel — idempotent on Android 8+
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "BAS Deadlines",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Upcoming BAS and compliance deadline reminders"
            },
        )

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_NAVIGATE_TO, "compliance")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        manager.notify(
            id,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        const val CHANNEL_ID = "bas_deadlines"
        const val WORK_NAME = "bas_notification_check"
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }
}
