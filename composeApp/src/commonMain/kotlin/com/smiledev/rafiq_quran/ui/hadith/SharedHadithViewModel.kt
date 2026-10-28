package com.smiledev.rafiq_quran.ui.hadith

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.smiledev.rafiq_quran.domain.model.Hadith
import com.smiledev.rafiq_quran.domain.model.HadithBook
import com.smiledev.rafiq_quran.domain.model.HadithTopic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val DEFAULT_HADITH_BOOKS = listOf(
    HadithBook(
        id = "bukhari",
        collection = "Sahih al-Bukhari",
        number = 1,
        nameAr = "صحيح البخاري",
        nameEn = "Sahih al-Bukhari",
        nameId = "Shahih Al-Bukhari"
    ),
    HadithBook(
        id = "muslim",
        collection = "Sahih Muslim",
        number = 2,
        nameAr = "صحيح مسلم",
        nameEn = "Sahih Muslim",
        nameId = "Shahih Muslim"
    ),
    HadithBook(
        id = "nawawi40",
        collection = "40 Hadith Nawawi",
        number = 3,
        nameAr = "الأربعون النووية",
        nameEn = "An-Nawawi's 40 Hadith",
        nameId = "Hadits Arbain An-Nawawi"
    ),
    HadithBook(
        id = "abudawood",
        collection = "Sunan Abi Dawud",
        number = 4,
        nameAr = "سنن أبي داود",
        nameEn = "Sunan Abi Dawud",
        nameId = "Sunan Abu Daud"
    ),
    HadithBook(
        id = "tirmidhi",
        collection = "Jami` at-Tirmidhi",
        number = 5,
        nameAr = "جامع الترمذي",
        nameEn = "Jami` at-Tirmidhi",
        nameId = "Jami' At-Tirmidzi"
    ),
    HadithBook(
        id = "nasai",
        collection = "Sunan an-Nasa'i",
        number = 6,
        nameAr = "سنن النسائي",
        nameEn = "Sunan an-Nasa'i",
        nameId = "Sunan An-Nasa'i"
    ),
    HadithBook(
        id = "ibnmajah",
        collection = "Sunan Ibn Majah",
        number = 7,
        nameAr = "سنن ابن ماجه",
        nameEn = "Sunan Ibn Majah",
        nameId = "Sunan Ibnu Majah"
    ),
    HadithBook(
        id = "malik",
        collection = "Muwatta Malik",
        number = 8,
        nameAr = "موطأ مالك",
        nameEn = "Muwatta Malik",
        nameId = "Muwatha' Imam Malik"
    )
)

val DEFAULT_HADITH_TOPICS = listOf(
    HadithTopic(
        id = "faith",
        nameAr = "الإيمان",
        nameEn = "Faith & Sincerity",
        nameId = "Iman & Keikhlasan",
        bookIds = listOf("bukhari", "muslim", "nawawi40")
    ),
    HadithTopic(
        id = "character",
        nameAr = "حسن الخلق",
        nameEn = "Good Character & Manners",
        nameId = "Akhlak Mulia & Adab",
        bookIds = listOf("bukhari", "tirmidhi", "nawawi40")
    ),
    HadithTopic(
        id = "prayer",
        nameAr = "الصلاة",
        nameEn = "Daily Prayers (Salah)",
        nameId = "Shalat & Kewajibannya",
        bookIds = listOf("bukhari", "muslim", "abudawood")
    ),
    HadithTopic(
        id = "charity",
        nameAr = "الصدقة والزكاة",
        nameEn = "Charity & Generosity",
        nameId = "Sedekah & Kepedulian",
        bookIds = listOf("muslim", "tirmidhi")
    ),
    HadithTopic(
        id = "patience",
        nameAr = "الصبر والتوكل",
        nameEn = "Patience & Trust in Allah",
        nameId = "Sabar & Tawakal",
        bookIds = listOf("bukhari", "muslim")
    )
)

val DEFAULT_HADITHS = listOf(
    Hadith(
        id = 1,
        bookId = "nawawi40",
        inBookNumber = 1,
        narratorAr = "أمير المؤمنين عمر بن الخطاب رضي الله عنه",
        narratorEn = "Umar ibn al-Khattab (RA)",
        textAr = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
        textEn = "Actions are according to intentions, and everyone will get what was intended.",
        textId = "Sesungguhnya setiap amal perbuatan tergantung pada niatnya, dan setiap orang akan mendapatkan sesuai apa yang ia niatkan."
    ),
    Hadith(
        id = 2,
        bookId = "bukhari",
        inBookNumber = 13,
        narratorAr = "أنس رضي الله عنه",
        narratorEn = "Anas ibn Malik (RA)",
        textAr = "لاَ يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
        textEn = "None of you truly believes until he loves for his brother what he loves for himself.",
        textId = "Tidak sempurna iman salah seorang di antara kalian hingga mencintai untuk saudaranya apa yang ia cintai untuk dirinya sendiri."
    ),
    Hadith(
        id = 3,
        bookId = "muslim",
        inBookNumber = 223,
        narratorAr = "أبو مالك الأشعري رضي الله عنه",
        narratorEn = "Abu Malik al-Ash'ari (RA)",
        textAr = "الطُّهُورُ شَطْرُ الإِيمَانِ، وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ",
        textEn = "Purification is half of faith, and Alhamdulillah fills the scale.",
        textId = "Kebersihan dan bersuci adalah separuh dari keimanan, dan Alhamdulillah memenuhi timbangan kebaikan."
    ),
    Hadith(
        id = 4,
        bookId = "tirmidhi",
        inBookNumber = 1956,
        narratorAr = "أبو الدرداء رضي الله عنه",
        narratorEn = "Abu ad-Darda (RA)",
        textAr = "مَا مِنْ شَيْءٍ أَثْقَلُ فِي مِيزَانِ الْمُؤْمِنِ يَوْمَ الْقِيَامَةِ مِنْ حُسْنِ الْخُلُقِ",
        textEn = "Nothing is heavier upon the scale of a believer on the Day of Resurrection than good character.",
        textId = "Tidak ada sesuatu yang lebih berat dalam timbangan seorang mukmin pada hari kiamat daripada akhlak yang mulia."
    ),
    Hadith(
        id = 5,
        bookId = "bukhari",
        inBookNumber = 6023,
        narratorAr = "أبو هريرة رضي الله عنه",
        narratorEn = "Abu Hurairah (RA)",
        textAr = "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
        textEn = "Whoever believes in Allah and the Last Day should speak good or remain silent.",
        textId = "Barangsiapa beriman kepada Allah dan Hari Akhir, hendaklah ia berkata yang baik atau diam."
    ),
    Hadith(
        id = 6,
        bookId = "bukhari",
        inBookNumber = 5354,
        narratorAr = "سهل بن سعد رضي الله عنه",
        narratorEn = "Sahl ibn Sa'd (RA)",
        textAr = "أَنَا وَكَافِلُ الْيَتِيمِ فِي الْجَنَّةِ هَكَذَا، وَأَشَارَ بِالسَّبَّابَةِ وَالْوُسْطَى",
        textEn = "I and the person who looks after an orphan will be in Paradise like this, putting his index and middle fingers together.",
        textId = "Aku dan orang yang memelihara anak yatim di surga bagaikan ini, seraya beliau mengisyaratkan jari telunjuk dan tengahnya."
    )
)

@Immutable
data class SharedHadithUiState(
    val books: List<HadithBook> = DEFAULT_HADITH_BOOKS,
    val topics: List<HadithTopic> = DEFAULT_HADITH_TOPICS,
    val hadiths: List<Hadith> = DEFAULT_HADITHS,
    val selectedBook: HadithBook? = null,
    val selectedTopic: HadithTopic? = null,
    val selectedHadith: Hadith? = null,
    val hadithOfTheDay: Hadith = DEFAULT_HADITHS.first(),
    val searchQuery: String = "",
    val favoriteIds: Set<Int> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    val fontSize: Int = 18
)

class SharedHadithViewModel(
    initialBooks: List<HadithBook> = DEFAULT_HADITH_BOOKS,
    initialTopics: List<HadithTopic> = DEFAULT_HADITH_TOPICS,
    initialHadiths: List<Hadith> = DEFAULT_HADITHS
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SharedHadithUiState(
            books = initialBooks,
            topics = initialTopics,
            hadiths = initialHadiths,
            hadithOfTheDay = initialHadiths.firstOrNull() ?: DEFAULT_HADITHS.first()
        )
    )
    val uiState: StateFlow<SharedHadithUiState> = _uiState.asStateFlow()

    fun selectBook(book: HadithBook?) {
        _uiState.update { s ->
            s.copy(
                selectedBook = if (s.selectedBook?.id == book?.id) null else book,
                selectedTopic = null
            )
        }
    }

    fun selectTopic(topic: HadithTopic?) {
        _uiState.update { s ->
            s.copy(
                selectedTopic = if (s.selectedTopic?.id == topic?.id) null else topic,
                selectedBook = null
            )
        }
    }

    fun selectHadith(hadith: Hadith?) {
        _uiState.update { it.copy(selectedHadith = hadith) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun toggleFavorite(hadithId: Int) {
        _uiState.update { s ->
            val updated = if (hadithId in s.favoriteIds) {
                s.favoriteIds - hadithId
            } else {
                s.favoriteIds + hadithId
            }
            s.copy(favoriteIds = updated)
        }
    }

    fun setFontSize(size: Int) {
        val clamped = size.coerceIn(14, 28)
        _uiState.update { it.copy(fontSize = clamped) }
    }

    fun getFilteredHadiths(state: SharedHadithUiState): List<Hadith> {
        var result = state.hadiths

        if (state.showFavoritesOnly) {
            result = result.filter { it.id in state.favoriteIds }
        }

        state.selectedBook?.let { book ->
            result = result.filter { it.bookId.equals(book.id, ignoreCase = true) }
        }

        state.selectedTopic?.let { topic ->
            result = result.filter { hadith ->
                topic.bookIds.any { it.equals(hadith.bookId, ignoreCase = true) }
            }
        }

        val query = state.searchQuery.trim().lowercase()
        if (query.isNotEmpty()) {
            result = result.filter { h ->
                h.textEn.lowercase().contains(query) ||
                h.textId.lowercase().contains(query) ||
                h.textAr.contains(query) ||
                (h.narratorEn?.lowercase()?.contains(query) == true) ||
                (h.narratorAr?.contains(query) == true)
            }
        }

        return result
    }
}
