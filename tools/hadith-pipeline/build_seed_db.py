"""Generate the seed hadith.db committed to app assets.

Schema is the source of truth for Plan A. Plan B extends this generator with the
full corpus. Run:  python build_seed_db.py
Output: app/src/main/assets/quran-data/hadiths/hadith.db
"""
import os
import sqlite3

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_PATH = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "quran-data", "hadiths", "hadith.db")

BOOKS = [
    {
        "id": "bukhari.1",
        "collection": "bukhari",
        "number": 1,
        "name_ar": "كتاب بدء الوحي",
        "name_en": "Revelation",
        "name_id": "Permulaan Wahyu",
    },
    {
        "id": "muslim.1",
        "collection": "muslim",
        "number": 1,
        "name_ar": "كتاب الإيمان",
        "name_en": "Faith",
        "name_id": "Iman",
    },
]

# Seed hadith, one tuple per hadith:
#   (book_id, in_book_number, narrator_ar, narrator_en, text_ar, text_en, text_id)
#  - bukhari.1/1 <- sunnah.com/bukhari:1  (Sahih al-Bukhari 1)
#  - bukhari.1/2 <- sunnah.com/bukhari:2  (Sahih al-Bukhari 2)
#  - muslim.1/1  <- sunnah.com/muslim:1   (Sahih Muslim 8a)
#  - muslim.1/2  <- sunnah.com/muslim:2   (Sahih Muslim 8b)
# Arabic + English copied verbatim from sunnah.com; Indonesian from the
# irsyadulibad/hadits-database dump (MIT licensed) matching the same hadith.
HADITH = [
    ('bukhari.1', 1,
     '',
     "Narrated 'Umar bin Al-Khattab:",
     'حَدَّثَنَا الْحُمَيْدِيُّ عَبْدُ اللَّهِ بْنُ الزُّبَيْرِ، قَالَ حَدَّثَنَا سُفْيَانُ، قَالَ حَدَّثَنَا يَحْيَى بْنُ سَعِيدٍ الأَنْصَارِيُّ، قَالَ أَخْبَرَنِي مُحَمَّدُ بْنُ إِبْرَاهِيمَ التَّيْمِيُّ، أَنَّهُ سَمِعَ عَلْقَمَةَ بْنَ وَقَّاصٍ اللَّيْثِيَّ، يَقُولُ سَمِعْتُ عُمَرَ بْنَ الْخَطَّابِ ـ رضى الله عنه ـ عَلَى الْمِنْبَرِ قَالَ سَمِعْتُ رَسُولَ اللَّهِ صلى الله عليه وسلم يَقُولُ \u200f"\u200f إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى، فَمَنْ كَانَتْ هِجْرَتُهُ إِلَى دُنْيَا يُصِيبُهَا أَوْ إِلَى امْرَأَةٍ يَنْكِحُهَا فَهِجْرَتُهُ إِلَى مَا هَاجَرَ إِلَيْهِ \u200f"\u200f\u200f.\u200f',
     'Narrated \'Umar bin Al-Khattab: I heard Allah\'s Messenger (ﷺ) saying, "The reward of deeds depends upon the intentions and every person will get the reward according to what he has intended. So whoever emigrated for worldly benefits or for a woman to marry, his emigration was for what he emigrated for."',
     'Telah menceritakan kepada kami [Al Humaidi Abdullah bin Az Zubair] dia berkata, Telah menceritakan kepada kami [Sufyan] yang berkata, bahwa Telah menceritakan kepada kami [Yahya bin Sa\'id Al Anshari] berkata, telah mengabarkan kepada kami [Muhammad bin Ibrahim At Taimi], bahwa dia pernah mendengar [Alqamah bin Waqash Al Laitsi] berkata; saya pernah mendengar [Umar bin Al Khaththab] diatas mimbar berkata; saya mendengar Rasulullah shallallahu \'alaihi wasallam bersabda: "Semua perbuatan tergantung niatnya, dan (balasan) bagi tiap-tiap orang (tergantung) apa yang diniatkan; Barangsiapa niat hijrahnya karena dunia yang ingin digapainya atau karena seorang perempuan yang ingin dinikahinya, maka hijrahnya adalah kepada apa dia diniatkan"'),
    ('bukhari.1', 2,
     '',
     "Narrated 'Aisha:",
     'حَدَّثَنَا عَبْدُ اللَّهِ بْنُ يُوسُفَ، قَالَ أَخْبَرَنَا مَالِكٌ، عَنْ هِشَامِ بْنِ عُرْوَةَ، عَنْ أَبِيهِ، عَنْ عَائِشَةَ أُمِّ الْمُؤْمِنِينَ ـ رضى الله عنها ـ أَنَّ الْحَارِثَ بْنَ هِشَامٍ ـ رضى الله عنه ـ سَأَلَ رَسُولَ اللَّهِ صلى الله عليه وسلم فَقَالَ يَا رَسُولَ اللَّهِ كَيْفَ يَأْتِيكَ الْوَحْىُ فَقَالَ رَسُولُ اللَّهِ صلى الله عليه وسلم \u200f"\u200f أَحْيَانًا يَأْتِينِي مِثْلَ صَلْصَلَةِ الْجَرَسِ ـ وَهُوَ أَشَدُّهُ عَلَىَّ ـ فَيُفْصَمُ عَنِّي وَقَدْ وَعَيْتُ عَنْهُ مَا قَالَ، وَأَحْيَانًا يَتَمَثَّلُ لِيَ الْمَلَكُ رَجُلاً فَيُكَلِّمُنِي فَأَعِي مَا يَقُولُ \u200f" \u200f\u200f.\u200f قَالَتْ عَائِشَةُ رضى الله عنها وَلَقَدْ رَأَيْتُهُ يَنْزِلُ عَلَيْهِ الْوَحْىُ فِي الْيَوْمِ الشَّدِيدِ الْبَرْدِ، فَيَفْصِمُ عَنْهُ وَإِنَّ جَبِينَهُ لَيَتَفَصَّدُ عَرَقًا\u200f.\u200f',
     'Narrated \'Aisha: (the mother of the faithful believers) Al-Harith bin Hisham asked Allah\'s Messenger (ﷺ) "O Allah\'s Messenger (ﷺ)! How is the Divine Inspiration revealed to you?" Allah\'s Messenger (ﷺ) replied, "Sometimes it is (revealed) like the ringing of a bell, this form of Inspiration is the hardest of all and then this state passes off after I have grasped what is inspired. Sometimes the Angel comes in the form of a man and talks to me and I grasp whatever he says." \'Aisha added: Verily I saw the Prophet (ﷺ) being inspired divinely on a very cold day and noticed the sweat dropping from his forehead (as the Inspiration was over).',
     'Telah menceritakan kepada kami [Abdullah bin Yusuf] berkata, telah mengabarkan kepada kami [Malik] dari [Hisyam bin \'Urwah] dari [bapaknya] dari [Aisyah] Ibu Kaum Mu\'minin, bahwa Al Harits bin Hisyam bertanya kepada Rasulullah shallallahu \'alaihi wasallam: "Wahai Rasulullah, bagaimana caranya wahyu turun kepada engkau?" Maka Rasulullah shallallahu \'alaihi wasallam menjawab: "Terkadang datang kepadaku seperti suara gemerincing lonceng dan cara ini yang paling berat buatku, lalu terhenti sehingga aku dapat mengerti apa yang disampaikan. Dan terkadang datang Malaikat menyerupai seorang laki-laki lalu berbicara kepadaku maka aku ikuti apa yang diucapkannya". Aisyah berkata: "Sungguh aku pernah melihat turunnya wahyu kepada Beliau shallallahu \'alaihi wasallam pada suatu hari yang sangat dingin lalu terhenti, dan aku lihat dahi Beliau mengucurkan keringat." '),
    ('muslim.1', 1,
     '',
     "It is narrated on the authority of Yahya b. Ya'mur that the first man who discussed qadr (Divine Decree) in Basra was Ma'bad al-Juhani. I along with Humaid b. 'Abdur-Rahman Himyari set out for pilgrimage or for 'Umrah and said:",
     'حَدَّثَنِي أَبُو خَيْثَمَةَ، زُهَيْرُ بْنُ حَرْبٍ حَدَّثَنَا وَكِيعٌ، عَنْ كَهْمَسٍ، عَنْ عَبْدِ اللَّهِ بْنِ بُرَيْدَةَ، عَنْ يَحْيَى بْنِ يَعْمَرَ، ح وَحَدَّثَنَا عُبَيْدُ اللَّهِ بْنُ مُعَاذٍ الْعَنْبَرِيُّ، - وَهَذَا حَدِيثُهُ - حَدَّثَنَا أَبِي، حَدَّثَنَا كَهْمَسٌ، عَنِ ابْنِ بُرَيْدَةَ، عَنْ يَحْيَى بْنِ يَعْمَرَ، قَالَ كَانَ أَوَّلَ مَنْ قَالَ فِي الْقَدَرِ بِالْبَصْرَةِ مَعْبَدٌ الْجُهَنِيُّ فَانْطَلَقْتُ أَنَا وَحُمَيْدُ بْنُ عَبْدِ الرَّحْمَنِ الْحِمْيَرِيُّ حَاجَّيْنِ أَوْ مُعْتَمِرَيْنِ فَقُلْنَا لَوْ لَقِينَا أَحَدًا مِنْ أَصْحَابِ رَسُولِ اللَّهِ صلى الله عليه وسلم فَسَأَلْنَاهُ عَمَّا يَقُولُ هَؤُلاَءِ فِي الْقَدَرِ فَوُفِّقَ لَنَا عَبْدُ اللَّهِ بْنُ عُمَرَ بْنِ الْخَطَّابِ دَاخِلاً الْمَسْجِدَ فَاكْتَنَفْتُهُ أَنَا وَصَاحِبِي أَحَدُنَا عَنْ يَمِينِهِ وَالآخَرُ عَنْ شِمَالِهِ فَظَنَنْتُ أَنَّ صَاحِبِي سَيَكِلُ الْكَلاَمَ إِلَىَّ فَقُلْتُ أَبَا عَبْدِ الرَّحْمَنِ إِنَّهُ قَدْ ظَهَرَ قِبَلَنَا نَاسٌ يَقْرَءُونَ الْقُرْآنَ وَيَتَقَفَّرُونَ الْعِلْمَ - وَذَكَرَ مِنْ شَأْنِهِمْ - وَأَنَّهُمْ يَزْعُمُونَ أَنْ لاَ قَدَرَ وَأَنَّ الأَمْرَ أُنُفٌ \u200f.\u200f قَالَ فَإِذَا لَقِيتَ أُولَئِكَ فَأَخْبِرْهُمْ أَنِّي بَرِيءٌ مِنْهُمْ وَأَنَّهُمْ بُرَآءُ مِنِّي وَالَّذِي يَحْلِفُ بِهِ عَبْدُ اللَّهِ بْنُ عُمَرَ لَوْ أَنَّ لأَحَدِهِمْ مِثْلَ أُحُدٍ ذَهَبًا فَأَنْفَقَهُ مَا قَبِلَ اللَّهُ مِنْهُ حَتَّى يُؤْمِنَ بِالْقَدَرِ ثُمَّ قَالَ حَدَّثَنِي أَبِي عُمَرُ بْنُ الْخَطَّابِ قَالَ بَيْنَمَا نَحْنُ عِنْدَ رَسُولِ اللَّهِ صلى الله عليه وسلم ذَاتَ يَوْمٍ إِذْ طَلَعَ عَلَيْنَا رَجُلٌ شَدِيدُ بَيَاضِ الثِّيَابِ شَدِيدُ سَوَادِ الشَّعَرِ لاَ يُرَى عَلَيْهِ أَثَرُ السَّفَرِ وَلاَ يَعْرِفُهُ مِنَّا أَحَدٌ حَتَّى جَلَسَ إِلَى النَّبِيِّ صلى الله عليه وسلم فَأَسْنَدَ رُكْبَتَيْهِ إِلَى رُكْبَتَيْهِ وَوَضَعَ كَفَّيْهِ عَلَى فَخِذَيْهِ وَقَالَ يَا مُحَمَّدُ أَخْبِرْنِي عَنِ الإِسْلاَمِ \u200f.\u200f فَقَالَ رَسُولُ اللَّهِ صلى الله عليه وسلم \u200f"\u200f الإِسْلاَمُ أَنْ تَشْهَدَ أَنْ لاَ إِلَهَ إِلاَّ اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ وَتُقِيمَ الصَّلاَةَ وَتُؤْتِيَ الزَّكَاةَ وَتَصُومَ رَمَضَانَ وَتَحُجَّ الْبَيْتَ إِنِ اسْتَطَعْتَ إِلَيْهِ سَبِيلاً \u200f.\u200f قَالَ صَدَقْتَ \u200f.\u200f قَالَ فَعَجِبْنَا لَهُ يَسْأَلُهُ وَيُصَدِّقُهُ \u200f.\u200f قَالَ فَأَخْبِرْنِي عَنِ الإِيمَانِ \u200f.\u200f قَالَ \u200f"\u200f أَنْ تُؤْمِنَ بِاللَّهِ وَمَلاَئِكَتِهِ وَكُتُبِهِ وَرُسُلِهِ وَالْيَوْمِ الآخِرِ وَتُؤْمِنَ بِالْقَدَرِ خَيْرِهِ وَشَرِّهِ \u200f"\u200f \u200f.\u200f قَالَ صَدَقْتَ \u200f.\u200f قَالَ فَأَخْبِرْنِي عَنِ الإِحْسَانِ \u200f.\u200f قَالَ \u200f"\u200f أَنْ تَعْبُدَ اللَّهَ كَأَنَّكَ تَرَاهُ فَإِنْ لَمْ تَكُنْ تَرَاهُ فَإِنَّهُ يَرَاكَ \u200f"\u200f \u200f.\u200f قَالَ فَأَخْبِرْنِي عَنِ السَّاعَةِ \u200f.\u200f قَالَ \u200f"\u200f مَا الْمَسْئُولُ عَنْهَا بِأَعْلَمَ مِنَ السَّائِلِ \u200f"\u200f \u200f.\u200f قَالَ فَأَخْبِرْنِي عَنْ أَمَارَتِهَا \u200f.\u200f قَالَ \u200f"\u200f أَنْ تَلِدَ الأَمَةُ رَبَّتَهَا وَأَنْ تَرَى الْحُفَاةَ الْعُرَاةَ الْعَالَةَ رِعَاءَ الشَّاءِ يَتَطَاوَلُونَ فِي الْبُنْيَانِ \u200f"\u200f \u200f.\u200f قَالَ ثُمَّ انْطَلَقَ فَلَبِثْتُ مَلِيًّا ثُمَّ قَالَ لِي \u200f"\u200f يَا عُمَرُ أَتَدْرِي مَنِ السَّائِلُ \u200f"\u200f \u200f.\u200f قُلْتُ اللَّهُ وَرَسُولُهُ أَعْلَمُ \u200f.\u200f قَالَ \u200f"\u200f فَإِنَّهُ جِبْرِيلُ أَتَاكُمْ يُعَلِّمُكُمْ دِينَكُمْ \u200f"\u200f \u200f.\u200f',
     "It is narrated on the authority of Yahya b. Ya'mur that the first man who discussed qadr (Divine Decree) in Basra was Ma'bad al-Juhani. I along with Humaid b. 'Abdur-Rahman Himyari set out for pilgrimage or for 'Umrah and said:Should it so happen that we come into contact with one of the Companions of the Messenger of Allah (peace be upon him) we shall ask him about what is talked about taqdir (Divine Decree). Accidentally we came across Abdullah ibn Umar ibn al-Khattab, while he was entering the mosque. My companion and I surrounded him. One of us (stood) on his right and the other stood on his left. I expected that my companion would authorize me to speak. I therefore said: Abu Abdur Rahman! There have appeared some people in our land who recite the Qur'an and pursue knowledge. And then after talking about their affairs, added: They (such people) claim that there is no such thing as Divine Decree and events are not predestined. He (Abdullah ibn Umar) said: When you happen to meet such people tell them that I have nothing to do with them and they have nothing to do with me. And verily they are in no way responsible for my (belief). Abdullah ibn Umar swore by Him (the Lord) (and said): If any one of them (who does not believe in the Divine Decree) had with him gold equal to the bulk of (the mountain) Uhud and spent it (in the way of Allah), Allah would not accept it unless he affirmed his faith in Divine Decree. He further said: My father, Umar ibn al-Khattab, told me: One day we were sitting in the company of Allah's Apostle (peace be upon him) when there appeared before us a man dressed in pure white clothes, his hair extraordinarily black. There were no signs of travel on him. None amongst us recognized him. At last he sat with the Apostle (peace be upon him) He knelt before him placed his palms on his thighs and said: Muhammad, inform me about al-Islam. The Messenger of Allah (peace be upon him) said: Al-Islam implies that you testify that there is no god but Allah and that Muhammad is the messenger of Allah, and you establish prayer, pay Zakat, observe the fast of Ramadan, and perform pilgrimage to the (House) if you are solvent enough (to bear the expense of) the journey. He (the inquirer) said: You have told the truth. He (Umar ibn al-Khattab) said: It amazed us that he would put the question and then he would himself verify the truth. He (the inquirer) said: Inform me about Iman (faith). He (the Holy Prophet) replied: That you affirm your faith in Allah, in His angels, in His Books, in His Apostles, in the Day of Judgment, and you affirm your faith in the Divine Decree about good and evil. He (the inquirer) said: You have told the truth. He (the inquirer) again said: Inform me about al-Ihsan (performance of good deeds). He (the Holy Prophet) said: That you worship Allah as if you are seeing Him, for though you don't see Him, He, verily, sees you. He (the enquirer) again said: Inform me about the hour (of the Doom). He (the Holy Prophet) remarked: One who is asked knows no more than the one who is inquiring (about it). He (the inquirer) said: Tell me some of its indications. He (the Holy Prophet) said: That the slave-girl will give birth to her mistress and master, that you will find barefooted, destitute goat-herds vying with one another in the construction of magnificent buildings. He (the narrator, Umar ibn al-Khattab) said: Then he (the inquirer) went on his way but I stayed with him (the Holy Prophet) for a long while. He then, said to me: Umar, do you know who this inquirer was? I replied: Allah and His Apostle knows best. He (the Holy Prophet) remarked: He was Gabriel (the angel). He came to you in order to instruct you in matters of religion.",
     'Telah menceritakan kepadaku [Abu Khaitsamah Zuhair bin Harb] telah menceritakan kepada kami [Waki\'] dari [Kahmas] dari [Abdullah bin Buraidah] dari [Yahya bin Ya\'mar]. (dalam riwayat lain disebutkan) Dan telah menceritakan kepada kami [Ubaidullah bin Mu\'adz al-\'Anbari] dan ini hadisnya, telah menceritakan kepada kami [Bapakku] telah menceritakan kepada kami [Kahmas] dari [Ibnu Buraidah] dari [Yahya bin Ya\'mar] dia berkata, "Orang yang pertama kali membahas takdir di Bashrah adalah Ma\'bad al-Juhani, maka aku dan Humaid bin Abdurrahman al-Himyari bertolak haji atau umrah, maka kami berkata, \'Seandainya kami bertemu dengan salah seorang sahabat Rasulullah shallallahu \'alaihi wasallam, maka kami akan bertanya kepadanya tentang sesuatu yang mereka katakan berkaitan dengan takdir.\' Maka [Abdullah bin Umar] diberikan taufik (oleh Allah) untuk kami, sedangkan dia masuk masjid. Lalu aku dan temanku menghadangnya. Salah seorang dari kami di sebelah kanannya dan yang lain di sebelah kirinya. Lalu aku mengira bahwa temanku akan mewakilkan pembicaraan kepadaku, maka aku berkata, \'Wahai Abu Abdurrahman, sesungguhnya nampak di hadapan kami suatu kaum membaca al-Qur\'an dan mencari ilmu lalu mengklaim bahwa tidak ada takdir, dan perkaranya adalah baru (tidak didahului oleh takdir dan ilmu Allah).\' Maka [Abdullah bin Umar] menjawab, \'Apabila kamu bertemu orang-orang tersebut, maka kabarkanlah kepada mereka bahwa saya berlepas diri dari mereka, dan bahwa mereka berlepas diri dariku. Dan demi Dzat yang mana hamba Allah bersumpah dengan-Nya, kalau seandainya salah seorang dari kalian menafkahkan emas seperti gunung Uhud, niscaya sedekahnya tidak akan diterima hingga dia beriman kepada takdir baik dan buruk.\' Dia berkata, \'Kemudian dia mulai menceritakan hadis seraya berkata, [\'Umar bin al-Khaththab] berkata, \'Dahulu kami pernah berada di sisi Rasulullah shallallahu \'alaihi wasallam, lalu datanglah seorang laki-laki yang bajunya sangat putih, rambutnya sangat hitam, tidak tampak padanya bekas-bekas perjalanan. Tidak seorang pun dari kami mengenalnya, hingga dia mendatangi Nabi Shallallahu \'Alaihi Wasalam lalu menyandarkan lututnya pada lutut Nabi Shallallahu \'Alaihi Wasalam, kemudian ia berkata, \'Wahai Muhammad, kabarkanlah kepadaku tentang Islam? \' Rasulullah Shallallahu \'Alaihi Wasalam menjawab: "Kesaksian bahwa tidak ada tuhan (yang berhak disembah) selain Allah dan bahwa Muhammad adalah hamba dan utusan-Nya, mendirikan shalat, menunaikan zakat, dan puasa Ramadlan, serta haji ke Baitullah jika kamu mampu bepergian kepadanya.\' Dia berkata, \'Kamu benar.\' Umar berkata, \'Maka kami kaget terhadapnya karena dia menanyakannya dan membenarkannya.\' Dia bertanya lagi, \'Kabarkanlah kepadaku tentang iman itu? \' Beliau menjawab: "Kamu beriman kepada Allah, malaikat-malaikat-Nya, kitab-kitab-Nya, para Rasul-Nya, hari akhir, dan takdir baik dan buruk." Dia berkata, \'Kamu benar.\' Dia bertanya, \'Kabarkanlah kepadaku tentang ihsan itu? \' Beliau menjawab: "Kamu menyembah Allah seakan-akan kamu melihat-Nya, maka jika kamu tidak melihat-Nya, maka sesungguhnya Dia melihatmu." Dia bertanya lagi, \'Kapankah hari akhir itu? \' Beliau menjawab: "Tidaklah orang yang ditanya itu lebih mengetahui daripada orang yang bertanya." Dia bertanya, \'Lalu kabarkanlah kepadaku tentang tanda-tandanya? \' Beliau menjawab: "Apabila seorang budak melahirkan (anak) tuan-Nya, dan kamu melihat orang yang tidak beralas kaki, telanjang, miskin, penggembala kambing, namun bermegah-megahan dalam membangun bangunan." Kemudian dia bertolak pergi. Maka aku tetap saja heran kemudian beliau berkata; "Wahai Umar, apakah kamu tahu siapa penanya tersebut?" Aku menjawab, \'Allah dan Rasul-Nya lebih tahu.\' Beliau bersabda: "Itulah jibril, dia mendatangi kalian untuk mengajarkan kepada kalian tentang pengetahuan agama kalian\'." '),
    ('muslim.1', 2,
     '',
     "It is narrated on the authority of Yahya b. Ya'mur that when Ma'bad discussed the problem pertaining to Divine Decree, we refuted that. He (the narrator) said:",
     'حَدَّثَنِي مُحَمَّدُ بْنُ عُبَيْدٍ الْغُبَرِيُّ، وَأَبُو كَامِلٍ الْجَحْدَرِيُّ وَأَحْمَدُ بْنُ عَبْدَةَ قَالُوا حَدَّثَنَا حَمَّادُ بْنُ زَيْدٍ، عَنْ مَطَرٍ الْوَرَّاقِ، عَنْ عَبْدِ اللَّهِ بْنِ بُرَيْدَةَ، عَنْ يَحْيَى بْنِ يَعْمَرَ، قَالَ لَمَّا تَكَلَّمَ مَعْبَدٌ بِمَا تَكَلَّمَ بِهِ فِي شَأْنِ الْقَدَرِ أَنْكَرْنَا ذَلِكَ \u200f.\u200f قَالَ فَحَجَجْتُ أَنَا وَحُمَيْدُ بْنُ عَبْدِ الرَّحْمَنِ الْحِمْيَرِيُّ حِجَّةً \u200f.\u200f وَسَاقُوا الْحَدِيثَ بِمَعْنَى حَدِيثِ كَهْمَسٍ وَإِسْنَادِهِ \u200f.\u200f وَفِيهِ بَعْضُ زِيَادَةٍ وَنُقْصَانُ أَحْرُفٍ \u200f.\u200f',
     "It is narrated on the authority of Yahya b. Ya'mur that when Ma'bad discussed the problem pertaining to Divine Decree, we refuted that. He (the narrator) said:I and Humaid b. Abdur-Rahman Himyari argued. And they carried on the conversation about the purport of the hadith related by Kahmas and its chain of transmission too, and there is some variation of words.",
     'Telah menceritakan kepada kami [Muhammad bin Ubaid al-Ghubari] dan [Abu Kamil al-Jahdari] serta [Ahmad bin Abdah] mereka berkata, telah menceritakan kepada kami [Hammad bin Yazid] dari [Mathar al Warraq] dari [Abdullah bin Buraidah] dari [Yahya bin Ya\'mar] dia berkata, \'Ketika Ma\'bad berkata dengan sesuatu yang dia bicarakan tentang masalah takdir, maka kami mengingkari hal tersebut.\' Dia berkata lagi, \'Lalu aku melakukan haji bersama Humaid bin Abdurrahman al-Himyari.\' Lalu mereka menyebutkan hadis dengan makna hadis Kahmas. Di dalamnya terdapat sebagian tambahan dan kekurangan huruf." '),
]


DB_SCHEMA = """
CREATE TABLE books (
    id TEXT PRIMARY KEY,
    collection TEXT NOT NULL,
    number INTEGER NOT NULL,
    name_ar TEXT NOT NULL,
    name_en TEXT NOT NULL,
    name_id TEXT NOT NULL
);
CREATE TABLE hadiths (
    id INTEGER PRIMARY KEY,
    book_id TEXT NOT NULL,
    in_book_number INTEGER NOT NULL,
    narrator_ar TEXT,
    narrator_en TEXT,
    text_ar TEXT NOT NULL,
    text_en TEXT NOT NULL,
    text_id TEXT NOT NULL
);
CREATE INDEX idx_hadiths_book ON hadiths(book_id);
"""


def main() -> None:
    assert len(HADITH) >= 4, "Seed must contain at least 4 verified hadith"
    for i, h in enumerate(HADITH, start=1):
        assert h[4].strip() and h[5].strip() and h[6].strip(), f"hadith #{i} has blank text"
    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    if os.path.exists(OUT_PATH):
        os.remove(OUT_PATH)
    conn = sqlite3.connect(OUT_PATH)
    try:
        conn.executescript(DB_SCHEMA)
        conn.executemany(
            "INSERT INTO books (id, collection, number, name_ar, name_en, name_id)"
            " VALUES (:id, :collection, :number, :name_ar, :name_en, :name_id)",
            BOOKS,
        )
        conn.executemany(
            "INSERT INTO hadiths (id, book_id, in_book_number, narrator_ar, narrator_en,"
            " text_ar, text_en, text_id)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            [tuple([i + 1] + list(h)) for i, h in enumerate(HADITH)],
        )
        conn.commit()
    finally:
        conn.close()
    print(f"Wrote {OUT_PATH}")


if __name__ == "__main__":
    main()
