package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.data.remote.MetalPriceApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetalPriceRepository @Inject constructor(
    private val metalPriceApi: MetalPriceApi
) {
    suspend fun getGoldPricePerGram(): Double = metalPriceApi.getGoldPricePerGram()
    suspend fun getSilverPricePerGram(): Double = metalPriceApi.getSilverPricePerGram()
}
