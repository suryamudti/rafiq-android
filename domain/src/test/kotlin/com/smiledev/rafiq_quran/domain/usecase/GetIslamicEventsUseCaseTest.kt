package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.IslamicEvent
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class GetIslamicEventsUseCaseTest {

    private val repository: IslamicCalendarRepository = mockk()
    private val useCase = GetIslamicEventsUseCase(repository)

    @Test
    fun `invoke delegates to repository and returns success`() {
        val events = listOf(IslamicEvent(10, 1, "Eid al-Fitr", "Idul Fitri", "End of Ramadan", "Akhir Ramadhan", "holiday"))
        every { repository.getEvents() } returns Result.Success(events)

        val result = useCase()

        assertTrue(result is Result.Success)
        verify(exactly = 1) { repository.getEvents() }
    }

    @Test
    fun `invoke returns error when repository fails`() {
        every { repository.getEvents() } returns Result.Error(AppError.Network("timeout"))

        val result = useCase()

        assertTrue(result is Result.Error)
        verify(exactly = 1) { repository.getEvents() }
    }
}
