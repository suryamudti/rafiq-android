package com.smiledev.rafiq_quran.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResultTest {

    @Test
    fun successReturnsData() {
        val result = Result.Success("data")
        assertEquals("data", result.data)
    }

    @Test
    fun errorReturnsError() {
        val error = AppError.NotFound
        val result = Result.Error(error)
        assertEquals(error, result.error)
        assertTrue(result.error is AppError.NotFound)
    }
}
