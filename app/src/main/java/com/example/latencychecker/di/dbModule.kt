package com.example.latencychecker.di

import android.app.Application
import androidx.room.Room
import com.example.latencychecker.AppDatabase
import com.example.latencychecker.UsageSnapshotDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dbModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "netwatch.db"
        ).fallbackToDestructiveMigration().build()
    }
    single<UsageSnapshotDao> { get<AppDatabase>().usageDao() }
}
