package com.example.latencychecker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.latencychecker.work.UsageAlertWorker
import java.util.concurrent.TimeUnit

import com.example.latencychecker.di.dbModule
import com.example.latencychecker.di.repoModule
import com.example.latencychecker.di.viewModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LatencyCheckerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@LatencyCheckerApp)
            modules(
                listOf(
                    dbModule,
                    repoModule,
                    viewModule
                )
            )
        }
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel("alerts", "NetWatch Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val req = PeriodicWorkRequestBuilder<UsageAlertWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("usage_alerts", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
