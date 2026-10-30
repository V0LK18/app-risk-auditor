package com.example.appriskauditor.data.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.appriskauditor.MainActivity
import com.example.appriskauditor.R
import com.example.appriskauditor.data.history.Snapshot
import com.example.appriskauditor.data.history.SnapshotStore
import com.example.appriskauditor.data.scanner.AppScanner
import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.scoring.PermissionDiff
import java.util.concurrent.TimeUnit

class RiskyAppAlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "RiskyAppAlertWorker"
        const val CHANNEL_ID = "risky_app_alerts_channel"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RiskyAppAlertWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val prefs = appContext.getSharedPreferences("app_risk_auditor_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notify_risky_apps", true)

        if (!notificationsEnabled) {
            return Result.success()
        }

        try {
            val store = SnapshotStore(appContext)
            val appScanner = AppScanner(appContext)

            val hasBaseline = store.hasBaseline()
            val previousSnapshots = store.loadAll()

            val currentApps = appScanner.scan(includeSystem = false)
            val updatedSnapshots = mutableMapOf<String, Snapshot>()

            if (!hasBaseline) {
                // Baseline creation: save snapshots without notifying
                for (app in currentApps) {
                    val snapshot = createSnapshotFromApp(app)
                    updatedSnapshots[app.packageName] = snapshot
                }
                store.saveAll(updatedSnapshots)
                return Result.success()
            }

            var notificationId = 1001

            for (app in currentApps) {
                val oldSnapshot = previousSnapshots[app.packageName]
                val diff = PermissionDiff.compute(old = oldSnapshot, new = app, newResult = app.riskResult, isBaselineScan = false)

                val isHighOrCritical = app.riskResult.band == RiskBand.HIGH || app.riskResult.band == RiskBand.CRITICAL
                val notifiedKey = "${app.packageName}:${app.versionCode}:${app.riskResult.band.name}"

                val shouldAlert = isHighOrCritical && (
                        diff.isNewApp || diff.bandIncreased || diff.addedSensitivePermissions.isNotEmpty()
                        ) && (oldSnapshot?.lastNotifiedKey != notifiedKey)

                var updatedNotifiedKey = oldSnapshot?.lastNotifiedKey

                if (shouldAlert) {
                    postNotification(notificationId++, app)
                    updatedNotifiedKey = notifiedKey
                }

                val newSnapshot = createSnapshotFromApp(app, updatedNotifiedKey)
                updatedSnapshots[app.packageName] = newSnapshot
            }

            store.saveAll(updatedSnapshots)
            return Result.success()
        } catch (e: Exception) {
            Log.e("RiskyAppAlertWorker", "Error in background risk scan worker: ${e.message}")
            return Result.failure()
        }
    }

    private fun createSnapshotFromApp(app: AppInfo, lastNotifiedKey: String? = null): Snapshot {
        return Snapshot(
            packageName = app.packageName,
            versionCode = app.versionCode,
            versionName = app.versionName,
            score = app.riskResult.score,
            band = app.riskResult.band.name,
            sensitivePermissions = PermissionDiff.extractSensitivePermissions(app),
            specialAccessFlags = PermissionDiff.encodeSpecialAccessFlags(app),
            firstSeen = System.currentTimeMillis(),
            lastScanned = System.currentTimeMillis(),
            lastNotifiedKey = lastNotifiedKey
        )
    }

    private fun postNotification(notificationId: Int, app: AppInfo) {
        try {
            createNotificationChannel()

            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("packageName", app.packageName)
            }

            val pendingIntent = PendingIntent.getActivity(
                appContext,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val topReason = app.riskResult.reasons.firstOrNull()?.text ?: "High risk permissions requested"
            val title = "${app.name} (${app.riskResult.band.name} Risk)"
            val text = "Score ${app.riskResult.score}: $topReason"

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            if (NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
                @Suppress("MissingPermission")
                NotificationManagerCompat.from(appContext).notify(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.e("RiskyAppAlertWorker", "Error posting notification: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Risky app alerts"
            val descriptionText = "Notifications when apps present high or critical security risks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
