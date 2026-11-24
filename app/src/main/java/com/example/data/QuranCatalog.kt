package com.example.data

import com.example.model.DailyRecitation
import com.example.model.MemorialDua
import com.example.model.Reciter
import com.example.model.Riwayah
import com.example.model.Surah
import java.util.Calendar

object QuranCatalog {

  const val CAIRO_RADIO_STREAM_URL = "https://stream.radiojar.com/8s5u5tpdtwzuv"
  const val DEDICATED_PERSON_NAME = "عبد العظيم رجب زكي"

  val reciters: List<Reciter> =
    listOf(
      Reciter(
        id = "husary",
        nameAr = "محمود خليل الحصري",
        isMostListened = true,
        wikipediaTitles = listOf("محمود خليل الحصري"),
        enWikipediaTitles = listOf("Mahmoud Khalil Al-Hussary"),
        riwayat =
          listOf(
            Riwayah("حفص عن عاصم", "https://server13.mp3quran.net/husr/"),
            Riwayah("ورش عن نافع", "https://server13.mp3quran.net/husr/Rewayat-Warsh-A-n-Nafi/"),
            Riwayah("قالون عن نافع", "https://server13.mp3quran.net/husr/Rewayat-Qalon-A-n-Nafi/"),
            Riwayah(
              "الدوري عن أبي عمرو",
              "https://server13.mp3quran.net/husr/Rewayat-Aldori-A-n-Abi-Amr/",
            ),
          ),
      ),
      Reciter(
        id = "minshawi",
        nameAr = "محمد صديق المنشاوي",
        isMostListened = true,
        wikipediaTitles = listOf("محمد صديق المنشاوي"),
        enWikipediaTitles = listOf("Muhammad Siddiq Al-Minshawi"),
        riwayat =
          listOf(
            Riwayah("حفص عن عاصم - مرتل", "https://server10.mp3quran.net/minsh/"),
            Riwayah(
              "المصحف المجود",
              "https://server10.mp3quran.net/minsh/Almusshaf-Al-Mojawwad/",
            ),
          ),
      ),
      Reciter(
        id = "basit",
        nameAr = "عبد الباسط عبد الصمد",
        isMostListened = true,
        wikipediaTitles = listOf("عبد الباسط عبد الصمد"),
        enWikipediaTitles = listOf("Abdul Basit 'Abd us-Samad"),
        riwayat =
          listOf(
            Riwayah("حفص عن عاصم", "https://server7.mp3quran.net/basit/"),
            Riwayah("ورش عن نافع", "https://server7.mp3quran.net/basit/Rewayat-Warsh-A-n-Nafi/"),
            Riwayah(
              "المصحف المجود",
              "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/",
            ),
          ),
      ),
      Reciter(
        id = "afasy",
        nameAr = "مشاري بن راشد العفاسي",
        isMostListened = true,
        wikipediaTitles = listOf("مشاري العفاسي", "مشاري بن راشد العفاسي"),
        enWikipediaTitles = listOf("Mishary bin Rashid Alafasy"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server8.mp3quran.net/afs/")),
      ),
      Reciter(
        id = "sudais",
        nameAr = "عبد الرحمن السديس",
        isMostListened = true,
        wikipediaTitles = listOf("عبد الرحمن السديس"),
        enWikipediaTitles = listOf("Abdul-Rahman Al-Sudais"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server11.mp3quran.net/sds/")),
      ),
      Reciter(
        id = "maher",
        nameAr = "ماهر المعيقلي",
        isMostListened = true,
        wikipediaTitles = listOf("ماهر المعيقلي"),
        enWikipediaTitles = listOf("Maher Al-Muaiqly"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server12.mp3quran.net/maher/")),
      ),
      Reciter(
        id = "yasser",
        nameAr = "ياسر الدوسري",
        isMostListened = true,
        wikipediaTitles = listOf("ياسر الدوسري"),
        enWikipediaTitles = listOf("Yasser Al-Dosari"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server11.mp3quran.net/yasser/")),
      ),
      Reciter(
        id = "tablawi",
        nameAr = "محمد محمود الطبلاوي",
        isMostListened = false,
        wikipediaTitles = listOf("محمد محمود الطبلاوي", "محمد الطبلاوي"),
        enWikipediaTitles = listOf("Mohamed Mahmoud Al-Tablawi"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server12.mp3quran.net/tblawi/")),
      ),
      Reciter(
        id = "banna",
        nameAr = "محمود علي البنا",
        isMostListened = false,
        wikipediaTitles = listOf("محمود علي البنا"),
        enWikipediaTitles = listOf("Mahmoud Ali Al-Banna"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server8.mp3quran.net/bna/")),
      ),
      Reciter(
        id = "mustafa",
        nameAr = "مصطفى إسماعيل",
        isMostListened = false,
        wikipediaTitles = listOf("مصطفى إسماعيل (قارئ)", "مصطفى إسماعيل"),
        enWikipediaTitles = listOf("Mustafa Ismail"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server8.mp3quran.net/mustafa/")),
      ),
      Reciter(
        id = "ajmy",
        nameAr = "أحمد بن علي العجمي",
        isMostListened = false,
        wikipediaTitles = listOf("أحمد بن علي العجمي", "أحمد العجمي (قارئ)"),
        enWikipediaTitles = listOf("Ahmad bin Ali Al-Ajmi"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server10.mp3quran.net/ajm/")),
      ),
      Reciter(
        id = "shatri",
        nameAr = "أبو بكر الشاطري",
        isMostListened = false,
        wikipediaTitles = listOf("أبو بكر الشاطري"),
        enWikipediaTitles = listOf("Abu Bakr ash-Shatry", "Abu Bakr al-Shatri"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server11.mp3quran.net/shatri/")),
      ),
      Reciter(
        id = "juhani",
        nameAr = "عبد الله عواد الجهني",
        isMostListened = false,
        wikipediaTitles = listOf("عبد الله عواد الجهني", "عبد الله الجهني"),
        enWikipediaTitles = listOf("Abdullah Awad Al Juhany"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server13.mp3quran.net/jhn/")),
      ),
      Reciter(
        id = "ghamdi",
        nameAr = "سعد الغامدي",
        isMostListened = true,
        wikipediaTitles = listOf("سعد الغامدي"),
        enWikipediaTitles = listOf("Saad Al-Ghamdi"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server7.mp3quran.net/s_gmd/")),
      ),
      Reciter(
        id = "alzain",
        nameAr = "الزين محمد أحمد",
        isMostListened = false,
        wikipediaTitles = listOf("الزين محمد أحمد"),
        enWikipediaTitles = listOf("Al-Zain Muhammad Ahmad"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server9.mp3quran.net/alzain/")),
      ),
      Reciter(
        id = "fateh",
        nameAr = "الفاتح محمد الزبير",
        isMostListened = false,
        wikipediaTitles = listOf("الفاتح محمد الزبير", "الفاتح الزبير"),
        riwayat = listOf(Riwayah("الدوري عن أبي عمرو", "https://server6.mp3quran.net/fateh/")),
      ),
      Reciter(
        id = "nourin",
        nameAr = "نورين محمد صديق",
        isMostListened = false,
        wikipediaTitles = listOf("نورين محمد صديق"),
        enWikipediaTitles = listOf("Noreen Muhammad Siddiq"),
        riwayat =
          listOf(
            Riwayah(
              "الدوري عن أبي عمرو",
              "https://server16.mp3quran.net/nourin_siddig/Rewayat-Aldori-A-n-Abi-Amr/",
            )
          ),
      ),
      Reciter(
        id = "koshi",
        nameAr = "العيون الكوشي",
        isMostListened = false,
        wikipediaTitles = listOf("العيون الكوشي", "لعيون الكوشي"),
        riwayat = listOf(Riwayah("ورش عن نافع", "https://server11.mp3quran.net/koshi/")),
      ),
      Reciter(
        id = "qazabri",
        nameAr = "عمر القزابري",
        isMostListened = false,
        wikipediaTitles = listOf("عمر القزابري"),
        riwayat = listOf(Riwayah("ورش عن نافع", "https://server9.mp3quran.net/omar_h/")),
      ),
      Reciter(
        id = "jibril",
        nameAr = "محمد جبريل",
        isMostListened = false,
        wikipediaTitles = listOf("محمد جبريل (قارئ)", "محمد جبريل"),
        riwayat = listOf(Riwayah("حفص عن عاصم", "https://server8.mp3quran.net/jbrl/")),
      ),
    )

  val surahs: List<Surah> =
    listOf(
      Surah(1, "الفاتحة", "مكية", 7),
      Surah(2, "البقرة", "مدنية", 286),
      Surah(3, "آل عمران", "مدنية", 200),
      Surah(4, "النساء", "مدنية", 176),
      Surah(5, "المائدة", "مدنية", 120),
      Surah(6, "الأنعام", "مكية", 165),
      Surah(7, "الأعراف", "مكية", 206),
      Surah(8, "الأنفال", "مدنية", 75),
      Surah(9, "التوبة", "مدنية", 129),
      Surah(10, "يونس", "مكية", 109),
      Surah(11, "هود", "مكية", 123),
      Surah(12, "يوسف", "مكية", 111),
      Surah(13, "الرعد", "مدنية", 43),
      Surah(14, "إبراهيم", "مكية", 52),
      Surah(15, "الحجر", "مكية", 99),
      Surah(16, "النحل", "مكية", 128),
      Surah(17, "الإسراء", "مكية", 111),
      Surah(18, "الكهف", "مكية", 110),
      Surah(19, "مريم", "مكية", 98),
      Surah(20, "طه", "مكية", 135),
      Surah(21, "الأنبياء", "مكية", 112),
      Surah(22, "الحج", "مدنية", 78),
      Surah(23, "المؤمنون", "مكية", 118),
      Surah(24, "النور", "مدنية", 64),
      Surah(25, "الفرقان", "مكية", 77),
      Surah(26, "الشعراء", "مكية", 227),
      Surah(27, "النمل", "مكية", 93),
      Surah(28, "القصص", "مكية", 88),
      Surah(29, "العنكبوت", "مكية", 69),
      Surah(30, "الروم", "مكية", 60),
      Surah(31, "لقمان", "مكية", 34),
      Surah(32, "السجدة", "مكية", 30),
      Surah(33, "الأحزاب", "مدنية", 73),
      Surah(34, "سبأ", "مكية", 54),
      Surah(35, "فاطر", "مكية", 45),
      Surah(36, "يس", "مكية", 83),
      Surah(37, "الصافات", "مكية", 182),
      Surah(38, "ص", "مكية", 88),
      Surah(39, "الزمر", "مكية", 75),
      Surah(40, "غافر", "مكية", 85),
      Surah(41, "فصلت", "مكية", 54),
      Surah(42, "الشورى", "مكية", 53),
      Surah(43, "الزخرف", "مكية", 89),
      Surah(44, "الدخان", "مكية", 59),
      Surah(45, "الجاثية", "مكية", 37),
      Surah(46, "الأحقاف", "مكية", 35),
      Surah(47, "محمد", "مدنية", 38),
      Surah(48, "الفتح", "مدنية", 29),
      Surah(49, "الحجرات", "مدنية", 18),
      Surah(50, "ق", "مكية", 45),
      Surah(51, "الذاريات", "مكية", 60),
      Surah(52, "الطور", "مكية", 49),
      Surah(53, "النجم", "مكية", 62),
      Surah(54, "القمر", "مكية", 55),
      Surah(55, "الرحمن", "مدنية", 78),
      Surah(56, "الواقعة", "مكية", 96),
      Surah(57, "الحديد", "مدنية", 29),
      Surah(58, "المجادلة", "مدنية", 22),
      Surah(59, "الحشر", "مدنية", 24),
      Surah(60, "الممتحنة", "مدنية", 13),
      Surah(61, "الصف", "مدنية", 14),
      Surah(62, "الجمعة", "مدنية", 11),
      Surah(63, "المنافقون", "مدنية", 11),
      Surah(64, "التغابن", "مدنية", 18),
      Surah(65, "الطلاق", "مدنية", 12),
      Surah(66, "التحريم", "مدنية", 12),
      Surah(67, "الملك", "مكية", 30),
      Surah(68, "القلم", "مكية", 52),
      Surah(69, "الحاقة", "مكية", 52),
      Surah(70, "المعارج", "مكية", 44),
      Surah(71, "نوح", "مكية", 28),
      Surah(72, "الجن", "مكية", 28),
      Surah(73, "المزمل", "مكية", 20),
      Surah(74, "المدثر", "مكية", 56),
      Surah(75, "القيامة", "مكية", 40),
      Surah(76, "الإنسان", "مدنية", 31),
      Surah(77, "المرسلات", "مكية", 50),
      Surah(78, "النبأ", "مكية", 40),
      Surah(79, "النازعات", "مكية", 46),
      Surah(80, "عبس", "مكية", 42),
      Surah(81, "التكوير", "مكية", 29),
      Surah(82, "الانفطار", "مكية", 19),
      Surah(83, "المطففين", "مكية", 36),
      Surah(84, "الانشقاق", "مكية", 25),
      Surah(85, "البروج", "مكية", 22),
      Surah(86, "الطارق", "مكية", 17),
      Surah(87, "الأعلى", "مكية", 19),
      Surah(88, "الغاشية", "مكية", 26),
      Surah(89, "الفجر", "مكية", 30),
      Surah(90, "البلد", "مكية", 20),
      Surah(91, "الشمس", "مكية", 15),
      Surah(92, "الليل", "مكية", 21),
      Surah(93, "الضحى", "مكية", 11),
      Surah(94, "الشرح", "مكية", 8),
      Surah(95, "التين", "مكية", 8),
      Surah(96, "العلق", "مكية", 19),
      Surah(97, "القدر", "مكية", 5),
      Surah(98, "البينة", "مدنية", 8),
      Surah(99, "الزلزلة", "مدنية", 8),
      Surah(100, "العاديات", "مكية", 11),
      Surah(101, "القارعة", "مكية", 11),
      Surah(102, "التكاثر", "مكية", 8),
      Surah(103, "العصر", "مكية", 3),
      Surah(104, "الهمزة", "مكية", 9),
      Surah(105, "الفيل", "مكية", 5),
      Surah(106, "قريش", "مكية", 4),
      Surah(107, "الماعون", "مكية", 7),
      Surah(108, "الكوثر", "مكية", 3),
      Surah(109, "الكافرون", "مكية", 6),
      Surah(110, "النصر", "مدنية", 3),
      Surah(111, "المسد", "مكية", 5),
      Surah(112, "الإخلاص", "مكية", 4),
      Surah(113, "الفلق", "مكية", 5),
      Surah(114, "الناس", "مكية", 6),
    )

  private val curatedDailyRecitations: List<DailyRecitation> =
    listOf(
      DailyRecitation("minshawi", 0, 19, "تلاوة خاشعة لسورة مريم"),
      DailyRecitation("husary", 0, 18, "نور ما بين الجمعتين — سورة الكهف"),
      DailyRecitation("basit", 1, 55, "سورة الرحمن برواية ورش عن نافع"),
      DailyRecitation("yasser", 0, 36, "تلاوة متدبرة لسورة يس"),
      DailyRecitation("afasy", 0, 67, "سورة الملك — المنجية من عذاب القبر"),
      DailyRecitation("nourin", 0, 56, "تلاوة سودانية مباركة لسورة الواقعة"),
      DailyRecitation("maher", 0, 12, "أحسن القصص — سورة يوسف"),
      DailyRecitation("sudais", 0, 2, "سورة البقرة — أخذها بركة"),
      DailyRecitation("mustafa", 0, 24, "آيات النور والسكينة"),
      DailyRecitation("ghamdi", 0, 31, "وصايا لقمان الحكيم"),
      DailyRecitation("koshi", 0, 50, "سورة ق برواية ورش عن نافع"),
      DailyRecitation("alzain", 0, 39, "تلاوة مؤثرة لسورة الزمر"),
      DailyRecitation("banna", 0, 76, "سورة الإنسان بأداء مصري أصيل"),
      DailyRecitation("jibril", 0, 40, "مناجاة وتلاوة لسورة غافر"),
    )

  fun todayRecitation(calendar: Calendar = Calendar.getInstance()): DailyRecitation {
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR).coerceAtLeast(1)
    return curatedDailyRecitations[(dayOfYear - 1) % curatedDailyRecitations.size]
  }

  val memorialDuas: List<MemorialDua> =
    listOf(
      MemorialDua(
        id = "dua_main",
        titleAr = "دعاء المغفرة والرحمة",
        textAr =
          "اللهم اغفر لعبدك عبد العظيم رجب زكي، وارحمه، وعافه واعف عنه، وأكرم نزله، ووسع مدخله، واغسله بالماء والثلج والبرد، ونقّه من الخطايا كما نقيت الثوب الأبيض من الدنس، وأبدله داراً خيراً من داره، وأهلاً خيراً من أهله، وأدخله الجنة وأعذه من عذاب القبر ومن عذاب النار.",
        sourceAr = "صحيح مسلم",
      ),
      MemorialDua(
        id = "dua_light",
        titleAr = "دعاء نور القبر والفردوس",
        textAr =
          "اللهم اجعل قبر عبد العظيم رجب زكي روضة من رياض الجنة ولا تجعله حفرة من حفر النار، اللهم آنس وحشته وارحم غربته وأنر قبره بنور القرآن الكريم الذي يُتلى آناء الليل وأطراف النهار، واجعل هذا العمل خالصاً لوجهك الكريم صدقةً جاريةً له.",
        sourceAr = "دعاء للميت",
      ),
      MemorialDua(
        id = "dua_steadfast",
        titleAr = "دعاء الثبات والرفعة",
        textAr =
          "اللهم إنه في ذمتك وحبل جوارك، فقِهِ فتنة القبر وعذاب النار، وأنت أهل الوفاء والحق، فاغفر له وارحمه إنك أنت الغفور الرحيم. اللهم ارفع درجته في المهديين، واخلفه في عقبه في الغابرين، واغفر لنا وله يا رب العالمين.",
        sourceAr = "سنن أبي داود وصحيح مسلم",
      ),
      MemorialDua(
        id = "dua_quran",
        titleAr = "دعاء التوسل بالقرآن",
        textAr =
          "اللهم اجعل القرآن العظيم ربيع قلبه، وشفيعاً له عندك، ومؤنساً له في قبره، وحجةً له لا عليه، واجمعنا به في الفردوس الأعلى من الجنة مع النبيين والصديقين والشهداء والصالحين.",
        sourceAr = "دعاء مأثور",
      ),
    )

  fun findReciter(id: String): Reciter? = reciters.firstOrNull { it.id == id }

  fun findSurah(number: Int): Surah? = surahs.firstOrNull { it.number == number }

  fun normalizeArabic(input: String): String =
    input
      .trim()
      .replace(Regex("[\u064B-\u065F\u0670\u0640]"), "")
      .replace('أ', 'ا')
      .replace('إ', 'ا')
      .replace('آ', 'ا')
      .replace('ة', 'ه')
      .replace('ى', 'ي')
      .lowercase()
}
