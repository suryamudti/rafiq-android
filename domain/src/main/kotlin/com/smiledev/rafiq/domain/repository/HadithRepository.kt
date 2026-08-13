package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook

interface HadithRepository {
    fun getBooks(): Result<List<HadithBook>, AppError>
    fun getHadithsByBook(bookId: String): Result<List<Hadith>, AppError>
}