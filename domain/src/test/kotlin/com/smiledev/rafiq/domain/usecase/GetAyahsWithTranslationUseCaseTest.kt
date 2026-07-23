package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.repository.QuranRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAyahsWithTranslationUseCaseTest {

    private val repository: QuranRepository = mockk()
    private val useCase = GetAyahsWithTranslationUseCase(repository)

    @Test
    fun `invoke delegates to repository with suraNumber and locale`() {
        val ayahs = listOf(Ayah(1, 1, "Bismillah", null))
        every { repository.getAyahsWithTranslation(1, "en") } returns Result.Success(ayahs)

        val result = useCase(1, "en")

        assertTrue(result is Result.Success)
        verify(exactly = 1) { repository.getAyahsWithTranslation(1, "en") }
    }

    @Test
    fun `invoke returns error when repository fails`() {
        every { repository.getAyahsWithTranslation(any(), any()) } returns Result.Error(AppError.Database("error"))

        val result = useCase(1, "id")

        assertTrue(result is Result.Error)
        verify(exactly = 1) { repository.getAyahsWithTranslation(1, "id") }
    }
}
