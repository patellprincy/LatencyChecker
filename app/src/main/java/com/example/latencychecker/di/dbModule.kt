package com.example.latencychecker.di

import androidx.room.Room
import com.example.latencychecker.data.local.AppDatabase
import com.example.latencychecker.data.local.UsageSnapDao
import org.koin.dsl.module

val dbModule = module {
    single {
        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            "latency_checker.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    single<UsageSnapDao> { get<AppDatabase>().usageSnapDao() }
}
