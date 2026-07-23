package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.AsmaulHusna
import com.smiledev.rafiq.domain.repository.AsmaulHusnaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAsmaulHusnaUseCaseTest {

    private val repository: AsmaulHusnaRepository = mockk()
    private val useCase = GetAsmaulHusnaUseCase(repository)

    @Test
    fun `invoke delegates to repository and returns success`() {
        val names = listOf(AsmaulHusna(1, "الرحمن", "Ar-Rahman", "The Most Gracious", "Maha Pengasih", "Benefit", "Manfaat"))
        every { repository.getNames() } returns Result.Success(names)

        val result = useCase()

        assertTrue(result is Result.Success)
        verify(exactly = 1) { repository.getNames() }
    }

    @Test
    fun `invoke returns error when repository fails`() {
        every { repository.getNames() } returns Result.Error(AppError.Database("error"))

        val result = useCase()

        assertTrue(result is Result.Error)
        verify(exactly = 1) { repository.getNames() }
    }
}
