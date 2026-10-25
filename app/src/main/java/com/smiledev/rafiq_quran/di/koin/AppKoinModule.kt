package com.smiledev.rafiq_quran.di.koin

import org.koin.dsl.module

val appKoinModules = listOf(
    coreModule,
    networkModule,
    dataModule,
    domainModule
)
