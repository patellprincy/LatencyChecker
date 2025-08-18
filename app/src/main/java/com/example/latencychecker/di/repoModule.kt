package com.example.latencychecker.di

import com.example.latencychecker.UsageRepo
import com.example.latencychecker.data.local.UsageSnapDao
import org.koin.dsl.module

val repoModule = module {
    single { UsageRepo(get<UsageSnapDao>()) }
}
