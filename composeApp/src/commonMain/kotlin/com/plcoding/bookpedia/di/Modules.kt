package com.plcoding.bookpedia.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.plcoding.bookpedia.mooney.data.DefaultCoreRepositoryImpl
import AccountViewModel
import com.plcoding.bookpedia.mooney.presentation.analytics.AnalyticsViewModel
import com.plcoding.bookpedia.core.data.HttpClientFactory
import com.plcoding.bookpedia.mooney.domain.CoreRepository
import com.recallit.core.data.database.AppDatabase
import com.recallit.transactions.presentation.TransactionViewModel

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    single { HttpClientFactory.create(get()) }

    singleOf(::DefaultCoreRepositoryImpl).bind<CoreRepository>()

    single {
        get<com.recallit.core.data.database.MooneyDatabaseFactory>().create()
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single { get<AppDatabase>().accountDao }
    single { get<AppDatabase>().transactionDao }

    viewModelOf(::AccountViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::AnalyticsViewModel)
}