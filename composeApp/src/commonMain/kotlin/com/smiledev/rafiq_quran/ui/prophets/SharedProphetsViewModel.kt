package com.smiledev.rafiq_quran.ui.prophets

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.ProphetStory
import com.smiledev.rafiq_quran.domain.model.VerseRef
import com.smiledev.rafiq_quran.domain.usecase.GetProphetsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val DEFAULT_SHARED_PROPHETS = listOf(
    ProphetStory(
        id = 1,
        nameArabic = "آدَمُ",
        nameEn = "Adam",
        nameId = "Adam",
        summaryEn = "The first human being and first prophet created by Allah.",
        summaryId = "Manusia pertama dan nabi pertama yang diciptakan oleh Allah.",
        storyEn = "Allah created Adam from clay and taught him the names of all things, placing him and Hawwa in Paradise before descending to Earth.",
        storyId = "Allah menciptakan Adam dari tanah dan mengajarkan kepadanya nama-nama segala sesuatu, menempatkannya di Surga sebelum diturunkan ke Bumi.",
        miraclesEn = "Direct creation by Allah, extensive divine knowledge of all created names.",
        miraclesId = "Penciptaan langsung oleh Allah, pengetahuan nama-nama seluruh ciptaan.",
        eraEn = "Beginning of mankind",
        eraId = "Awal mula umat manusia",
        peopleEn = "First human family",
        peopleId = "Keluarga manusia pertama",
        lifespanEn = "Approx. 960-1000 years",
        lifespanId = "Sekitar 960-1000 tahun",
        lessonsEn = listOf("Sincere repentance is accepted by Allah", "Humility before divine command", "Avoid arrogance and whispers of evil"),
        lessonsId = listOf("Tobat yang tulus diterima Allah", "Ketundukan pada perintah ilahi", "Menjauhi kesombongan dan godaan iblis"),
        verses = listOf(VerseRef(surah = 2, surahNameEn = "Al-Baqarah", surahNameId = "Al-Baqarah", ayahStart = 30, ayahEnd = 39))
    ),
    ProphetStory(
        id = 2,
        nameArabic = "نُوحٌ",
        nameEn = "Nuh (Noah)",
        nameId = "Nuh",
        summaryEn = "Preached patience and monotheism for centuries, built the Great Ark.",
        summaryId = "Berdakwah dengan sabar selama berabad-abad, membangun bahtera besar.",
        storyEn = "Prophet Nuh called his people to worship Allah alone for 950 years. When they persisted in rebellion, Allah commanded him to build an ark before the great flood.",
        storyId = "Nabi Nuh menyeru kaumnya menyembah Allah semata selama 950 tahun. Ketika kaumnya tetap membangkang, Allah memerintahkannya membangun bahtera sebelum banjir besar.",
        miraclesEn = "The Great Ark carrying the believers and pairs of creatures to safety.",
        miraclesId = "Bahtera penyelamat yang membawa orang-orang beriman dan pasangan makhluk hidup.",
        eraEn = "Pre-flood era",
        eraId = "Zaman sebelum banjir besar",
        peopleEn = "People of Nuh (Mesopotamia)",
        peopleId = "Kaum Nuh (Mesopotamia)",
        lifespanEn = "Over 950 years of mission",
        lifespanId = "Lebih dari 950 tahun masa dakwah",
        lessonsEn = listOf("Steadfast patience in preaching truth", "Kinship does not substitute personal faith", "Divine help arrives for the faithful"),
        lessonsId = listOf("Kesabaran tak berbatas dalam dakwah", "Nasab keluarga tidak menggantikan iman", "Pertolongan Allah pasti tiba bagi orang beriman"),
        verses = listOf(VerseRef(surah = 71, surahNameEn = "Nuh", surahNameId = "Nuh", ayahStart = 1, ayahEnd = 28))
    ),
    ProphetStory(
        id = 3,
        nameArabic = "إِبْرَاهِيمُ",
        nameEn = "Ibrahim (Abraham)",
        nameId = "Ibrahim",
        summaryEn = "The intimate friend of Allah (Khalilullah) and father of prophets.",
        summaryId = "Kekasih Allah (Khalilullah) dan bapak para nabi.",
        storyEn = "Prophet Ibrahim demolished idols, survived Nimrod's blazing fire unharmed, and with his son Ismail rebuilt the sacred Ka'bah in Makkah.",
        storyId = "Nabi Ibrahim menghancurkan berhala, selamat dari api unggun Raja Namrud tanpa terbakar, serta bersama Ismail membangun kembali Ka'bah suci di Makkah.",
        miraclesEn = "Fire rendered cool and peaceful, the flourishing valley of Makkah, water of Zamzam.",
        miraclesId = "Api menjadi dingin dan menyelamatkan, lembah Makkah yang tandus menjadi berkah, sumber air Zamzam.",
        eraEn = "Approx. 2000 BCE",
        eraId = "Sekitar 2000 SM",
        peopleEn = "People of Babylon & Canaan",
        peopleId = "Kaum Babilonia & Kanaan",
        lifespanEn = "Approx. 175 years",
        lifespanId = "Sekitar 175 tahun",
        lessonsEn = listOf("Pure monotheism without compromise", "Total surrender to Allah's wisdom", "Exemplary father and builder"),
        lessonsId = listOf("Ketauhidan murni tanpa kompromi", "Kepasrahan total pada kehendak Allah", "Teladan seorang ayah dan pembangun peradaban"),
        verses = listOf(VerseRef(surah = 14, surahNameEn = "Ibrahim", surahNameId = "Ibrahim", ayahStart = 35, ayahEnd = 41))
    ),
    ProphetStory(
        id = 4,
        nameArabic = "مُوسَىٰ",
        nameEn = "Musa (Moses)",
        nameId = "Musa",
        summaryEn = "Spoke directly with Allah (Kalimullah), delivered the Israelites from Pharaoh.",
        summaryId = "Berbicara langsung dengan Allah (Kalimullah), membebaskan Bani Israil dari Firaun.",
        storyEn = "Raised in Pharaoh's palace, called by Allah at Mount Sinai, confronted Pharaoh with miracles, and parted the Red Sea.",
        storyId = "Tumbuh di istana Firaun, dipanggil Allah di Bukit Sinai, menghadapi Firaun dengan mukjizat, dan membelah Laut Merah.",
        miraclesEn = "The miraculous staff, radiant luminous hand, parting of the Red Sea, descent of Manna and Salwa.",
        miraclesId = "Tongkat mukjizat, tangan yang bersinar putih, terbelahnya Laut Merah, turunnya Manna dan Salwa.",
        eraEn = "Approx. 13th Century BCE",
        eraId = "Sekitar Abad ke-13 SM",
        peopleEn = "Children of Israel & Pharaoh of Egypt",
        peopleId = "Bani Israil & Firaun Mesir",
        lifespanEn = "Approx. 120 years",
        lifespanId = "Sekitar 120 tahun",
        lessonsEn = listOf("Courage against ruthless tyranny", "Trust in divine guidance when trapped", "Vigilance in preserving worship"),
        lessonsId = listOf("Keberanian melawan kelaliman tirani", "Tawakal saat jalan buntu menghadang", "Kesiapsiagaan menjaga kemurnian syariat"),
        verses = listOf(VerseRef(surah = 20, surahNameEn = "Taha", surahNameId = "Taha", ayahStart = 9, ayahEnd = 98))
    ),
    ProphetStory(
        id = 5,
        nameArabic = "عِيسَىٰ",
        nameEn = "Isa (Jesus)",
        nameId = "Isa",
        summaryEn = "Born miraculously of Maryam, word and spirit from Allah to the Israelites.",
        summaryId = "Lahir secara mukjizat dari Maryam, kalimat dan ruh dari Allah untuk Bani Israil.",
        storyEn = "Prophet Isa spoke as an infant, healed the blind and lepers, raised the dead by Allah's permission, and was raised unto the heavens.",
        storyId = "Nabi Isa berbicara saat masih bayi di buaian, menyembuhkan orang buta dan penderita kusta, menghidupkan orang mati dengan izin Allah, dan diangkat ke langit.",
        miraclesEn = "Virgin birth, speaking in the cradle, curing leprosy and blindness, breathing life into clay birds by Allah's leave.",
        miraclesId = "Kelahiran tanpa ayah, berbicara di buaian bayi, menyembuhkan penyakit kusta dan buta, meniupkan burung tanah menjadi hidup dengan izin Allah.",
        eraEn = "1st Century CE",
        eraId = "Abad ke-1 M",
        peopleEn = "Children of Israel (Palestine)",
        peopleId = "Bani Israil (Palestina)",
        lifespanEn = "Raised at age 33",
        lifespanId = "Diangkat ke langit usia 33 tahun",
        lessonsEn = listOf("Asceticism and compassion", "Reforming inner spiritual hypocrisy", "Confirmation of previous scriptures"),
        lessonsId = listOf("Kesederhanaan hidup dan welas asih", "Memperbaiki kemunafikan hati", "Membenarkan kitab-kitab suci sebelumnya"),
        verses = listOf(VerseRef(surah = 3, surahNameEn = "Ali 'Imran", surahNameId = "Ali 'Imran", ayahStart = 45, ayahEnd = 59))
    ),
    ProphetStory(
        id = 6,
        nameArabic = "مُحَمَّدٌ ﷺ",
        nameEn = "Muhammad ﷺ",
        nameId = "Muhammad ﷺ",
        summaryEn = "The final seal of all prophets, mercy to all the worlds.",
        summaryId = "Penutup para nabi dan rasul, rahmat bagi semesta alam.",
        storyEn = "Received the Noble Quran in the cave of Hira, transformed Arabia with justice and mercy, established the global community of believers.",
        storyId = "Menerima Al-Qur'an mulia di Gua Hira, mengubah jazirah Arab dengan keadilan dan kasih sayang, menegakkan peradaban umat beriman.",
        miraclesEn = "The Holy Quran, the Night Journey (Isra' and Mi'raj), splitting of the moon.",
        miraclesId = "Al-Qur'an Al-Karim, peristiwa Isra' dan Mi'raj, terbelahnya bulan.",
        eraEn = "570 - 632 CE",
        eraId = "570 - 632 M",
        peopleEn = "All of humanity",
        peopleId = "Seluruh umat manusia",
        lifespanEn = "63 years",
        lifespanId = "63 tahun",
        lessonsEn = listOf("Sublime character and universal mercy", "Patience in adversity and generosity in victory", "Enduring guidance until the Last Day"),
        lessonsId = listOf("Akhlak mulia dan kasih sayang semesta", "Sabar dalam kesukaran dan pemaaf dalam kejayaan", "Pedoman hidup abadi hingga akhir zaman"),
        verses = listOf(VerseRef(surah = 33, surahNameEn = "Al-Ahzab", surahNameId = "Al-Ahzab", ayahStart = 40, ayahEnd = 48))
    )
)

@Immutable
data class SharedProphetsUiState(
    val prophets: List<ProphetStory> = DEFAULT_SHARED_PROPHETS,
    val selectedProphet: ProphetStory? = null,
    val searchQuery: String = "",
    val favoriteIds: Set<Int> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    val storyFontSize: Int = 16,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

class SharedProphetsViewModel(
    private val getProphetsUseCase: GetProphetsUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedProphetsUiState())
    val uiState: StateFlow<SharedProphetsUiState> = _uiState.asStateFlow()

    init {
        loadProphets()
    }

    fun loadProphets() {
        if (getProphetsUseCase == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getProphetsUseCase.invoke()) {
                is Result.Success -> {
                    val list = if (result.data.isNotEmpty()) result.data else DEFAULT_SHARED_PROPHETS
                    _uiState.update { it.copy(prophets = list, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun selectProphet(prophet: ProphetStory?) {
        _uiState.update { it.copy(selectedProphet = prophet) }
    }

    fun toggleFavorite(id: Int) {
        _uiState.update { state ->
            val updated = if (id in state.favoriteIds) {
                state.favoriteIds - id
            } else {
                state.favoriteIds + id
            }
            state.copy(favoriteIds = updated)
        }
    }

    fun setStoryFontSize(size: Int) {
        _uiState.update { it.copy(storyFontSize = size.coerceIn(12, 28)) }
    }

    fun getFilteredProphets(state: SharedProphetsUiState = _uiState.value): List<ProphetStory> {
        val query = state.searchQuery.trim().lowercase()
        return state.prophets.filter { item ->
            val matchesFavorites = !state.showFavoritesOnly || item.id in state.favoriteIds
            val matchesSearch = query.isEmpty() ||
                item.nameEn.lowercase().contains(query) ||
                item.nameId.lowercase().contains(query) ||
                item.nameArabic.contains(query) ||
                item.id.toString() == query

            matchesFavorites && matchesSearch
        }
    }
}
