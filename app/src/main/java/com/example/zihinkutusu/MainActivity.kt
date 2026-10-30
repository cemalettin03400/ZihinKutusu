package com.example.zihinkutusu

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Vibrator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlin.random.Random

data class WordPuzzle(val words: List<String>, val grid: Array<CharArray>)

class MainActivity : Activity() {
    private companion object { const val GRID_SIZE = 10 }
    private lateinit var root: LinearLayout
    private lateinit var grid: WordGridView
    private lateinit var selectedText: TextView
    private lateinit var wordListBox: LinearLayout
    private lateinit var infoText: TextView
    private lateinit var livesText: TextView
    private var level = 1
    private var maxUnlocked = 1
    private var score = 0
    private var coins = 50
    private var lives = 5
    private var found = mutableSetOf<String>()
    private var selectedCells = mutableListOf<Int>()
    private var currentPuzzle: WordPuzzle? = null
    private var hintCell = -1
    private var combo = 0
    private var wrongAttempts = 0
    private var levelStartTime = 0L
    private var soundEnabled = true
    private var vibrationEnabled = true
    private var toneGenerator: ToneGenerator? = null
    private var raceMode = false
    private var raceDifficulty = "Normal"
    private var computerFound = 0
    private var raceStatusText: TextView? = null
    private val raceHandler = Handler(Looper.getMainLooper())
    private var raceRunnable: Runnable? = null

    private val prefs by lazy { getSharedPreferences("game", 0) }

    // Türkçe karakterler özellikle korunur: Ç Ğ İ Ö Ş Ü ve ı/i ayrımı önemlidir.
    private val wordBank = listOf(
        listOf("KALEM", "KİTAP", "OKUL", "DEFTER", "SİLGİ", "ÇANTA"),
        listOf("MASA", "KAPI", "SAAT", "ODA", "DUVAR", "KOLTUK"),
        listOf("ELMA", "ARMUT", "MUZ", "KİRAZ", "ŞEFTALİ", "ERİK"),
        listOf("DENİZ", "GÜNEŞ", "BULUT", "YAĞMUR", "RÜZGÂR", "DÜNYA"),
        listOf("KEDİ", "KÖPEK", "KUŞ", "BALIK", "TAVŞAN", "KELEBEK"),
        listOf("ARABA", "YOL", "KÖPRÜ", "TREN", "GEMİ", "UÇAK"),
        listOf("EV", "ODA", "BAHÇE", "BALKON", "ÇATI", "KAPI"),
        listOf("ÇAY", "KAHVE", "EKMEK", "PEYNİR", "ZEYTİN", "ÇORBA"),
        listOf("ANNE", "BABA", "AİLE", "KARDEŞ", "DEDE", "NİNE"),
        listOf("MUTLU", "SEVGİ", "DOST", "GÜLÜMSE", "NEŞE", "UMUT"),
        listOf("TELEFON", "EKRAN", "MESAJ", "KAMERA", "İNTERNET", "ŞARJ"),
        listOf("KIRMIZI", "MAVİ", "YEŞİL", "SARI", "TURUNCU", "MOR"),
        listOf("KIŞ", "BAHAR", "YAZ", "SONBAHAR", "KAR", "ÇİÇEK"),
        listOf("SABAH", "AKŞAM", "GECE", "ÖĞLE", "BUGÜN", "YARIN"),
        listOf("TATLI", "TUZLU", "EKŞİ", "ACI", "LEZZET", "ŞEKER"),
        listOf("BİLGİ", "ZEKÂ", "SORU", "CEVAP", "DÜŞÜNCE", "AKIL"),
        listOf("OYUN", "KAZAN", "PUAN", "BÖLÜM", "HEDEF", "BAŞARI"),
        listOf("ALTIN", "PARA", "KASA", "HAZİNE", "KUPA", "ÖDÜL"),
        listOf("HIZLI", "DİKKAT", "AKIL", "ZEKA", "BECERİ", "KARAR"),
        listOf("KİTAP", "HİKÂYE", "ROMAN", "ŞİİR", "YAZAR", "OKUMAK"),
        listOf("DAĞ", "OVA", "GÖL", "NEHİR", "ORMAN", "TEPE"),
        listOf("ÇİÇEK", "AĞAÇ", "YAPRAK", "DAL", "KÖK", "MEYVE"),
        listOf("MÜZİK", "ŞARKI", "SES", "RİTİM", "SAZ", "DAVUL"),
        listOf("FUTBOL", "TOP", "KALE", "GOL", "TAKIM", "MAÇ"),
        listOf("OKUL", "ÖĞRETMEN", "ÖĞRENCİ", "DERS", "SINIF", "SINAV"),
        listOf("HASTANE", "DOKTOR", "HEMŞİRE", "İLAÇ", "SAĞLIK", "MUAYENE"),
        listOf("ŞEHİR", "SOKAK", "CADDE", "PARK", "MEYDAN", "BİNA"),
        listOf("DENİZ", "KUM", "DALGA", "SAHİL", "GÜNEŞ", "TATİL"),
        listOf("KÖY", "TARLA", "ÇİFTÇİ", "HAYVAN", "BUĞDAY", "HASAT"),
        listOf("KELİME", "HARF", "BULMACA", "ZEKA", "OYUN", "ÇÖZÜM")
    )


    // Her dil için ayrı kelime havuzu; bilgisayar yarışı da aynı bulmacayı kullandığı için burada seçili dili izler.
    private val localizedWordBanks: Map<String, List<List<String>>> = mapOf(
        "en" to listOf(listOf("CHAIR", "WALL", "TABLE", "DOOR", "CLOCK", "WINDOW", "FLOOR", "ROOM"), listOf("APPLE", "PEAR", "BANANA", "CHERRY", "PEACH", "PLUM"), listOf("OCEAN", "SUN", "CLOUD", "RAIN", "WIND", "EARTH")),
        "de" to listOf(listOf("STUHL", "WAND", "TISCH", "TUER", "UHR", "FENSTER", "BODEN", "RAUM"), listOf("APFEL", "BIRNE", "BANANE", "KIRSCHE", "PFIRSICH", "PFLAUME")),
        "fr" to listOf(listOf("CHAISE", "MUR", "TABLE", "PORTE", "HORLOGE", "FENETRE", "SOL", "PIECE"), listOf("POMME", "POIRE", "BANANE", "CERISE", "PECHE", "PRUNE")),
        "es" to listOf(listOf("SILLA", "PARED", "MESA", "PUERTA", "RELOJ", "VENTANA", "SUELO", "CUARTO"), listOf("MANZANA", "PERA", "PLATANO", "CEREZA", "MELOCOTON", "CIRUELA")),
        "ar" to listOf(listOf("كرسي", "جدار", "طاولة", "باب", "ساعة", "نافذة", "أرض", "غرفة"), listOf("تفاح", "كمثرى", "موز", "كرز", "خوخ", "برقوق")),
        "ru" to listOf(listOf("СТУЛ", "СТЕНА", "СТОЛ", "ДВЕРЬ", "ЧАСЫ", "ОКНО", "ПОЛ", "КОМНАТА"), listOf("ЯБЛОКО", "ГРУША", "БАНАН", "ВИШНЯ", "ПЕРСИК", "СЛИВА")),
        "it" to listOf(listOf("SEDIA", "MURO", "TAVOLO", "PORTA", "OROLOGIO", "FINESTRA", "PAVIMENTO", "STANZA"), listOf("MELA", "PERA", "BANANA", "CILIEGIA", "PESCA", "PRUGNA")),
        "pt" to listOf(listOf("CADEIRA", "PAREDE", "MESA", "PORTA", "RELOGIO", "JANELA", "CHAO", "QUARTO"), listOf("MACA", "PERA", "BANANA", "CEREJA", "PESSEGO", "AMEIXA")),
        "zh" to listOf(listOf("椅子", "墙", "桌子", "门", "时钟", "窗户", "地板", "房间"), listOf("苹果", "梨", "香蕉", "樱桃", "桃子", "李子")),
        "ja" to listOf(listOf("イス", "カベ", "ツクエ", "ドア", "トケイ", "マド", "ユカ", "ヘヤ"), listOf("リンゴ", "ナシ", "バナナ", "サクランボ", "モモ", "スモモ")),
        "ko" to listOf(listOf("의자", "벽", "탁자", "문", "시계", "창문", "바닥", "방"), listOf("사과", "배", "바나나", "체리", "복숭아", "자두")),
        "hi" to listOf(listOf("कुर्सी", "दीवार", "मेज", "दरवाजा", "घड़ी", "खिड़की", "फर्श", "कमरा"), listOf("सेब", "नाशपाती", "केला", "चेरी", "आड़ू", "आलूबुखारा")),
        "nl" to listOf(listOf("STOEL", "MUUR", "TAFEL", "DEUR", "KLOK", "RAAM", "VLOER", "KAMER"), listOf("APPEL", "PEER", "BANAAN", "KERS", "PERZIK", "PRUIM")),
        "el" to listOf(listOf("ΚΑΡΕΚΛΑ", "ΤΟΙΧΟΣ", "ΤΡΑΠΕΖΙ", "ΠΟΡΤΑ", "ΡΟΛΟΙ", "ΠΑΡΑΘΥΡΟ", "ΠΑΤΩΜΑ", "ΔΩΜΑΤΙΟ"), listOf("ΜΗΛΟ", "ΑΧΛΑΔΙ", "ΜΠΑΝΑΝΑ", "ΚΕΡΑΣΙ", "ΡΟΔΑΚΙΝΟ", "ΔΑΜΑΣΚΗΝΟ")),
        "pl" to listOf(listOf("KRZESLO", "SCIANA", "STOL", "DRZWI", "ZEGAR", "OKNO", "PODLOGA", "POKOJ"), listOf("JABLKO", "GRUSZKA", "BANAN", "WISNIA", "BRZOSKWINIA", "SLIWKA"))
    )
    private fun activeWordBank(): List<List<String>> = if (languageCode == "tr") wordBank else localizedWordBanks[languageCode] ?: localizedWordBanks["en"]!!
    private fun letterAlphabet(): String = when (languageCode) {
        "tr" -> "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ"
        "ru" -> "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЫЭЮЯ"
        "el" -> "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ"
        "ar" -> "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"
        "zh" -> "天地人大小上下左右日月水火木金土山川口手目心"
        "ja" -> "アイウエオカキクケコサシスセソタチツテトナニヌネノ"
        "ko" -> "가나다라마바사아자차카타파하"
        "hi" -> "अआइईउऊएऐओऔकखगघचछजझटठडढतथदधनपफबभमयरलवसह"
        else -> "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }

    private var languageCode = "tr"
    private val languageNames = linkedMapOf(
        "tr" to "Türkçe", "en" to "English", "de" to "Deutsch", "fr" to "Français",
        "es" to "Español", "ar" to "العربية", "ru" to "Русский", "it" to "Italiano",
        "pt" to "Português", "zh" to "中文", "ja" to "日本語", "ko" to "한국어",
        "hi" to "हिन्दी", "nl" to "Nederlands", "el" to "Ελληνικά", "pl" to "Polski"
    )
    private val uiText = mapOf(
      "en" to mapOf("settings" to "SETTINGS", "sound" to "🔊 Sound", "vibration" to "📳 Vibration", "menu" to "← MAIN MENU", "start" to "🎯  START GAME\nLevel", "race" to "🤖  RACE AGAINST COMPUTER", "levels" to "🗺️  LEVELS\n1 - 100", "achievements" to "🏆\nACHIEVEMENTS", "stats" to "📊\nSTATISTICS", "help" to "❓  HOW TO PLAY?", "exit" to "🚪  EXIT", "language" to "🌐 Language", "choose" to "Choose language", "cancel" to "CANCEL", "raceTitle" to "🤖 Choose difficulty", "easy" to "Easy — Computer finds words slowly", "normal" to "Normal — Medium speed", "hard" to "Hard — Computer finds words quickly", "subtitle" to "🔎 Find words • Complete levels • Earn coins", "note" to "Find the hidden words in each level.\nSelect letters in order to complete a word!"),
      "de" to mapOf("settings" to "EINSTELLUNGEN", "sound" to "🔊 Töne", "vibration" to "📳 Vibration", "menu" to "← HAUPTMENÜ", "start" to "🎯  SPIEL STARTEN\nLevel", "race" to "🤖  GEGEN COMPUTER SPIELEN", "levels" to "🗺️  LEVELS\n1 - 100", "achievements" to "🏆\nERFOLGE", "stats" to "📊\nSTATISTIK", "help" to "❓  SPIELANLEITUNG", "exit" to "🚪  BEENDEN", "language" to "🌐 Sprache", "choose" to "Sprache wählen", "cancel" to "ABBRECHEN", "raceTitle" to "🤖 Schwierigkeit wählen", "easy" to "Einfach — Computer langsam", "normal" to "Normal — Mittleres Tempo", "hard" to "Schwer — Computer schnell", "subtitle" to "🔎 Wörter finden • Level schaffen • Münzen verdienen", "note" to "Finde die versteckten Wörter in jedem Level.\nWähle Buchstaben der Reihe nach!"),
      "fr" to mapOf("settings" to "PARAMÈTRES", "sound" to "🔊 Sons", "vibration" to "📳 Vibration", "menu" to "← MENU PRINCIPAL", "start" to "🎯  JOUER\nNiveau", "race" to "🤖  COURSE CONTRE L'ORDINATEUR", "levels" to "🗺️  NIVEAUX\n1 - 100", "achievements" to "🏆\nSUCCÈS", "stats" to "📊\nSTATISTIQUES", "help" to "❓  COMMENT JOUER ?", "exit" to "🚪  QUITTER", "language" to "🌐 Langue", "choose" to "Choisir la langue", "cancel" to "ANNULER", "raceTitle" to "🤖 Choisir la difficulté", "easy" to "Facile — ordinateur lent", "normal" to "Normal — vitesse moyenne", "hard" to "Difficile — ordinateur rapide", "subtitle" to "🔎 Trouvez des mots • Terminez les niveaux • Gagnez des pièces", "note" to "Trouvez les mots cachés dans chaque niveau.\nSélectionnez les lettres dans l'ordre !"),
      "es" to mapOf("settings" to "AJUSTES", "sound" to "🔊 Sonidos", "vibration" to "📳 Vibración", "menu" to "← MENÚ PRINCIPAL", "start" to "🎯  JUGAR\nNivel", "race" to "🤖  COMPETIR CONTRA EL ORDENADOR", "levels" to "🗺️  NIVELES\n1 - 100", "achievements" to "🏆\nLOGROS", "stats" to "📊\nESTADÍSTICAS", "help" to "❓  ¿CÓMO JUGAR?", "exit" to "🚪  SALIR", "language" to "🌐 Idioma", "choose" to "Elegir idioma", "cancel" to "CANCELAR", "raceTitle" to "🤖 Elegir dificultad", "easy" to "Fácil — ordenador lento", "normal" to "Normal — velocidad media", "hard" to "Difícil — ordenador rápido", "subtitle" to "🔎 Encuentra palabras • Completa niveles • Gana monedas", "note" to "Encuentra las palabras ocultas en cada nivel.\n¡Selecciona las letras en orden!"),
      "it" to mapOf("settings" to "IMPOSTAZIONI", "sound" to "🔊 Suoni", "vibration" to "📳 Vibrazione", "menu" to "← MENU PRINCIPALE", "start" to "🎯  INIZIA\nLivello", "race" to "🤖  SFIDA IL COMPUTER", "levels" to "🗺️  LIVELLI\n1 - 100", "achievements" to "🏆\nOBIETTIVI", "stats" to "📊\nSTATISTICHE", "help" to "❓  COME SI GIOCA?", "exit" to "🚪  ESCI", "language" to "🌐 Lingua", "choose" to "Scegli lingua", "cancel" to "ANNULLA", "raceTitle" to "🤖 Scegli difficoltà", "easy" to "Facile — computer lento", "normal" to "Normale — velocità media", "hard" to "Difficile — computer veloce", "subtitle" to "🔎 Trova parole • Completa livelli • Guadagna monete", "note" to "Trova le parole nascoste in ogni livello.\nSeleziona le lettere in ordine!"),
      "pt" to mapOf("settings" to "CONFIGURAÇÕES", "sound" to "🔊 Sons", "vibration" to "📳 Vibração", "menu" to "← MENU PRINCIPAL", "start" to "🎯  JOGAR\nNível", "race" to "🤖  COMPETIR CONTRA O COMPUTADOR", "levels" to "🗺️  NÍVEIS\n1 - 100", "achievements" to "🏆\nCONQUISTAS", "stats" to "📊\nESTATÍSTICAS", "help" to "❓  COMO JOGAR?", "exit" to "🚪  SAIR", "language" to "🌐 Idioma", "choose" to "Escolher idioma", "cancel" to "CANCELAR", "raceTitle" to "🤖 Escolher dificuldade", "easy" to "Fácil — computador lento", "normal" to "Normal — velocidade média", "hard" to "Difícil — computador rápido", "subtitle" to "🔎 Encontre palavras • Complete níveis • Ganhe moedas", "note" to "Encontre as palavras escondidas em cada nível.\nSelecione as letras em ordem!"),
      "nl" to mapOf("settings" to "INSTELLINGEN", "sound" to "🔊 Geluid", "vibration" to "📳 Trillen", "menu" to "← HOOFDMENU", "start" to "🎯  SPELEN\nLevel", "race" to "🤖  SPEEL TEGEN DE COMPUTER", "levels" to "🗺️  LEVELS\n1 - 100", "achievements" to "🏆\nPRESTATIES", "stats" to "📊\nSTATISTIEKEN", "help" to "❓  HOE SPEEL JE?", "exit" to "🚪  AFSLUITEN", "language" to "🌐 Taal", "choose" to "Kies taal", "cancel" to "ANNULEREN", "raceTitle" to "🤖 Kies moeilijkheid", "easy" to "Makkelijk — computer langzaam", "normal" to "Normaal — gemiddelde snelheid", "hard" to "Moeilijk — computer snel", "subtitle" to "🔎 Vind woorden • Voltooi levels • Verdien munten", "note" to "Vind de verborgen woorden in elk level.\nSelecteer letters op volgorde!"),
      "pl" to mapOf("settings" to "USTAWIENIA", "sound" to "🔊 Dźwięki", "vibration" to "📳 Wibracje", "menu" to "← MENU GŁÓWNE", "start" to "🎯  GRAJ\nPoziom", "race" to "🤖  WYŚCIG Z KOMPUTEREM", "levels" to "🗺️  POZIOMY\n1 - 100", "achievements" to "🏆\nOSIĄGNIĘCIA", "stats" to "📊\nSTATYSTYKI", "help" to "❓  JAK GRAĆ?", "exit" to "🚪  WYJDŹ", "language" to "🌐 Język", "choose" to "Wybierz język", "cancel" to "ANULUJ", "raceTitle" to "🤖 Wybierz poziom trudności", "easy" to "Łatwy — wolny komputer", "normal" to "Normalny — średnie tempo", "hard" to "Trudny — szybki komputer", "subtitle" to "🔎 Znajduj słowa • Ukończ poziomy • Zdobywaj monety", "note" to "Znajdź ukryte słowa na każdym poziomie.\nWybieraj litery po kolei!"),
      "ru" to mapOf("settings" to "НАСТРОЙКИ", "sound" to "🔊 Звуки", "vibration" to "📳 Вибрация", "menu" to "← ГЛАВНОЕ МЕНЮ", "start" to "🎯  ИГРАТЬ\nУровень", "race" to "🤖  СОРЕВНОВАНИЕ С КОМПЬЮТЕРОМ", "levels" to "🗺️  УРОВНИ\n1 - 100", "achievements" to "🏆\nДОСТИЖЕНИЯ", "stats" to "📊\nСТАТИСТИКА", "help" to "❓  КАК ИГРАТЬ?", "exit" to "🚪  ВЫХОД", "language" to "🌐 Язык", "choose" to "Выберите язык", "cancel" to "ОТМЕНА", "raceTitle" to "🤖 Выберите сложность", "easy" to "Легко — компьютер медленный", "normal" to "Нормально — средняя скорость", "hard" to "Сложно — компьютер быстрый", "subtitle" to "🔎 Ищите слова • Проходите уровни • Зарабатывайте монеты", "note" to "Найдите скрытые слова на каждом уровне.\nВыбирайте буквы по порядку!"),
      "ar" to mapOf("settings" to "الإعدادات", "sound" to "🔊 الأصوات", "vibration" to "📳 الاهتزاز", "menu" to "← القائمة الرئيسية", "start" to "🎯  ابدأ اللعب\nالمستوى", "race" to "🤖  تسابق ضد الكمبيوتر", "levels" to "🗺️  المستويات\n1 - 100", "achievements" to "🏆\nالإنجازات", "stats" to "📊\nالإحصائيات", "help" to "❓  طريقة اللعب", "exit" to "🚪  خروج", "language" to "🌐 اللغة", "choose" to "اختر اللغة", "cancel" to "إلغاء", "raceTitle" to "🤖 اختر الصعوبة", "easy" to "سهل — الكمبيوتر بطيء", "normal" to "عادي — سرعة متوسطة", "hard" to "صعب — الكمبيوتر سريع", "subtitle" to "🔎 ابحث عن الكلمات • أكمل المراحل • اجمع العملات", "note" to "اعثر على الكلمات المخفية في كل مستوى.\nاختر الحروف بالترتيب!"),
      "zh" to mapOf("settings" to "设置", "sound" to "🔊 音效", "vibration" to "📳 震动", "menu" to "← 主菜单", "start" to "🎯  开始游戏\n关卡", "race" to "🤖  与电脑比赛", "levels" to "🗺️ 关卡\n1 - 100", "achievements" to "🏆\n成就", "stats" to "📊\n统计", "help" to "❓  如何游戏？", "exit" to "🚪 退出", "language" to "🌐 语言", "choose" to "选择语言", "cancel" to "取消", "raceTitle" to "🤖 选择难度", "easy" to "简单 — 电脑速度慢", "normal" to "普通 — 中等速度", "hard" to "困难 — 电脑速度快", "subtitle" to "🔎 找单词 • 完成关卡 • 赢取金币", "note" to "找出每一关隐藏的单词。\n按顺序选择字母！"),
      "ja" to mapOf("settings" to "設定", "sound" to "🔊 サウンド", "vibration" to "📳 振動", "menu" to "← メインメニュー", "start" to "🎯  ゲーム開始\nレベル", "race" to "🤖  コンピューターと対戦", "levels" to "🗺️ レベル\n1 - 100", "achievements" to "🏆\n実績", "stats" to "📊\n統計", "help" to "❓  遊び方", "exit" to "🚪 終了", "language" to "🌐 言語", "choose" to "言語を選択", "cancel" to "キャンセル", "raceTitle" to "🤖 難易度を選択", "easy" to "簡単 — コンピューターは遅い", "normal" to "普通 — 中速", "hard" to "難しい — コンピューターは速い", "subtitle" to "🔎 単語を探す • レベルをクリア • コインを獲得", "note" to "各レベルの隠れた単語を見つけよう。\n文字を順番に選択！"),
      "ko" to mapOf("settings" to "설정", "sound" to "🔊 소리", "vibration" to "📳 진동", "menu" to "← 메인 메뉴", "start" to "🎯  게임 시작\n레벨", "race" to "🤖  컴퓨터와 대결", "levels" to "🗺️ 레벨\n1 - 100", "achievements" to "🏆\n업적", "stats" to "📊\n통계", "help" to "❓  게임 방법", "exit" to "🚪 종료", "language" to "🌐 언어", "choose" to "언어 선택", "cancel" to "취소", "raceTitle" to "🤖 난이도 선택", "easy" to "쉬움 — 컴퓨터 느림", "normal" to "보통 — 중간 속도", "hard" to "어려움 — 컴퓨터 빠름", "subtitle" to "🔎 단어 찾기 • 레벨 완료 • 코인 획득", "note" to "각 레벨에서 숨겨진 단어를 찾으세요.\n문자를 순서대로 선택하세요!"),
      "hi" to mapOf("settings" to "सेटिंग्स", "sound" to "🔊 ध्वनि", "vibration" to "📳 कंपन", "menu" to "← मुख्य मेनू", "start" to "🎯  खेल शुरू करें\nस्तर", "race" to "🤖  कंप्यूटर के विरुद्ध खेलें", "levels" to "🗺️ स्तर\n1 - 100", "achievements" to "🏆\nउपलब्धियाँ", "stats" to "📊\nआँकड़े", "help" to "❓  कैसे खेलें?", "exit" to "🚪 बाहर निकलें", "language" to "🌐 भाषा", "choose" to "भाषा चुनें", "cancel" to "रद्द करें", "raceTitle" to "🤖 कठिनाई चुनें", "easy" to "आसान — धीमा कंप्यूटर", "normal" to "सामान्य — मध्यम गति", "hard" to "कठिन — तेज़ कंप्यूटर", "subtitle" to "🔎 शब्द खोजें • स्तर पूरे करें • सिक्के कमाएँ", "note" to "हर स्तर में छिपे शब्द खोजें।\nअक्षरों को क्रम से चुनें!"),
      "el" to mapOf("settings" to "ΡΥΘΜΙΣΕΙΣ", "sound" to "🔊 Ήχοι", "vibration" to "📳 Δόνηση", "menu" to "← ΚΥΡΙΟ ΜΕΝΟΥ", "start" to "🎯  ΕΝΑΡΞΗ\nΕπίπεδο", "race" to "🤖  ΑΝΤΑΓΩΝΙΣΜΟΣ ΜΕ ΥΠΟΛΟΓΙΣΤΗ", "levels" to "🗺️ ΕΠΙΠΕΔΑ\n1 - 100", "achievements" to "🏆\nΕΠΙΤΕΥΓΜΑΤΑ", "stats" to "📊\nΣΤΑΤΙΣΤΙΚΑ", "help" to "❓  ΠΩΣ ΠΑΙΖΕΤΑΙ;", "exit" to "🚪 ΕΞΟΔΟΣ", "language" to "🌐 Γλώσσα", "choose" to "Επιλογή γλώσσας", "cancel" to "ΑΚΥΡΟ", "raceTitle" to "🤖 Επιλογή δυσκολίας", "easy" to "Εύκολο — αργός υπολογιστής", "normal" to "Κανονικό — μέτρια ταχύτητα", "hard" to "Δύσκολο — γρήγορος υπολογιστής", "subtitle" to "🔎 Βρες λέξεις • Ολοκλήρωσε επίπεδα • Κέρδισε νομίσματα", "note" to "Βρες τις κρυμμένες λέξεις σε κάθε επίπεδο.\nΕπίλεξε τα γράμματα με τη σειρά!")
    )
    private fun tr(key: String, fallback: String): String =
        uiText[languageCode]?.get(key) ?: fallback

    private fun chooseLanguage() {
        val codes = languageNames.keys.toList()
        AlertDialog.Builder(this)
            .setTitle(tr("choose", "Dil seç"))
            .setSingleChoiceItems(languageNames.values.toTypedArray(), codes.indexOf(languageCode)) { dialog, which ->
                languageCode = codes[which]
                prefs.edit().putString("languageCode", languageCode).apply()
                dialog.dismiss()
                showSettings()
            }
            .setNegativeButton(tr("cancel", "İPTAL"), null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        languageCode = prefs.getString("languageCode", "tr") ?: "tr"
        MobileAds.initialize(this) {}
        showMenu()
    }

    override fun onDestroy() {
        stopRace()
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        showMenu()
    }

    private fun load() {
        maxUnlocked = prefs.getInt("maxUnlocked", prefs.getInt("level", 1)).coerceIn(1, 100)
        level = prefs.getInt("currentLevel", maxUnlocked).coerceIn(1, maxUnlocked)
        score = prefs.getInt("score", 0)
        coins = prefs.getInt("coins", 50)
        lives = prefs.getInt("lives", 5)
        combo = prefs.getInt("combo", 0)
        soundEnabled = prefs.getBoolean("soundEnabled", true)
        vibrationEnabled = prefs.getBoolean("vibrationEnabled", true)
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    }

    private fun save() {
        prefs.edit().putInt("level", maxUnlocked).putInt("maxUnlocked", maxUnlocked).putInt("currentLevel", level).putInt("score", score).putInt("coins", coins).putInt("lives", lives).putInt("combo", combo).apply()
    }

    private fun base(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(8))
        setBackgroundColor(Color.rgb(248, 246, 252))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Float = 18f): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius.toInt()).toFloat()
    }

    private fun gameButton(text: String, action: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 17f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(48, 35, 60))
        background = rounded(Color.WHITE, 16f)
        elevation = dp(2).toFloat()
        setPadding(dp(10), 0, dp(10), 0)
        isClickable = true
        setOnClickListener { action() }
    }

    private fun title(text: String, size: Float = 28f): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(78, 43, 126))
    }

    private fun addAd() {
        val ad = AdView(this).apply {
            adUnitId = "ca-app-pub-2692425898190134/1634647228"
            setAdSize(AdSize.BANNER)
            loadAd(AdRequest.Builder().build())
        }
        root.addView(ad, LinearLayout.LayoutParams(-1, dp(50)))
    }


    private fun totalFoundWords(): Int = prefs.getInt("totalFoundWords", 0)
    private fun completedLevels(): Int = prefs.getInt("completedLevels", 0)
    private fun bestCombo(): Int = prefs.getInt("bestCombo", 0)

    private fun achievementUnlocked(id: String): Boolean =
        prefs.getBoolean("achievement_$id", false)

    private fun unlockAchievement(id: String, title: String, reward: Int) {
        if (achievementUnlocked(id)) return
        prefs.edit()
            .putBoolean("achievement_$id", true)
            .putInt("coins", coins + reward)
            .apply()
        coins += reward
        Toast.makeText(this, "🏆 Başarı açıldı: $title\n+$reward altın", Toast.LENGTH_LONG).show()
    }

    private fun updateAchievements() {
        val words = totalFoundWords()
        val completed = completedLevels()
        val comboNow = bestCombo()
        if (words >= 1) unlockAchievement("first_word", "İlk Kelime", 10)
        if (words >= 10) unlockAchievement("ten_words", "10 Kelime", 20)
        if (words >= 50) unlockAchievement("fifty_words", "50 Kelime", 40)
        if (words >= 100) unlockAchievement("hundred_words", "100 Kelime", 75)
        if (completed >= 10) unlockAchievement("ten_levels", "10 Bölüm", 30)
        if (completed >= 25) unlockAchievement("twentyfive_levels", "25 Bölüm", 60)
        if (completed >= 50) unlockAchievement("fifty_levels", "50 Bölüm", 100)
        if (completed >= 100) unlockAchievement("hundred_levels", "100 Bölüm", 200)
        if (comboNow >= 5) unlockAchievement("combo_five", "5'li Seri", 25)
        if (comboNow >= 10) unlockAchievement("combo_ten", "10'lu Seri", 60)
    }

    private fun playFeedback(success: Boolean) {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(if (success) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 120)
            } catch (_: Exception) {}
        }
        if (vibrationEnabled) {
            try {
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION") vibrator.vibrate(if (success) 45L else 70L)
            } catch (_: Exception) {}
        }
    }

    private fun showSettings() {
        root = base()
        root.addView(title("⚙️ " + tr("settings", "AYARLAR"), 28f), LinearLayout.LayoutParams(-1, dp(55)))
        val sound = Switch(this).apply {
            text = tr("sound", "🔊 Sesler")
            textSize = 18f
            isChecked = soundEnabled
            setPadding(dp(12), 0, dp(12), 0)
            setOnCheckedChangeListener { _, checked ->
                soundEnabled = checked
                prefs.edit().putBoolean("soundEnabled", checked).apply()
                if (checked) playFeedback(true)
            }
        }
        root.addView(sound, LinearLayout.LayoutParams(-1, dp(60)).apply { setMargins(0, dp(8), 0, dp(6)) })
        val vibration = Switch(this).apply {
            text = tr("vibration", "📳 Titreşim")
            textSize = 18f
            isChecked = vibrationEnabled
            setPadding(dp(12), 0, dp(12), 0)
            setOnCheckedChangeListener { _, checked ->
                vibrationEnabled = checked
                prefs.edit().putBoolean("vibrationEnabled", checked).apply()
                if (checked) playFeedback(true)
            }
        }
        root.addView(vibration, LinearLayout.LayoutParams(-1, dp(60)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(TextView(this).apply {
            text = "Yanlışlıkla bir harfe dokunup kelimeyi tamamlamadığında artık can veya puan kaybetmezsin. Geçersiz seçim sadece temizlenir."
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setTextColor(Color.DKGRAY)
            background = rounded(Color.WHITE, 16f)
        }, LinearLayout.LayoutParams(-1, dp(110)).apply { setMargins(0, 0, 0, dp(10)) })
        root.addView(gameButton(tr("language", "🌐 Dil: ") + " " + (languageNames[languageCode] ?: "Türkçe")) { chooseLanguage() }, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(gameButton(tr("menu", "← ANA MENÜ")) { showMenu() }, LinearLayout.LayoutParams(-1, dp(52)))
        setContentView(root)
    }

    private fun showAchievements() {
        root = base()
        root.addView(title("🏆 BAŞARILAR", 28f), LinearLayout.LayoutParams(-1, dp(55)))
        val words = totalFoundWords()
        val completed = completedLevels()
        val comboBest = bestCombo()

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val achievements = listOf(
            Triple("first_word", "İlk Kelime", "İlk kelimeyi bul • +10 altın"),
            Triple("ten_words", "10 Kelime", "Toplam 10 kelime bul • +20 altın"),
            Triple("fifty_words", "50 Kelime", "Toplam 50 kelime bul • +40 altın"),
            Triple("hundred_words", "100 Kelime", "Toplam 100 kelime bul • +75 altın"),
            Triple("ten_levels", "10 Bölüm", "10 bölüm tamamla • +30 altın"),
            Triple("twentyfive_levels", "25 Bölüm", "25 bölüm tamamla • +60 altın"),
            Triple("fifty_levels", "50 Bölüm", "50 bölüm tamamla • +100 altın"),
            Triple("hundred_levels", "100 Bölüm", "100 bölüm tamamla • +200 altın"),
            Triple("combo_five", "5'li Seri", "5 kelimeyi arka arkaya bul • +25 altın"),
            Triple("combo_ten", "10'lu Seri", "10 kelimeyi arka arkaya bul • +60 altın")
        )
        achievements.forEach { (id, name, desc) ->
            val unlocked = achievementUnlocked(id)
            val t = TextView(this).apply {
                text = if (unlocked) "🏆 $name\n$desc\n✓ TAMAMLANDI" else "🔒 $name\n$desc"
                textSize = 14f
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setTextColor(if (unlocked) Color.rgb(40,130,75) else Color.DKGRAY)
                background = rounded(if (unlocked) Color.rgb(225,247,231) else Color.WHITE, 14f)
            }
            list.addView(t, LinearLayout.LayoutParams(-1, dp(68)).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        val scroll = ScrollView(this)
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(gameButton("← ANA MENÜ") { showMenu() }, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(6), 0, 0) })
        setContentView(root)
    }

    private fun showStats() {
        root = base()
        root.addView(title("📊 İSTATİSTİKLER", 28f), LinearLayout.LayoutParams(-1, dp(55)))
        val totalStars = (1..100).sumOf { getStars(it) }
        val t = TextView(this).apply {
            text = "🏆 Toplam Puan\n$score\n\n🪙 Altın\n$coins\n\n🔎 Bulunan Kelime\n${totalFoundWords()}\n\n🗺️ Tamamlanan Bölüm\n${completedLevels()}/100\n\n⭐ Toplam Yıldız\n$totalStars/300\n\n🔥 En Uzun Seri\n${bestCombo()}\n\n🔓 Açılan Bölüm\n$maxUnlocked/100"
            textSize = 19f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(65,45,85))
            background = rounded(Color.WHITE, 18f)
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }
        root.addView(t, LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(0, dp(8), 0, dp(8)) })
        root.addView(gameButton("← ANA MENÜ") { showMenu() }, LinearLayout.LayoutParams(-1, dp(52)))
        setContentView(root)
    }

    private fun showMenu() {
        stopRace()
        raceMode = false
        root = base()
        root.addView(title("🧠 ZİHİN KUTUSU", 30f), LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(TextView(this).apply {
            text = tr("subtitle", "🔎 Kelimeleri bul • Bölümleri tamamla • Altın kazan")
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        }, LinearLayout.LayoutParams(-1, dp(42)))

        val stats = TextView(this).apply {
            text = "🏆 $score puan     🪙 $coins altın     ❤️ $lives can"
            textSize = 15f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(108, 63, 199), 18f)
        }
        root.addView(stats, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(4), 0, dp(10)) })

        root.addView(gameButton(tr("start", "🎯  OYUNA BAŞLA\nBölüm") + " $level" ) { raceMode = false; startGame() }, LinearLayout.LayoutParams(-1, dp(68)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(gameButton(tr("race", "🤖  BİLGİSAYARA KARŞI YARIŞ") ) { chooseRaceDifficulty() }, LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(gameButton(tr("levels", "🗺️  BÖLÜMLER\n1 - 100") ) { showLevels() }, LinearLayout.LayoutParams(-1, dp(68)).apply { setMargins(0, 0, 0, dp(8)) })
        val infoRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        infoRow.addView(gameButton(tr("achievements", "🏆\nBAŞARILAR")) { showAchievements() }, LinearLayout.LayoutParams(0, dp(62), 1f).apply { setMargins(0, 0, dp(4), dp(8)) })
        infoRow.addView(gameButton(tr("stats", "📊\nİSTATİSTİK")) { showStats() }, LinearLayout.LayoutParams(0, dp(62), 1f).apply { setMargins(dp(4), 0, 0, dp(8)) })
        root.addView(infoRow)

        root.addView(gameButton("⚙️  " + tr("settings", "AYARLAR") ) { showSettings() }, LinearLayout.LayoutParams(-1, dp(60)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(gameButton(tr("help", "❓  NASIL OYNANIR?") ) { help() }, LinearLayout.LayoutParams(-1, dp(60)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(gameButton(tr("exit", "🚪  ÇIKIŞ") ) { exitGame() }, LinearLayout.LayoutParams(-1, dp(58)))

        val note = TextView(this).apply {
            text = tr("note", "Her bölümde gizlenmiş kelimeleri bul.\nHarfleri sırayla seç ve kelimeyi tamamla!")
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.GRAY)
        }
        root.addView(note, LinearLayout.LayoutParams(-1, dp(65)))
        addAd()
        setContentView(root)
    }

    private fun chooseRaceDifficulty() {
        val choices = arrayOf(tr("easy", "Kolay — Bilgisayar yavaş kelime bulur"), tr("normal", "Normal — Orta hızda yarış"), tr("hard", "Zor — Bilgisayar hızlı kelime bulur"))
        AlertDialog.Builder(this).setTitle(tr("raceTitle", "🤖 Bilgisayar zorluğunu seç"))
            .setItems(choices) { _, which ->
                raceDifficulty = when (which) { 0 -> "Kolay"; 1 -> "Normal"; else -> "Zor" }
                raceMode = true
                computerFound = 0
                startGame()
            }.setNegativeButton(tr("cancel", "İPTAL"), null).show()
    }

    private fun stopRace() {
        raceRunnable?.let { raceHandler.removeCallbacks(it) }
        raceRunnable = null
    }

    private fun startComputerRace() {
        stopRace()
        val p = currentPuzzle ?: return
        val delayMs = when (raceDifficulty) { "Kolay" -> 12000L; "Zor" -> 3500L; else -> 7000L }
        raceStatusText?.text = "🤖 Bilgisayar ($raceDifficulty): $computerFound/${p.words.size} kelime"
        val task = object : Runnable {
            override fun run() {
                val puzzle = currentPuzzle ?: return
                if (!raceMode || found.size >= puzzle.words.size) return
                computerFound = (computerFound + 1).coerceAtMost(puzzle.words.size)
                raceStatusText?.text = "🤖 Bilgisayar ($raceDifficulty): $computerFound/${puzzle.words.size} kelime"
                if (computerFound >= puzzle.words.size) {
                    stopRace()
                    AlertDialog.Builder(this@MainActivity).setTitle("🤖 Bilgisayar kazandı!")
                        .setMessage("Bilgisayar $raceDifficulty seviyesinde senden önce tüm kelimeleri buldu. Tekrar deneyebilirsin!")
                        .setPositiveButton("TEKRAR YARIŞ") { _, _ -> computerFound = 0; raceMode = true; startGame() }
                        .setNegativeButton("ANA MENÜ") { _, _ -> showMenu() }.setCancelable(false).show()
                } else raceHandler.postDelayed(this, delayMs)
            }
        }
        raceRunnable = task
        raceHandler.postDelayed(task, delayMs)
    }

    private fun showLevels() {
        root = base()
        root.addView(title("🗺️ BÖLÜMLER", 28f), LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(TextView(this).apply {
            text = "Açılan bölümler: 1 - $maxUnlocked"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }, LinearLayout.LayoutParams(-1, dp(26)))

        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (i in 1..100) {
            val unlocked = i <= maxUnlocked
            val stars = getStars(i)
            val starText = if (stars > 0) "  ${"⭐".repeat(stars)}" else if (i < maxUnlocked) "  ✓" else ""
            val b = gameButton("${if (unlocked) "🔓" else "🔒"}  Bölüm $i$starText") {
                if (unlocked) {
                    level = i
                    save()
                    startGame()
                } else Toast.makeText(this, "Önceki bölümü tamamla.", Toast.LENGTH_SHORT).show()
            }
            box.addView(b, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        scroll.addView(box)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(gameButton("← ANA MENÜ") { showMenu() }, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(6), 0, 0) })
        setContentView(root)
    }

    private fun exitGame() {
        AlertDialog.Builder(this)
            .setTitle("🚪 Oyundan Çıkış")
            .setMessage("Zihin Kutusu'ndan çıkmak istiyor musunuz?")
            .setNegativeButton("HAYIR", null)
            .setPositiveButton("EVET") { _, _ ->
                save()
                finishAffinity()
            }
            .show()
    }

    private fun help() {
        AlertDialog.Builder(this)
            .setTitle("🔎 Nasıl Oynanır?")
            .setMessage("1. Üstte istenen kelimeleri gör.\n\n2. Parmağını ilk harfin üzerine koy ve kaldırmadan kelimenin harfleri üzerinden kaydır. Yan yana ve çapraz harfler seçilebilir.\n\n3. Parmağını kaldırınca kelime otomatik kontrol edilir.\n\n4. Puan; kelime uzunluğu, arka arkaya bulma serisi ve hızlı çözüm bonusuna göre artar.\n\n5. İpucu 10 altın karşılığında bir kelimenin ilk harfini gösterir.\n\nTüm kelimeleri bulunca bölüm tamamlanır.")
            .setPositiveButton("TAMAM", null).show()
    }

    private fun startGame() {
        stopRace()
        if (raceMode) computerFound = 0
        found = mutableSetOf()
        selectedCells = mutableListOf()
        hintCell = -1
        combo = 0
        wrongAttempts = 0
        levelStartTime = System.currentTimeMillis()
        currentPuzzle = createPuzzle(level)
        root = base()

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(gameButton("←\nGERİ") { showLevels() }, LinearLayout.LayoutParams(dp(68), dp(44)).apply { setMargins(0, 0, dp(4), 0) })
        infoText = title(if (raceMode) "🤖 BİLGİSAYARA KARŞI" else "Bölüm $level", 16f)
        top.addView(infoText, LinearLayout.LayoutParams(0, dp(44), 1f))
        top.addView(gameButton("⌂\nANA MENÜ") { showMenu() }, LinearLayout.LayoutParams(dp(96), dp(44)).apply { setMargins(dp(4), 0, 0, 0) })
        root.addView(top)

        livesText = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.DKGRAY)
        }
        root.addView(livesText, LinearLayout.LayoutParams(-1, dp(32)))
        raceStatusText = if (raceMode) TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(108, 63, 199))
        }.also { root.addView(it, LinearLayout.LayoutParams(-1, dp(26))) } else null

        root.addView(TextView(this).apply {
            text = "BULUNACAK KELİMELER"
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(108, 63, 199))
        }, LinearLayout.LayoutParams(-1, dp(24)))

        wordListBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        root.addView(wordListBox, LinearLayout.LayoutParams(-1, dp(38)))

        grid = WordGridView(this)
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(0, dp(4), 0, dp(4)) })

        selectedText = TextView(this).apply {
            text = "Seçilen: —"
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(78, 43, 126))
            background = rounded(Color.WHITE, 14f)
        }
        root.addView(selectedText, LinearLayout.LayoutParams(-1, dp(36)).apply { setMargins(0, 0, 0, dp(4)) })

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bar.addView(gameButton("🧹\nTEMİZLE") { clearSelection() }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(0, 0, dp(4), 0) })
        bar.addView(gameButton("💡\nİPUCU") { hint() }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(dp(4), 0, 0, 0) })
        root.addView(bar)
        root.addView(gameButton("✓  KELİMEYİ KONTROL ET") { checkWord() }, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0, dp(4), 0, 0) })

        val navigation = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        navigation.addView(gameButton("← OYUNA GERİ DÖN") { startGame() }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(0, dp(5), dp(4), 0) })
        navigation.addView(gameButton("⌂ ANA MENÜ") { showMenu() }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(4), dp(5), dp(4), 0) })
        navigation.addView(gameButton("🚪 ÇIKIŞ") { exitGame() }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(4), dp(5), 0, 0) })
        root.addView(navigation)
        addAd()
        setContentView(root)
        renderPuzzle()
        if (raceMode) startComputerRace()
    }

    private fun renderPuzzle() {
        val p = currentPuzzle ?: return
        infoText.text = "Bölüm $level   •   🏆 $score   •   🪙 $coins"
        livesText.text = "❤️ $lives can   •   Bulunan ${found.size}/${p.words.size}"
        wordListBox.removeAllViews()
        p.words.forEach { word ->
            val t = TextView(this).apply {
                text = if (found.contains(word)) "✓ $word" else word
                textSize = 13f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (found.contains(word)) Color.rgb(40, 130, 75) else Color.DKGRAY)
                background = rounded(if (found.contains(word)) Color.rgb(218, 246, 226) else Color.WHITE, 12f)
                setPadding(dp(6), 0, dp(6), 0)
            }
            wordListBox.addView(t, LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }

        grid.setPuzzle(p)
        grid.invalidate()
        selectedText.text = "Seçilen: " + selectedCells.joinToString("") { idx -> p.grid[idx / GRID_SIZE][idx % GRID_SIZE].toString() }.ifEmpty { "—" }
        save()
    }

    private fun isFoundCell(index: Int): Boolean {
        val p = currentPuzzle ?: return false
        return p.words.filter { found.contains(it) }.any { word ->
            findWordCells(p.grid, word).contains(index)
        }
    }

    private var swipeDr = 0
    private var swipeDc = 0

    private fun selectCell(index: Int) {
        if (selectedCells.contains(index)) return
        if (selectedCells.size >= 12) return

        val p = currentPuzzle ?: return
        if (selectedCells.isNotEmpty()) {
            val last = selectedCells.last()
            val lr = last / GRID_SIZE; val lc = last % GRID_SIZE
            val r = index / GRID_SIZE; val c = index % GRID_SIZE
            val dr = r - lr; val dc = c - lc
            if (dr !in -1..1 || dc !in -1..1 || (dr == 0 && dc == 0)) return
            if (selectedCells.size == 1) {
                swipeDr = dr
                swipeDc = dc
            } else if (dr != swipeDr || dc != swipeDc) {
                return
            }
        } else {
            swipeDr = 0
            swipeDc = 0
        }
        val candidateCells = selectedCells + index
        val candidate = candidateCells.joinToString("") { idx -> p.grid[idx / GRID_SIZE][idx % GRID_SIZE].toString() }
        val stillPossible = p.words.any { word ->
            !found.contains(word) && (word.startsWith(candidate) || word.endsWith(candidate.reversed()))
        }
        if (!stillPossible) {
            // Parmak komşu bir kelimeye taşarsa seçim genişlemez. Ceza yok.
            return
        }
        selectedCells.add(index)
        hintCell = -1
        updateSelectionText()
        grid.invalidate()
    }

    private fun finishSwipeSelection() {
        val p = currentPuzzle ?: return
        if (selectedCells.isEmpty()) return
        val selected = selectedCells.joinToString("") { idx -> p.grid[idx / GRID_SIZE][idx % GRID_SIZE].toString() }
        if (p.words.contains(selected) && !found.contains(selected)) {
            acceptWord(selected)
        } else {
            // Dokunmatik ekranda yanlışlıkla yapılan seçimler artık ceza vermez.
            // Sadece seçimi temizleyip oyuncunun devam etmesine izin veriyoruz.
            selectedCells.clear()
            swipeDr = 0
            swipeDc = 0
            playFeedback(false)
            renderPuzzle()
        }
    }

    private fun updateSelectionText() {
        val p = currentPuzzle ?: return
        selectedText.text = "Seçilen: " + selectedCells.joinToString("") { idx -> p.grid[idx / GRID_SIZE][idx % GRID_SIZE].toString() }.ifEmpty { "—" }
    }

    private fun acceptWord(word: String) {
        val p = currentPuzzle ?: return
        found.add(word)
        combo += 1
        val newTotalWords = totalFoundWords() + 1
        val newBestCombo = maxOf(bestCombo(), combo)
        prefs.edit()
            .putInt("totalFoundWords", newTotalWords)
            .putInt("bestCombo", newBestCombo)
            .apply()
        val lengthBonus = (word.length - 3).coerceAtLeast(0) * 5
        val comboBonus = (combo - 1) * 5
        val timeBonus = if (System.currentTimeMillis() - levelStartTime < 45000) 10 else 0
        val earned = 20 + lengthBonus + comboBonus + timeBonus
        score += earned
        coins += 5 + (word.length / 4)
        selectedCells.clear()
        playFeedback(true)
        Toast.makeText(this, "🎉 $word bulundu! +$earned puan  •  🔥 Seri x$combo", Toast.LENGTH_SHORT).show()
        updateAchievements()
        if (found.size == p.words.size) {
            val elapsed = System.currentTimeMillis() - levelStartTime
            val stars = when {
                wrongAttempts == 0 && elapsed <= 45000 -> 3
                wrongAttempts <= 1 && elapsed <= 90000 -> 2
                else -> 1
            }
            val oldStars = getStars(level)
            if (stars > oldStars) prefs.edit().putInt("stars_$level", stars).apply()
            coins += 15 + stars * 5

            val completedLevel = level
            val newCompleted = if (prefs.getBoolean("completed_$completedLevel", false)) {
                completedLevels()
            } else {
                prefs.edit().putBoolean("completed_$completedLevel", true).apply()
                completedLevels() + 1
            }
            prefs.edit().putInt("completedLevels", newCompleted).apply()
            if (level >= maxUnlocked && level < 100) maxUnlocked = level + 1
            val nextLevel = (level + 1).coerceAtMost(100)
            level = nextLevel
            save()
            updateAchievements()
            val starsLine = "⭐".repeat(stars)
            val bestLine = if (stars > oldStars) "\nYeni rekor!" else ""
            stopRace()
            val resultTitle = if (raceMode) "🏆 Yarışı kazandın!" else "🏆 Bölüm Tamamlandı!"
            val resultMessage = if (raceMode) "Bilgisayarı ($raceDifficulty) yendin!\n$starsLine\n\n+${15 + stars * 5} altın"
                else "Bölüm $completedLevel\n$starsLine\n\n+${15 + stars * 5} altın$bestLine\n\nSıradaki bölüm: $level"
            AlertDialog.Builder(this)
                .setTitle(resultTitle)
                .setMessage(resultMessage)
                .setPositiveButton(if (raceMode) "TEKRAR YARIŞ" else "DEVAM ET") { _, _ -> if (raceMode) { computerFound = 0; startGame() } else startGame() }
                .setNegativeButton("BÖLÜMLER") { _, _ -> raceMode = false; showLevels() }
                .setNeutralButton("MENÜ") { _, _ -> showMenu() }
                .setCancelable(false).show()
        } else {
            renderPuzzle()
        }
    }

    private fun clearSelection() {
        selectedCells.clear()
        swipeDr = 0
        swipeDc = 0
        hintCell = -1
        renderPuzzle()
    }

    private fun checkWord() {
        val p = currentPuzzle ?: return
        if (selectedCells.isEmpty()) {
            Toast.makeText(this, "Önce harfleri seç.", Toast.LENGTH_SHORT).show()
            return
        }
        val selected = selectedCells.joinToString("") { idx -> p.grid[idx / GRID_SIZE][idx % GRID_SIZE].toString() }
        if (p.words.contains(selected) && !found.contains(selected)) {
            acceptWord(selected)
        } else {
            wrongAttempts++
            selectedCells.clear()
            swipeDr = 0
            swipeDc = 0
            playFeedback(false)
            renderPuzzle()
        }
    }

    private fun hint() {
        val p = currentPuzzle ?: return
        if (coins < 10) {
            Toast.makeText(this, "İpucu için 10 altın gerekiyor.", Toast.LENGTH_SHORT).show()
            return
        }
        val target = p.words.firstOrNull { !found.contains(it) } ?: return
        val cells = findWordCells(p.grid, target)
        if (cells.isNotEmpty()) {
            coins -= 10
            hintCell = cells.first()
            save()
            renderPuzzle()
            Toast.makeText(this, "💡 İpucu: Sarı harf, ${target.first()} ile başlayan kelimenin ilk harfi.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getStars(levelNumber: Int): Int =
        prefs.getInt("stars_$levelNumber", 0).coerceIn(0, 3)

    private fun createPuzzle(level: Int): WordPuzzle {
        val bank = activeWordBank()
        val base = bank[(level - 1) % bank.size]
        val count = when {
            level <= 10 -> 5
            level <= 30 -> 6
            level <= 60 -> 7
            else -> 8
        }
        val words = base.distinct()
            .filter { it.length in if (level <= 10) 2..7 else 2..8 }
            .sortedByDescending { it.length }
            .take(minOf(count, base.size))
        repeat(200) {
            val grid = Array(GRID_SIZE) { CharArray(GRID_SIZE) { ' ' } }
            val placements = mutableMapOf<String, List<Int>>()
            val shuffled = words.shuffled(Random(level * 1000 + it))
            var ok = true
            for (word in shuffled) {
                val placed = placeWord(grid, word)
                if (placed == null) { ok = false; break }
                placements[word] = placed
            }
            if (ok) {
                for (r in 0 until GRID_SIZE) for (c in 0 until GRID_SIZE) if (grid[r][c] == ' ') grid[r][c] = randomLetter()
                return WordPuzzle(words, grid)
            }
        }
        val grid = Array(GRID_SIZE) { CharArray(GRID_SIZE) { randomLetter() } }
        var row = 0
        val safeWords = words.filter { it.length <= 8 }
        for (word in safeWords) {
            if (row >= GRID_SIZE) break
            for (i in word.indices) grid[row][i] = word[i]
            row++
        }
        return WordPuzzle(safeWords, grid)
    }

    private fun placeWord(grid: Array<CharArray>, word: String): List<Int>? {
        val dirs = listOf(
            0 to 1, 0 to -1, 1 to 0, -1 to 0,
            1 to 1, -1 to -1, 1 to -1, -1 to 1
        ).shuffled()
        repeat(80) {
            val (dr, dc) = dirs.random()
            val sr = Random.nextInt(GRID_SIZE)
            val sc = Random.nextInt(GRID_SIZE)
            val endR = sr + dr * (word.length - 1)
            val endC = sc + dc * (word.length - 1)
            if (endR !in 0 until GRID_SIZE || endC !in 0 until GRID_SIZE) return@repeat
            var valid = true
            for (k in word.indices) {
                val r = sr + dr * k; val c = sc + dc * k
                val cell = grid[r][c]
                if (cell != ' ' && cell != word[k]) { valid = false; break }
            }
            if (!valid) return@repeat
            val cells = mutableListOf<Int>()
            for (k in word.indices) {
                val r = sr + dr * k; val c = sc + dc * k
                grid[r][c] = word[k]
                cells.add(r * GRID_SIZE + c)
            }
            return cells
        }
        return null
    }

    private fun findWordCells(grid: Array<CharArray>, word: String): List<Int> {
        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0, 1 to 1, -1 to -1, 1 to -1, -1 to 1)
        for (r in 0 until GRID_SIZE) for (c in 0 until GRID_SIZE) for ((dr, dc) in dirs) {
            val endR = r + dr * (word.length - 1); val endC = c + dc * (word.length - 1)
            if (endR !in 0 until GRID_SIZE || endC !in 0 until GRID_SIZE) continue
            val cells = mutableListOf<Int>()
            var ok = true
            for (k in word.indices) {
                val rr = r + dr * k; val cc = c + dc * k
                if (grid[rr][cc] != word[k]) { ok = false; break }
                cells.add(rr * GRID_SIZE + cc)
            }
            if (ok) return cells
        }
        return emptyList()
    }

    private fun randomLetter(): Char = letterAlphabet().random()
    private inner class WordGridView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var puzzle: WordPuzzle? = null
        private var cellSize = 0f
        private var gap = dp(3).toFloat()

        fun setPuzzle(p: WordPuzzle) { puzzle = p }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val p = puzzle ?: return
            val size = minOf(width, height)
            cellSize = (size - gap * (GRID_SIZE - 1)) / GRID_SIZE
            val total = cellSize * GRID_SIZE + gap * (GRID_SIZE - 1)
            val ox = (width - total) / 2f
            val oy = (height - total) / 2f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            for (i in 0 until GRID_SIZE * GRID_SIZE) {
                val r = i / GRID_SIZE; val c = i % GRID_SIZE
                val left = ox + c * (cellSize + gap); val top = oy + r * (cellSize + gap)
                val selected = selectedCells.contains(i)
                val foundCell = isFoundCell(i)
                paint.style = Paint.Style.FILL
                paint.color = when {
                    i == hintCell -> Color.rgb(255, 222, 102)
                    selected -> Color.rgb(205, 184, 247)
                    foundCell -> Color.rgb(215, 242, 222)
                    else -> Color.WHITE
                }
                canvas.drawRoundRect(left, top, left + cellSize, top + cellSize, dp(9).toFloat(), dp(9).toFloat(), paint)
                paint.color = Color.rgb(48, 35, 60)
                paint.textSize = cellSize * 0.48f
                val cy = top + cellSize / 2f - (paint.ascent() + paint.descent()) / 2f
                canvas.drawText(p.grid[r][c].toString(), left + cellSize / 2f, cy, paint)
            }
        }

        private var lastTouchX = 0f
        private var lastTouchY = 0f
        private var didMove = false

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    didMove = false
                    cellIndexAt(event.x, event.y)?.let { selectCell(it) }
                    invalidate()
                    updateSelectionText()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Hızlı kaydırmada arada kalan hücreleri de yakala.
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(6)) didMove = true
                    val steps = (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) /
                        maxOf(1f, cellSize / 4f)).toInt().coerceIn(1, 30)
                    for (step in 1..steps) {
                        val fraction = step / steps.toFloat()
                        cellIndexAt(lastTouchX + dx * fraction, lastTouchY + dy * fraction)
                            ?.let { selectCell(it) }
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                    updateSelectionText()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    cellIndexAt(event.x, event.y)?.let { selectCell(it) }
                    if (didMove) finishSwipeSelection()
                    performClick()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    clearSelection()
                    return true
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        private fun cellIndexAt(x: Float, y: Float): Int? {
            val size = minOf(width, height)
            cellSize = (size - gap * (GRID_SIZE - 1)) / GRID_SIZE
            val total = cellSize * GRID_SIZE + gap * (GRID_SIZE - 1)
            val ox = (width - total) / 2f
            val oy = (height - total) / 2f
            val c = ((x - ox) / (cellSize + gap)).toInt()
            val r = ((y - oy) / (cellSize + gap)).toInt()
            if (r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE) return null
            val localX = (x - ox) - c * (cellSize + gap)
            val localY = (y - oy) - r * (cellSize + gap)
            if (localX < 0 || localY < 0 || localX > cellSize || localY > cellSize) return null
            return r * GRID_SIZE + c
        }
    }

}
