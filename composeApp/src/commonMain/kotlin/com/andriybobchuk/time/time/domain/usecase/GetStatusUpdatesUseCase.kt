package com.andriybobchuk.time.time.domain.usecase

import com.andriybobchuk.time.time.domain.StatusUpdate
import com.andriybobchuk.time.time.domain.TimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetStatusUpdatesUseCase(
    private val repository: TimeRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<StatusUpdate>> {
        return repository.getStatusUpdatesByDate(date)
    }
}