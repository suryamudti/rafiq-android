package com.smiledev.rafiq_quran.ui.calendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.smiledev.rafiq_quran.domain.model.GregorianDate
import com.smiledev.rafiq_quran.domain.model.HijriDate
import com.smiledev.rafiq_quran.domain.model.IslamicEvent
import com.smiledev.rafiq_quran.domain.util.HijriDateConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class SharedCalendarDay(
    val gregorianDay: Int,
    val gregorianMonth: Int,
    val gregorianYear: Int,
    val hijriDate: HijriDate,
    val events: List<IslamicEvent>,
    val isToday: Boolean
)

val DEFAULT_ISLAMIC_EVENTS = listOf(
    IslamicEvent(
        hijriMonth = 1,
        hijriDay = 1,
        titleEn = "Islamic New Year (1447 H)",
        titleId = "Tahun Baru Islam (1 Muharram)",
        descriptionEn = "Commemorates the Hijrah of Prophet Muhammad (PBUH) from Makkah to Madinah.",
        descriptionId = "Peringatan hijrah Nabi Muhammad SAW dari Makkah ke Madinah.",
        eventType = "HOLIDAY"
    ),
    IslamicEvent(
        hijriMonth = 1,
        hijriDay = 10,
        titleEn = "Day of Ashura",
        titleId = "Hari Asyura (10 Muharram)",
        descriptionEn = "Sunnah day of fasting commemorating Allah saving Musa (AS) and the Israelites.",
        descriptionId = "Puasa sunnah Asyura mengenang keselamatan Nabi Musa AS.",
        eventType = "SUNNAH_FASTING"
    ),
    IslamicEvent(
        hijriMonth = 3,
        hijriDay = 12,
        titleEn = "Mawlid al-Nabi",
        titleId = "Maulid Nabi Muhammad SAW",
        descriptionEn = "Commemorating the birth of the final Prophet Muhammad (PBUH).",
        descriptionId = "Peringatan hari kelahiran Rasulullah SAW.",
        eventType = "SPECIAL_DAY"
    ),
    IslamicEvent(
        hijriMonth = 7,
        hijriDay = 27,
        titleEn = "Isra and Mi'raj",
        titleId = "Isra Mi'raj",
        descriptionEn = "The miraculous night journey and ascension to the heavens where 5 daily prayers were ordained.",
        descriptionId = "Perjalanan agung malam hari dan mi'raj diterimanya perintah shalat 5 waktu.",
        eventType = "SPECIAL_DAY"
    ),
    IslamicEvent(
        hijriMonth = 8,
        hijriDay = 15,
        titleEn = "Nisfu Sha'ban",
        titleId = "Malam Nisfu Sya'ban",
        descriptionEn = "Night of forgiveness and preparation for the holy month of Ramadan.",
        descriptionId = "Malam pengampunan dan persiapan menyambut bulan suci Ramadan.",
        eventType = "SPECIAL_DAY"
    ),
    IslamicEvent(
        hijriMonth = 9,
        hijriDay = 1,
        titleEn = "First Day of Ramadan",
        titleId = "Awal Bulan Suci Ramadan",
        descriptionEn = "Beginning of the blessed month of obligatory fasting and intense Quran recitation.",
        descriptionId = "Awal puasa Ramadan wajib bagi seluruh umat Islam.",
        eventType = "HOLIDAY"
    ),
    IslamicEvent(
        hijriMonth = 9,
        hijriDay = 17,
        titleEn = "Nuzulul Qur'an",
        titleId = "Nuzulul Qur'an",
        descriptionEn = "Commemoration of the initial revelation of the Holy Qur'an.",
        descriptionId = "Peringatan turunnya ayat pertama Al-Qur'an.",
        eventType = "SPECIAL_DAY"
    ),
    IslamicEvent(
        hijriMonth = 9,
        hijriDay = 27,
        titleEn = "Laylat al-Qadr (Estimated)",
        titleId = "Malam Lailatul Qadar",
        descriptionEn = "The Night of Decree, better than a thousand months of worship.",
        descriptionId = "Malam kemuliaan yang lebih baik daripada seribu bulan.",
        eventType = "SPECIAL_DAY"
    ),
    IslamicEvent(
        hijriMonth = 10,
        hijriDay = 1,
        titleEn = "Eid al-Fitr (1 Shawwal)",
        titleId = "Hari Raya Idul Fitri",
        descriptionEn = "Celebration of breaking the fast at the conclusion of Ramadan.",
        descriptionId = "Hari raya kemenangan setelah sebulan berpuasa Ramadan.",
        eventType = "HOLIDAY"
    ),
    IslamicEvent(
        hijriMonth = 12,
        hijriDay = 9,
        titleEn = "Day of Arafah",
        titleId = "Hari Arafah",
        descriptionEn = "The pinnacle of Hajj; highly recommended fasting expiating sins of two years.",
        descriptionId = "Puncak ibadah haji di padang Arafah; puasa sunnah penghapus dosa dua tahun.",
        eventType = "SUNNAH_FASTING"
    ),
    IslamicEvent(
        hijriMonth = 12,
        hijriDay = 10,
        titleEn = "Eid al-Adha (10 Dhu al-Hijjah)",
        titleId = "Hari Raya Idul Adha",
        descriptionEn = "Feast of Sacrifice honoring Prophet Ibrahim's devotion.",
        descriptionId = "Hari raya kurban memperingati keteladanan Nabi Ibrahim AS.",
        eventType = "HOLIDAY"
    )
)

val ISLAMIC_MONTH_NAMES_EN = listOf(
    "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
    "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
    "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
)

val ISLAMIC_MONTH_NAMES_ID = listOf(
    "Muharram", "Safar", "Rabi'ul Awal", "Rabi'ul Akhir",
    "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban",
    "Ramadan", "Syawal", "Zulkaidah", "Zulhijah"
)

val GREGORIAN_MONTH_NAMES_EN = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

@Immutable
data class SharedCalendarUiState(
    val displayedYear: Int = 2026,
    val displayedMonth: Int = 10,
    val today: GregorianDate = GregorianDate(2026, 10, 28),
    val grid: List<SharedCalendarDay?> = emptyList(),
    val events: List<IslamicEvent> = DEFAULT_ISLAMIC_EVENTS,
    val selectedDay: SharedCalendarDay? = null,
    val searchQuery: String = ""
)

class SharedCalendarViewModel(
    initialYear: Int = 2026,
    initialMonth: Int = 10,
    initialDay: Int = 28,
    private val eventsList: List<IslamicEvent> = DEFAULT_ISLAMIC_EVENTS
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SharedCalendarUiState(
            displayedYear = initialYear,
            displayedMonth = initialMonth,
            today = GregorianDate(initialYear, initialMonth, initialDay),
            events = eventsList
        )
    )
    val uiState: StateFlow<SharedCalendarUiState> = _uiState.asStateFlow()

    init {
        refreshGrid()
    }

    fun nextMonth() {
        _uiState.update { s ->
            val nextMonth = if (s.displayedMonth == 12) 1 else s.displayedMonth + 1
            val nextYear = if (s.displayedMonth == 12) s.displayedYear + 1 else s.displayedYear
            s.copy(displayedYear = nextYear, displayedMonth = nextMonth, selectedDay = null)
        }
        refreshGrid()
    }

    fun previousMonth() {
        _uiState.update { s ->
            val prevMonth = if (s.displayedMonth == 1) 12 else s.displayedMonth - 1
            val prevYear = if (s.displayedMonth == 1) s.displayedYear - 1 else s.displayedYear
            s.copy(displayedYear = prevYear, displayedMonth = prevMonth, selectedDay = null)
        }
        refreshGrid()
    }

    fun goToToday() {
        _uiState.update { s ->
            s.copy(
                displayedYear = s.today.year,
                displayedMonth = s.today.month,
                selectedDay = null
            )
        }
        refreshGrid()
    }

    fun selectDay(day: SharedCalendarDay?) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun getMonthName(month: Int): String {
        return GREGORIAN_MONTH_NAMES_EN.getOrElse(month - 1) { "" }
    }

    fun getHijriMonthName(month: Int): String {
        return ISLAMIC_MONTH_NAMES_EN.getOrElse(month - 1) { "" }
    }

    fun getFilteredEvents(state: SharedCalendarUiState): List<IslamicEvent> {
        val query = state.searchQuery.trim().lowercase()
        if (query.isEmpty()) return state.events
        return state.events.filter { event ->
            event.titleEn.lowercase().contains(query) ||
            event.titleId.lowercase().contains(query) ||
            event.descriptionEn.lowercase().contains(query) ||
            event.descriptionId.lowercase().contains(query)
        }
    }

    private fun refreshGrid() {
        val s = _uiState.value
        val grid = buildGrid(s.displayedYear, s.displayedMonth, s.today, s.events)
        _uiState.update { it.copy(grid = grid) }
    }

    private fun buildGrid(
        year: Int,
        month: Int,
        today: GregorianDate,
        events: List<IslamicEvent>
    ): List<SharedCalendarDay?> {
        val daysInMonth = HijriDateConverter.daysInGregorianMonth(year, month)
        val firstDayWeekday = HijriDateConverter.weekdayOf(year, month, 1)

        val grid = mutableListOf<SharedCalendarDay?>()
        for (i in 0 until firstDayWeekday) {
            grid.add(null)
        }

        for (day in 1..daysInMonth) {
            val hijri = HijriDateConverter.gregorianToHijri(year, month, day)
            val dayEvents = events.filter {
                it.hijriMonth == hijri.month && it.hijriDay == hijri.day
            }
            val isToday = (year == today.year && month == today.month && day == today.day)
            grid.add(
                SharedCalendarDay(
                    gregorianDay = day,
                    gregorianMonth = month,
                    gregorianYear = year,
                    hijriDate = hijri,
                    events = dayEvents,
                    isToday = isToday
                )
            )
        }
        return grid
    }
}
