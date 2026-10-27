package com.smiledev.rafiq_quran.ui.guidance

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceCategory
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceItem
import com.smiledev.rafiq_quran.domain.model.PrayerStep
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem
import com.smiledev.rafiq_quran.domain.model.SunnahStep
import com.smiledev.rafiq_quran.domain.usecase.GetPrayerGuidanceUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetSunnahGuidanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val DEFAULT_SHARED_PRAYER_ITEMS = listOf(
    PrayerGuidanceItem(
        id = "fajr",
        nameEn = "Fajr Prayer",
        nameId = "Shalat Subuh",
        nameArabic = "صَلَاةُ الفَجْر",
        category = PrayerGuidanceCategory.OBLIGATORY,
        rakaat = 2,
        descriptionEn = "The dawn prayer consisting of two obligatory rak'ahs recited aloud.",
        descriptionId = "Shalat wajib dua rakaat pada waktu fajar dengan bacaan jahr (nyaring).",
        niyyahArabic = "أُصَلِّي فَرْضَ الصُّبْحِ رَكْعَتَيْنِ مُسْتَقْبِلَ الْقِبْلَةِ أَدَاءً لِلَّهِ تَعَالَى",
        niyyahTransliteration = "Usholli fardhas-subhi rak'ataini mustaqbilal-qiblati adaa-an lillaahi ta'aalaa",
        niyyahTranslationEn = "I intend to perform two rak'ahs of the obligatory Fajr prayer facing the Qiblah for Allah.",
        niyyahTranslationId = "Aku berniat shalat fardhu Subuh dua rakaat menghadap kiblat karena Allah Ta'ala.",
        steps = listOf(
            PrayerStep(
                order = 1,
                titleEn = "Takbiratul Ihram & Niyyah",
                titleId = "Takbiratul Ihram & Niat",
                descriptionEn = "Raise hands to shoulder level and say Allahu Akbar with sincere intention in heart.",
                descriptionId = "Mengangkat kedua tangan sejajar bahu/telinga sambil mengucap Takbir dan berniat di dalam hati.",
                arabic = "اللهُ أَكْبَرُ",
                transliteration = "Allahu Akbar",
                translationEn = "Allah is the Greatest",
                translationId = "Allah Maha Besar"
            ),
            PrayerStep(
                order = 2,
                titleEn = "Recitation of Surah Al-Fatihah",
                titleId = "Membaca Surah Al-Fatihah",
                descriptionEn = "Recite Surah Al-Fatihah followed by an additional Surah in both rak'ahs.",
                descriptionId = "Membaca Al-Fatihah dilanjutkan dengan surah pendek pada rakaat pertama dan kedua.",
                arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ...",
                transliteration = "Bismillaahir-rahmaanir-raheem...",
                translationEn = "In the name of Allah, the Most Gracious, the Most Merciful",
                translationId = "Dengan menyebut nama Allah Yang Maha Pengasih lagi Maha Penyayang"
            ),
            PrayerStep(
                order = 3,
                titleEn = "Ruku' (Bowing) & I'tidal",
                titleId = "Ruku' dan I'tidal",
                descriptionEn = "Bow with straight back placing hands on knees, then stand upright praising Allah.",
                descriptionId = "Rukuk dengan punggung lurus memegang lutut dengan tuma'ninah, lalu bangkit tegak bertahmid.",
                arabic = "سُبْحَانَ رَبِّيَ الْعَظِيمِ وَبِحَمْدِهِ",
                transliteration = "Subhaana Rabbiyal-'Adheemi wa bihamdih",
                translationEn = "Glory be to my Lord the Most Magnificent and Praise be to Him",
                translationId = "Maha Suci Tuhanku Yang Maha Agung dan segala puji bagi-Nya"
            ),
            PrayerStep(
                order = 4,
                titleEn = "Sujud (Prostration) & Tashahhud",
                titleId = "Sujud dan Tahiyyat",
                descriptionEn = "Prostrate on seven bones, sit between prostrations, and complete final sitting with Salam.",
                descriptionId = "Sujud di atas 7 anggota tubuh, duduk di antara dua sujud, dan selesaikan dengan tasyahhud akhir serta salam.",
                arabic = "سُبْحَانَ رَبِّيَ الأَعْلَى وَبِحَمْدِهِ",
                transliteration = "Subhaana Rabbiyal-A'laa wa bihamdih",
                translationEn = "Glory be to my Lord the Most High and Praise be to Him",
                translationId = "Maha Suci Tuhanku Yang Maha Tinggi dan segala puji bagi-Nya"
            )
        ),
        tipsEn = listOf("Recite Al-Fatihah attentively", "Perform Qunut if following Shafi'i jurisprudence"),
        tipsId = listOf("Membaca Al-Fatihah dengan tartil", "Membaca doa qunut pada rakaat kedua")
    ),
    PrayerGuidanceItem(
        id = "dhuhr",
        nameEn = "Dhuhr Prayer",
        nameId = "Shalat Dzuhur",
        nameArabic = "صَلَاةُ الظُّهْر",
        category = PrayerGuidanceCategory.OBLIGATORY,
        rakaat = 4,
        descriptionEn = "The midday prayer consisting of four obligatory silent rak'ahs.",
        descriptionId = "Shalat wajib empat rakaat di waktu siang dengan bacaan sirr (hening).",
        niyyahArabic = "أُصَلِّي فَرْضَ الظُّهْرِ أَرْبَعَ رَكَعَاتٍ مُسْتَقْبِلَ الْقِبْلَةِ أَدَاءً لِلَّهِ تَعَالَى",
        niyyahTransliteration = "Usholli fardhaz-zhuhri arba'a raka'aatin mustaqbilal-qiblati adaa-an lillaahi ta'aalaa",
        niyyahTranslationEn = "I intend to perform four rak'ahs of the obligatory Dhuhr prayer facing the Qiblah for Allah.",
        niyyahTranslationId = "Aku berniat shalat fardhu Dzuhur empat rakaat menghadap kiblat karena Allah Ta'ala."
    ),
    PrayerGuidanceItem(
        id = "wudu",
        nameEn = "Wudu (Ablution)",
        nameId = "Wudhu",
        nameArabic = "الْوُضُوء",
        category = PrayerGuidanceCategory.PURIFICATION,
        descriptionEn = "The ritual purification required before performing prayer.",
        descriptionId = "Bersuci dari hadats kecil sebagai syarat sah shalat.",
        steps = listOf(
            PrayerStep(order = 1, titleEn = "Niyyah and Basmalah", titleId = "Niat dan Bismillah", descriptionEn = "Form sincere intention and say Bismillah.", descriptionId = "Berniat dalam hati dan mengucapkan basmalah."),
            PrayerStep(order = 2, titleEn = "Wash Hands & Rinse Mouth", titleId = "Cuci Tangan & Berkumur", descriptionEn = "Wash both hands up to wrists, rinse mouth and nose.", descriptionId = "Mencuci kedua telapak tangan, berkumur dan istinsyaq."),
            PrayerStep(order = 3, titleEn = "Wash Face and Arms", titleId = "Basuh Wajah dan Lengan", descriptionEn = "Wash entire face 3 times, then wash arms up to elbows.", descriptionId = "Membasuh seluruh wajah 3 kali, lalu kedua tangan hingga siku."),
            PrayerStep(order = 4, titleEn = "Wipe Head & Wash Feet", titleId = "Usap Kepala & Basuh Kaki", descriptionEn = "Wipe head and ears, then wash feet up to ankles.", descriptionId = "Mengusap sebagian/seluruh kepala dan telinga, lalu membasuh kaki hingga mata kaki.")
        )
    )
)

val DEFAULT_SHARED_SUNNAH_ITEMS = listOf(
    SunnahGuidanceItem(
        id = "duha",
        titleEn = "Duha Prayer",
        titleId = "Shalat Dhuha",
        titleArabic = "صَلَاةُ الضُّحَى",
        category = SunnahCategory.PRAYER,
        summaryEn = "Forenoon sunnah prayer bringing blessings, forgiveness, and sustenance.",
        summaryId = "Shalat sunnah di waktu dhuha pembawa keberkahan, ampunan, dan kelapangan rezeki.",
        descriptionEn = "Performed between sunrise (when the sun has risen about a spear's length) and shortly before Dhuhr. Typically 2 to 8 rak'ahs.",
        descriptionId = "Dilaksanakan setelah matahari terbit setinggi tombak hingga menjelang waktu dzuhur, umumnya 2 hingga 8 rakaat.",
        virtuesEn = listOf("Equivalent to giving charity for every joint in the body", "A sanctuary for repentance"),
        virtuesId = listOf("Sedekah bagi seluruh 360 persendian tubuh", "Shalat orang-orang yang senantiasa bertaubat (Awwabin)"),
        steps = listOf(
            SunnahStep(order = 1, titleEn = "Intention and Two Rak'ahs", titleId = "Niat dan 2 Rakaat", descriptionEn = "Pray two rak'ahs with recitation of Surah Ash-Shams or Ad-Duha.", descriptionId = "Shalat dua rakaat dengan membaca Surah Asy-Syams atau Ad-Dhuha.")
        )
    ),
    SunnahGuidanceItem(
        id = "tahajjud",
        titleEn = "Tahajjud (Night Prayer)",
        titleId = "Shalat Tahajjud",
        titleArabic = "صَلَاةُ التَّهَجُّد",
        category = SunnahCategory.PRAYER,
        summaryEn = "Voluntary night prayer performed after waking from sleep.",
        summaryId = "Shalat sunnah malam yang dikerjakan setelah bangun dari tidur.",
        descriptionEn = "One of the most elevated acts of worship, bringing intimate closeness to Allah during the last third of the night.",
        descriptionId = "Ibadah sunnah paling utama yang mendatangkan kemuliaan di sisi Allah pada sepertiga malam terakhir.",
        virtuesEn = listOf("Praised directly in Surah Al-Isra (17:79)", "Duas are accepted during the descent of divine mercy"),
        virtuesId = listOf("Dipuji langsung dalam Al-Qur'an (Surah Al-Isra': 79)", "Waktu mustajab terkabulnya doa dan ampunan")
    ),
    SunnahGuidanceItem(
        id = "friday_sunnahs",
        titleEn = "Friday Sunnahs",
        titleId = "Amalan Sunnah Hari Jumat",
        titleArabic = "سُنَنُ يَوْمِ الجُمُعَة",
        category = SunnahCategory.FRIDAY,
        summaryEn = "Special observances for the best day of the week (Sayyid al-Ayyam).",
        summaryId = "Amalan-amalan istimewa pada hari terbaik dalam sepekan.",
        descriptionEn = "Includes bathing (Ghusl), applying perfume, wearing clean garments, reciting Surah Al-Kahf, and sending abundant blessings upon the Prophet ﷺ.",
        descriptionId = "Meliputi mandi sunnah, memakai wewangian, mengenakan pakaian terbaik, membaca Surah Al-Kahfi, dan memperbanyak shalawat.",
        virtuesEn = listOf("Light between the two Fridays for reciting Surah Al-Kahf", "Hour of answered prayers (Sa'at al-Ijabah)"),
        virtuesId = listOf("Cahaya penerang antara dua Jumat", "Terdapat waktu terkabulnya doa tanpa tertolak")
    )
)

enum class GuidanceTab(val title: String) {
    PRAYER("Prayer Guidance"),
    SUNNAH("Sunnah Guidance")
}

@Immutable
data class SharedGuidanceUiState(
    val selectedTab: GuidanceTab = GuidanceTab.PRAYER,
    val prayerItems: List<PrayerGuidanceItem> = DEFAULT_SHARED_PRAYER_ITEMS,
    val sunnahItems: List<SunnahGuidanceItem> = DEFAULT_SHARED_SUNNAH_ITEMS,
    val selectedPrayerItem: PrayerGuidanceItem? = null,
    val selectedSunnahItem: SunnahGuidanceItem? = null,
    val prayerCategoryFilter: PrayerGuidanceCategory? = null,
    val sunnahCategoryFilter: SunnahCategory? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)

class SharedGuidanceViewModel(
    private val getPrayerGuidanceUseCase: GetPrayerGuidanceUseCase? = null,
    private val getSunnahGuidanceUseCase: GetSunnahGuidanceUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedGuidanceUiState())
    val uiState: StateFlow<SharedGuidanceUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        if (getPrayerGuidanceUseCase == null && getSunnahGuidanceUseCase == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val prayers = getPrayerGuidanceUseCase?.invoke()
            val sunnahs = getSunnahGuidanceUseCase?.invoke()

            _uiState.update { state ->
                val pList = if (prayers is Result.Success && prayers.data.isNotEmpty()) prayers.data else DEFAULT_SHARED_PRAYER_ITEMS
                val sList = if (sunnahs is Result.Success && sunnahs.data.isNotEmpty()) sunnahs.data else DEFAULT_SHARED_SUNNAH_ITEMS
                state.copy(
                    prayerItems = pList,
                    sunnahItems = sList,
                    isLoading = false
                )
            }
        }
    }

    fun selectTab(tab: GuidanceTab) {
        _uiState.update { it.copy(selectedTab = tab, selectedPrayerItem = null, selectedSunnahItem = null) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectPrayerCategory(cat: PrayerGuidanceCategory?) {
        _uiState.update { it.copy(prayerCategoryFilter = cat) }
    }

    fun selectSunnahCategory(cat: SunnahCategory?) {
        _uiState.update { it.copy(sunnahCategoryFilter = cat) }
    }

    fun selectPrayerItem(item: PrayerGuidanceItem?) {
        _uiState.update { it.copy(selectedPrayerItem = item) }
    }

    fun selectSunnahItem(item: SunnahGuidanceItem?) {
        _uiState.update { it.copy(selectedSunnahItem = item) }
    }

    fun getFilteredPrayerItems(state: SharedGuidanceUiState = _uiState.value): List<PrayerGuidanceItem> {
        val query = state.searchQuery.trim().lowercase()
        return state.prayerItems.filter { item ->
            val matchesCategory = state.prayerCategoryFilter == null || item.category == state.prayerCategoryFilter
            val matchesSearch = query.isEmpty() ||
                item.nameEn.lowercase().contains(query) ||
                item.nameId.lowercase().contains(query) ||
                item.nameArabic.contains(query) ||
                item.descriptionEn.lowercase().contains(query)
            matchesCategory && matchesSearch
        }
    }

    fun getFilteredSunnahItems(state: SharedGuidanceUiState = _uiState.value): List<SunnahGuidanceItem> {
        val query = state.searchQuery.trim().lowercase()
        return state.sunnahItems.filter { item ->
            val matchesCategory = state.sunnahCategoryFilter == null || item.category == state.sunnahCategoryFilter
            val matchesSearch = query.isEmpty() ||
                item.titleEn.lowercase().contains(query) ||
                item.titleId.lowercase().contains(query) ||
                item.titleArabic.contains(query) ||
                item.summaryEn.lowercase().contains(query)
            matchesCategory && matchesSearch
        }
    }
}
