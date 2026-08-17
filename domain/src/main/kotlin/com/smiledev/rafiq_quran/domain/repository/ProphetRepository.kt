package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.ProphetStory

interface ProphetRepository {
    fun getProphets(): Result<List<ProphetStory>, AppError>
    fun getProphetById(id: Int): Result<ProphetStory?, AppError>
}
