package com.smiledev.rafiq_quran.domain.model

data class TasbihHistoryItem(
    val id: Long = 0,
    val date: String,
    val dhikrId: Int,
    val dhikrName: String,
    val arabic: String = "",
    val count: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class TasbihDayHistory(
    val date: String,
    val totalCount: Int,
    val items: List<TasbihHistoryItem>
)
