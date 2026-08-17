package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.IslamicEvent
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository

class GetIslamicEventsUseCase(
    private val islamicCalendarRepository: IslamicCalendarRepository
) {
    operator fun invoke(): Result<List<IslamicEvent>, AppError> {
        return islamicCalendarRepository.getEvents()
    }
}
