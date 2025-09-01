package com.example.latencychecker.di

import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val coreModule = module {
    single { Dispatchers.IO }
}
