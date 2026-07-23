package com.smiledev.rafiq.ui.tasbih

import android.content.Context
import android.os.Vibrator
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TasbihViewModelTest {

    private val context: Context = mockk<Context>(relaxed = true).apply {
        every { getSystemService(Context.VIBRATOR_SERVICE) } returns mockk<Vibrator>(relaxed = true)
    }

    @Test
    fun `initial count is zero`() {
        val vm = TasbihViewModel(context = context)
        assertEquals(0, vm.uiState.value.count)
    }

    @Test
    fun `increment increases count`() {
        val vm = TasbihViewModel(context = context)
        vm.increment()
        assertEquals(1, vm.uiState.value.count)
    }

    @Test
    fun `multiple increments`() {
        val vm = TasbihViewModel(context = context)
        repeat(5) { vm.increment() }
        assertEquals(5, vm.uiState.value.count)
    }

    @Test
    fun `reset sets count to zero`() {
        val vm = TasbihViewModel(context = context)
        repeat(10) { vm.increment() }
        vm.reset()
        assertEquals(0, vm.uiState.value.count)
    }
}
