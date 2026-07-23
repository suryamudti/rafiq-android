package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.ProphetStory
import com.smiledev.rafiq.domain.repository.ProphetRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProphetsUseCaseTest {

    private val repository: ProphetRepository = mockk()
    private val useCase = GetProphetsUseCase(repository)

    @Test
    fun `invoke delegates to repository and returns success`() {
        val prophets = listOf(ProphetStory(1, "آدم", "Adam", "Adam", "First prophet", "Nabi pertama", "Story...", "Kisah...", "Miracles...", "Mukjizat..."))
        every { repository.getProphets() } returns Result.Success(prophets)

        val result = useCase()

        assertTrue(result is Result.Success)
        verify(exactly = 1) { repository.getProphets() }
    }

    @Test
    fun `invoke returns error when repository fails`() {
        every { repository.getProphets() } returns Result.Error(AppError.Database("error"))

        val result = useCase()

        assertTrue(result is Result.Error)
        verify(exactly = 1) { repository.getProphets() }
    }
}
