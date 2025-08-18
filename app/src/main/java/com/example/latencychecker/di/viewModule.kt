package com.example.latencychecker.di

import com.example.latencychecker.AppUsageViewModel
import com.example.latencychecker.viewmodel.DashboardViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModule = module {
    // Adjust constructors if yours differ. Both assume they need UsageRepo.
    viewModel { DashboardViewModel(get()) }
    viewModel { AppUsageViewModel(get()) }
}
