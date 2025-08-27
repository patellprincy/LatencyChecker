package com.example.latencychecker.work

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.latencychecker.R
import com.example.latencychecker.UsageStatsHelper

class UsageAlertWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val end = System.currentTimeMillis()
        val start = end - 60 * 60 * 1000 // last hour
        val list = UsageStatsHelper.getAppDataUsage(applicationContext, start, end)
        val heavy = list.firstOrNull { it.totalBytes > 100L * 1024 * 1024 } // >100MB/hr
        heavy?.let {
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            val notif = NotificationCompat.Builder(applicationContext, "alerts")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("High data usage")
                .setContentText("${it.appName} used ${(it.totalBytes / (1024*1024))} MB in the last hour")
                .setAutoCancel(true)
                .build()
            nm.notify(1001, notif)
        }
        return Result.success()
    }
}
