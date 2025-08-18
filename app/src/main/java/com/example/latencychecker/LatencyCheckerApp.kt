package com.example.latencychecker

import android.app.Application
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
    }
}
