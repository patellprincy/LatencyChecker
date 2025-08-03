package com.example.latencychecker.di

import com.example.latencychecker.viewmodel.DashboardViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModule = module {
    viewModel { DashboardViewModel(get()) }
}
