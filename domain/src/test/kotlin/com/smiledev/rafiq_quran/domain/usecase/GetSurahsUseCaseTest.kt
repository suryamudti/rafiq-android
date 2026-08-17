package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.QuranRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSurahsUseCaseTest {

    private val repository: QuranRepository = mockk()
    private val useCase = GetSurahsUseCase(repository)

    @Test
    fun `invoke delegates to repository and returns success`() {
        val chapters = listOf(Surah(1, 1, "الفاتحة", "Al-Fatiha", "The Opening", 7, "Mecca"))
        every { repository.getChapters("en") } returns Result.Success(chapters)

        val result = useCase("en")

        assertTrue(result is Result.Success)
        verify(exactly = 1) { repository.getChapters("en") }
    }

    @Test
    fun `invoke returns error when repository fails`() {
        every { repository.getChapters("id") } returns Result.Error(AppError.Database("db error"))

        val result = useCase("id")

        assertTrue(result is Result.Error)
        verify(exactly = 1) { repository.getChapters("id") }
    }
}
