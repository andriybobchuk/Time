package com.andriybobchuk.time.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.andriybobchuk.time.core.data.HttpClientFactory
import com.andriybobchuk.time.core.data.database.AppDatabase
import com.andriybobchuk.time.core.data.database.MooneyDatabaseFactory
import com.andriybobchuk.time.core.data.database.MIGRATION_3_6
import com.andriybobchuk.time.core.data.database.MIGRATION_6_7
import com.andriybobchuk.time.core.data.database.MIGRATION_7_8
import com.andriybobchuk.time.core.data.database.MIGRATION_8_9
import com.andriybobchuk.time.mooney.data.DefaultCoreRepositoryImpl
import com.andriybobchuk.time.mooney.domain.CoreRepository
import com.andriybobchuk.time.mooney.domain.usecase.*
import com.andriybobchuk.time.mooney.presentation.account.AccountViewModel
import com.andriybobchuk.time.mooney.presentation.analytics.AnalyticsViewModel
import com.andriybobchuk.time.mooney.presentation.transaction.TransactionViewModel
import com.andriybobchuk.time.time.data.DefaultTimeRepositoryImpl
import com.andriybobchuk.time.time.domain.TimeRepository
import com.andriybobchuk.time.time.domain.usecase.*
import com.andriybobchuk.time.time.domain.usecase.GetAllJobsUseCase
import com.andriybobchuk.time.time.domain.usecase.UpsertJobUseCase
import com.andriybobchuk.time.time.domain.usecase.DeleteJobUseCase
import com.andriybobchuk.time.time.domain.usecase.SetMainJobUseCase
import com.andriybobchuk.time.time.domain.usecase.CalculateContextSwitchTimeUseCase
import com.andriybobchuk.time.time.domain.usecase.GetEightWeekTrendUseCase
import com.andriybobchuk.time.time.domain.usecase.GetWeekDetailsUseCase
import com.andriybobchuk.time.time.presentation.AnalyticsViewModel as TimeAnalyticsViewModel
import com.andriybobchuk.time.time.presentation.AdvancedAnalyticsViewModel
import com.andriybobchuk.time.time.presentation.TimeTrackingViewModel
import com.andriybobchuk.time.time.presentation.TimeSettingsViewModel
import com.andriybobchuk.time.time.presentation.RedesignedAnalyticsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    single { HttpClientFactory.create(get()) }

    singleOf(::DefaultCoreRepositoryImpl).bind<CoreRepository>()
    single<TimeRepository> {
        DefaultTimeRepositoryImpl(
            timeBlockDao = get(),
            statusUpdateDao = get(),
            jobDao = get()
        )
    }

    single {
        get<MooneyDatabaseFactory>().create()
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .build()
    }
    single { get<AppDatabase>().accountDao }
    single { get<AppDatabase>().transactionDao }
    single { get<AppDatabase>().timeBlockDao }
    single { get<AppDatabase>().statusUpdateDao }
    single { get<AppDatabase>().jobDao }

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
    singleOf(::GetStatusUpdatesUseCase)
    singleOf(::UpsertStatusUpdateUseCase)
    singleOf(::HandleCrossMidnightBlocksUseCase)
    
    // Job Management Use Cases
    singleOf(::GetAllJobsUseCase)
    singleOf(::UpsertJobUseCase)
    singleOf(::DeleteJobUseCase)
    singleOf(::SetMainJobUseCase)
    
    // Advanced Analytics Use Cases
    singleOf(::GetWeekAnalyticsUseCase)
    singleOf(::GetSixWeekAnalyticsUseCase)
    singleOf(::GetSixMonthAnalyticsUseCase)
    singleOf(::GetWorkPatternInsightsUseCase)
    singleOf(::GetAdvancedAnalyticsUseCase)
    
    // Redesigned Analytics Use Cases
    singleOf(::CalculateContextSwitchTimeUseCase)
    singleOf(::GetEightWeekTrendUseCase)
    singleOf(::GetWeekDetailsUseCase)

    // ViewModels
    viewModelOf(::AccountViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::TimeTrackingViewModel)
    viewModelOf(::TimeAnalyticsViewModel)
    single {
        TimeSettingsViewModel(
            getAllJobsUseCase = get(),
            upsertJobUseCase = get(),
            deleteJobUseCase = get(),
            setMainJobUseCase = get()
        )
    }
    single {
        AdvancedAnalyticsViewModel(
            getWeekAnalyticsUseCase = get(),
            getSixWeekAnalyticsUseCase = get(),
            getSixMonthAnalyticsUseCase = get(),
            getWorkPatternInsightsUseCase = get()
        )
    }
    single {
        RedesignedAnalyticsViewModel(
            getEightWeekTrendUseCase = get(),
            getWeekDetailsUseCase = get()
        )
    }
}