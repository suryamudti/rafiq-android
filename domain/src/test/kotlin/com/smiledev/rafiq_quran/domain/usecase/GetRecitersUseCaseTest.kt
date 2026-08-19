package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.Reciter
import com.smiledev.rafiq_quran.domain.repository.ReciterRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class GetRecitersUseCaseTest {

    private val repository: ReciterRepository = mockk()
    private val useCase = GetRecitersUseCase(repository)

    @Test
    fun `invoke delegates to repository and returns success`() {
        val reciters = listOf(Reciter(1, "Abdul Basit", "عبد الباسط", "Mujawwad", "Egypt", "https://download.quranicaudio.com/quran/abdul_basit_murattal"))
        every { repository.getReciters() } returns Result.Success(reciters)

        val result = useCase()

        assertTrue(result is Result.Success)
        verify(exactly = 1) { repository.getReciters() }
    }

    @Test
    fun `invoke returns error when repository fails`() {
        every { repository.getReciters() } returns Result.Error(AppError.Network("timeout"))

        val result = useCase()

        assertTrue(result is Result.Error)
        verify(exactly = 1) { repository.getReciters() }
    }
}
