package com.smiledev.rafiq.ui.calendar

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class EventColorTest {

    @Test
    fun `holiday events map to gold`() {
        assertEquals(Color(0xFFB8860B), eventColor("holiday"))
    }

    @Test
    fun `fasting events map to purple`() {
        assertEquals(Color(0xFF7B1FA2), eventColor("fasting"))
    }

    @Test
    fun `observance events map to teal`() {
        assertEquals(Color(0xFF009688), eventColor("observance"))
    }

    @Test
    fun `recommendation events map to teal`() {
        assertEquals(Color(0xFF009688), eventColor("recommendation"))
    }

    @Test
    fun `unknown event types map to teal`() {
        assertEquals(Color(0xFF009688), eventColor("someOtherType"))
    }
}
