package com.surfswirl.bakery.notify

import android.Manifest
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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.surfswirl.bakery.MainActivity
import com.surfswirl.bakery.R
import java.util.concurrent.TimeUnit

/** One gentle "energy is full" notification, scheduled when the app goes to the background. */
object EnergyReminder {
    private const val WORK = "energy_full"
    const val CHANNEL = "energy"

    fun schedule(context: Context, delayMs: Long) {
        if (delayMs <= 0) return cancel(context)
        val req = OneTimeWorkRequestBuilder<EnergyWorker>().setInitialDelay(delayMs, TimeUnit.MILLISECONDS).build()
        runCatching { WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req) }
    }

    fun cancel(context: Context) {
        runCatching { WorkManager.getInstance(context).cancelUniqueWork(WORK) }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Energy full", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel(context)
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val lines = listOf(
            "Your energy is full! Surf is waiting by the oven. 🐶",
            "Energy's full and the bakery smells like cinnamon rolls. Come bake!",
            "Customers are lining up at Surf & Swirl! Your energy is full.",
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_roll)
            .setContentTitle("Surf & Swirl")
            .setContentText(lines.random())
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(1, n) }
    }
}

class EnergyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        EnergyReminder.show(applicationContext)
        return Result.success()
    }
}
