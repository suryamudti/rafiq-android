package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.IslamicEvent
import com.smiledev.rafiq_quran.domain.repository.IslamicCalendarRepository

class GetIslamicEventsUseCase(
    private val islamicCalendarRepository: IslamicCalendarRepository
) {
    operator fun invoke(): Result<List<IslamicEvent>, AppError> {
        return islamicCalendarRepository.getEvents()
    }
}
