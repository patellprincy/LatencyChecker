package com.example.latencychecker

import android.app.Application
import com.example.latencychecker.di.viewModule
import org.koin.core.context.startKoin
import org.koin.android.ext.koin.androidContext

class LatencyCheckerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@LatencyCheckerApp)
            modules(viewModule) // Keep only this for now
        }
    }
}
