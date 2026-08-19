package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.ProphetStory

interface ProphetRepository {
    fun getProphets(): Result<List<ProphetStory>, AppError>
    fun getProphetById(id: Int): Result<ProphetStory?, AppError>
}
