package com.smiledev.rafiq_quran.ui.quran

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.domain.usecase.GetAyahsWithTranslationUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetSurahsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val DEFAULT_SHARED_SURAHS = listOf(
    Surah(id = 1, chapterNumber = 1, nameArabic = "الفاتحة", nameSimple = "Al-Fatihah", translatedName = "The Opening", versesCount = 7, revelationPlace = "makkah"),
    Surah(id = 2, chapterNumber = 2, nameArabic = "البقرة", nameSimple = "Al-Baqarah", translatedName = "The Cow", versesCount = 286, revelationPlace = "madinah"),
    Surah(id = 3, chapterNumber = 3, nameArabic = "آل عمران", nameSimple = "Ali 'Imran", translatedName = "Family of Imran", versesCount = 200, revelationPlace = "madinah"),
    Surah(id = 4, chapterNumber = 4, nameArabic = "النساء", nameSimple = "An-Nisa", translatedName = "The Women", versesCount = 176, revelationPlace = "madinah"),
    Surah(id = 18, chapterNumber = 18, nameArabic = "الكهف", nameSimple = "Al-Kahf", translatedName = "The Cave", versesCount = 110, revelationPlace = "makkah"),
    Surah(id = 36, chapterNumber = 36, nameArabic = "يس", nameSimple = "Ya-Sin", translatedName = "Ya-Sin", versesCount = 83, revelationPlace = "makkah"),
    Surah(id = 55, chapterNumber = 55, nameArabic = "الرحمن", nameSimple = "Ar-Rahman", translatedName = "The Beneficent", versesCount = 78, revelationPlace = "madinah"),
    Surah(id = 56, chapterNumber = 56, nameArabic = "الواقعة", nameSimple = "Al-Waqi'ah", translatedName = "The Inevitable", versesCount = 96, revelationPlace = "makkah"),
    Surah(id = 67, chapterNumber = 67, nameArabic = "الملك", nameSimple = "Al-Mulk", translatedName = "The Sovereignty", versesCount = 30, revelationPlace = "makkah"),
    Surah(id = 112, chapterNumber = 112, nameArabic = "الإخلاص", nameSimple = "Al-Ikhlas", translatedName = "Sincerity", versesCount = 4, revelationPlace = "makkah"),
    Surah(id = 113, chapterNumber = 113, nameArabic = "الفلق", nameSimple = "Al-Falaq", translatedName = "The Daybreak", versesCount = 5, revelationPlace = "makkah"),
    Surah(id = 114, chapterNumber = 114, nameArabic = "الناس", nameSimple = "An-Nas", translatedName = "Mankind", versesCount = 6, revelationPlace = "makkah")
)

val DEFAULT_SHARED_AYAHS_AL_FATIHAH = listOf(
    Ayah(sura = 1, aya = 1, text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", bismillah = null, translationEn = "In the name of Allah, the Entirely Merciful, the Especially Merciful.", translationId = "Dengan menyebut nama Allah Yang Maha Pengasih lagi Maha Penyayang.", page = 1, juz = 1),
    Ayah(sura = 1, aya = 2, text = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", bismillah = null, translationEn = "[All] praise is [due] to Allah, Lord of the worlds -", translationId = "Segala puji bagi Allah, Tuhan semesta alam.", page = 1, juz = 1),
    Ayah(sura = 1, aya = 3, text = "الرَّحْمَٰنِ الرَّحِيمِ", bismillah = null, translationEn = "The Entirely Merciful, the Especially Merciful,", translationId = "Maha Pengasih lagi Maha Penyayang.", page = 1, juz = 1),
    Ayah(sura = 1, aya = 4, text = "مَالِكِ يَوْمِ الدِّينِ", bismillah = null, translationEn = "Sovereign of the Day of Recompense.", translationId = "Pemilik hari pembalasan.", page = 1, juz = 1),
    Ayah(sura = 1, aya = 5, text = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", bismillah = null, translationEn = "It is You we worship and You we ask for help.", translationId = "Hanya kepada Engkaulah kami menyembah dan hanya kepada Engkaulah kami memohon pertolongan.", page = 1, juz = 1),
    Ayah(sura = 1, aya = 6, text = "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", bismillah = null, translationEn = "Guide us to the straight path -", translationId = "Tunjukilah kami jalan yang lurus,", page = 1, juz = 1),
    Ayah(sura = 1, aya = 7, text = "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", bismillah = null, translationEn = "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.", translationId = "(yaitu) jalan orang-orang yang telah Engkau beri nikmat kepadanya; bukan (jalan) mereka yang dimurkai, dan bukan (pula jalan) mereka yang sesat.", page = 1, juz = 1)
)

val DEFAULT_SHARED_AYAHS_AL_IKHLAS = listOf(
    Ayah(sura = 112, aya = 1, text = "قُلْ هُوَ اللَّهُ أَحَدٌ", bismillah = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", translationEn = "Say, \"He is Allah, [who is] One,", translationId = "Katakanlah (Muhammad), \"Dialah Allah, Yang Maha Esa.\"", page = 604, juz = 30),
    Ayah(sura = 112, aya = 2, text = "اللَّهُ الصَّمَدُ", bismillah = null, translationEn = "Allah, the Eternal Refuge.", translationId = "Allah tempat meminta segala sesuatu.", page = 604, juz = 30),
    Ayah(sura = 112, aya = 3, text = "لَمْ يَلِدْ وَلَمْ يُولَدْ", bismillah = null, translationEn = "He neither begets nor is born,", translationId = "(Allah) tidak beranak dan tidak pula diperanakkan,", page = 604, juz = 30),
    Ayah(sura = 112, aya = 4, text = "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", bismillah = null, translationEn = "Nor is there to Him any equivalent.\"", translationId = "dan tidak ada sesuatu yang setara dengan Dia.\"", page = 604, juz = 30)
)

@Immutable
data class SharedQuranUiState(
    val surahs: List<Surah> = DEFAULT_SHARED_SURAHS,
    val selectedSurah: Surah? = null,
    val ayahs: List<Ayah> = emptyList(),
    val searchQuery: String = "",
    val revelationFilter: String? = null, // "makkah" or "madinah"
    val showTranslation: Boolean = true,
    val arabicFontSizeSp: Int = 26,
    val translationFontSizeSp: Int = 14,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

class SharedQuranViewModel(
    private val getSurahsUseCase: GetSurahsUseCase? = null,
    private val getAyahsWithTranslationUseCase: GetAyahsWithTranslationUseCase? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedQuranUiState())
    val uiState: StateFlow<SharedQuranUiState> = _uiState.asStateFlow()

    init {
        loadSurahs()
    }

    fun loadSurahs() {
        if (getSurahsUseCase == null) return
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getSurahsUseCase("en")) {
                is Result.Success -> {
                    val list = if (result.data.isNotEmpty()) result.data else DEFAULT_SHARED_SURAHS
                    _uiState.update { it.copy(surahs = list, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
            }
        }
    }

    fun selectSurah(surah: Surah?) {
        if (surah == null) {
            _uiState.update { it.copy(selectedSurah = null, ayahs = emptyList()) }
            return
        }

        val fallback = fallbackAyahsFor(surah.chapterNumber)
        _uiState.update {
            it.copy(
                selectedSurah = surah,
                ayahs = fallback,
                isLoading = getAyahsWithTranslationUseCase != null,
                error = null
            )
        }

        if (getAyahsWithTranslationUseCase != null) {
            viewModelScope.launch(dispatcherProvider.io) {
                when (val result = getAyahsWithTranslationUseCase(surah.chapterNumber, "en")) {
                    is Result.Success -> {
                        val ayahs = if (result.data.isNotEmpty()) result.data else fallback
                        _uiState.update { it.copy(ayahs = ayahs, isLoading = false) }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(ayahs = fallback, isLoading = false) }
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectRevelationFilter(filter: String?) {
        _uiState.update { it.copy(revelationFilter = filter) }
    }

    fun toggleTranslation() {
        _uiState.update { it.copy(showTranslation = !it.showTranslation) }
    }

    fun increaseArabicFontSize() {
        _uiState.update { it.copy(arabicFontSizeSp = (it.arabicFontSizeSp + 2).coerceAtMost(40)) }
    }

    fun decreaseArabicFontSize() {
        _uiState.update { it.copy(arabicFontSizeSp = (it.arabicFontSizeSp - 2).coerceAtLeast(18)) }
    }

    fun getFilteredSurahs(state: SharedQuranUiState = _uiState.value): List<Surah> {
        val query = state.searchQuery.trim().lowercase()
        return state.surahs.filter { surah ->
            val matchesFilter = state.revelationFilter == null || surah.revelationPlace.equals(state.revelationFilter, ignoreCase = true)
            val matchesSearch = query.isEmpty() ||
                surah.nameSimple.lowercase().contains(query) ||
                surah.translatedName.lowercase().contains(query) ||
                surah.nameArabic.contains(query) ||
                surah.chapterNumber.toString() == query
            matchesFilter && matchesSearch
        }
    }

    private fun fallbackAyahsFor(chapterNumber: Int): List<Ayah> {
        return when (chapterNumber) {
            1 -> DEFAULT_SHARED_AYAHS_AL_FATIHAH
            112 -> DEFAULT_SHARED_AYAHS_AL_IKHLAS
            else -> listOf(
                Ayah(
                    sura = chapterNumber,
                    aya = 1,
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    bismillah = null,
                    translationEn = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                    translationId = "Dengan menyebut nama Allah Yang Maha Pengasih lagi Maha Penyayang.",
                    page = 1,
                    juz = 1
                )
            )
        }
    }
}
