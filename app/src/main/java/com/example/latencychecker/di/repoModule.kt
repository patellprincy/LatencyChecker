package com.example.latencychecker.di

import com.example.latencychecker.UsageRepo
import com.example.latencychecker.UsageRepoImpl
import com.example.latencychecker.UsageStatsHelper
import org.koin.dsl.module

val repoModule = module {
    // object UsageStatsHelper can be injected as a singleton
    single { UsageStatsHelper }

    // Repo that composes helper + DAO
    single<UsageRepo> { UsageRepoImpl(helper = get(), dao = get()) }
}
