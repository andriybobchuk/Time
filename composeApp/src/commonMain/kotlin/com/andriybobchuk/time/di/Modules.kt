package com.andriybobchuk.time.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.andriybobchuk.time.core.data.HttpClientFactory
import com.andriybobchuk.time.core.data.database.AppDatabase
import com.andriybobchuk.time.core.data.database.MooneyDatabaseFactory
import com.andriybobchuk.time.mooney.data.DefaultCoreRepositoryImpl
import com.andriybobchuk.time.mooney.domain.CoreRepository
import com.andriybobchuk.time.mooney.domain.usecase.*
import com.andriybobchuk.time.mooney.presentation.account.AccountViewModel
import com.andriybobchuk.time.mooney.presentation.analytics.AnalyticsViewModel
import com.andriybobchuk.time.mooney.presentation.transaction.TransactionViewModel
import com.andriybobchuk.time.time.data.DefaultTimeRepositoryImpl
import com.andriybobchuk.time.time.domain.TimeRepository
import com.andriybobchuk.time.time.domain.usecase.*
import com.andriybobchuk.time.time.presentation.AnalyticsViewModel as TimeAnalyticsViewModel
import com.andriybobchuk.time.time.presentation.TimeTrackingViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    single { HttpClientFactory.create(get()) }

    singleOf(::DefaultCoreRepositoryImpl).bind<CoreRepository>()
    singleOf(::DefaultTimeRepositoryImpl).bind<TimeRepository>()

    single {
        get<MooneyDatabaseFactory>().create()
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single { get<AppDatabase>().accountDao }
    single { get<AppDatabase>().transactionDao }
    single { get<AppDatabase>().timeBlockDao }

    // Mooney Use Cases
    singleOf(::AddTransactionUseCase)
    singleOf(::DeleteTransactionUseCase)
    singleOf(::GetTransactionsUseCase)
    singleOf(::AddAccountUseCase)
    singleOf(::DeleteAccountUseCase)
    singleOf(::GetAccountsUseCase)
    singleOf(::CalculateMonthlyAnalyticsUseCase)

    // Time Tracking Use Cases
    singleOf(::StartTimeTrackingUseCase)
    singleOf(::StopTimeTrackingUseCase)
    singleOf(::GetTimeBlocksUseCase)
    singleOf(::GetActiveTimeBlockUseCase)
    singleOf(::GetJobsUseCase)
    singleOf(::GetDailySummaryUseCase)
    singleOf(::GetWeeklyAnalyticsUseCase)
    singleOf(::GetLast7DaysAnalyticsUseCase)
    singleOf(::DeleteTimeBlockUseCase)
    singleOf(::UpsertTimeBlockUseCase)

    // ViewModels
    viewModelOf(::AccountViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::TimeTrackingViewModel)
    viewModelOf(::TimeAnalyticsViewModel)
}