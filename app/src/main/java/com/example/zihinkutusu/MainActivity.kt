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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        MobileAds.initialize(this) {}
        showMenu()
    }

    override fun onDestroy() {
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
            adUnitId = "ca-app-pub-3940256099942544/6300978111"
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
        root.addView(title("⚙️ AYARLAR", 28f), LinearLayout.LayoutParams(-1, dp(55)))
        val sound = Switch(this).apply {
            text = "🔊 Sesler"
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
            text = "📳 Titreşim"
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
        root.addView(gameButton("← ANA MENÜ") { showMenu() }, LinearLayout.LayoutParams(-1, dp(52)))
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
        root = base()
        root.addView(title("🧠 ZİHİN KUTUSU", 30f), LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(TextView(this).apply {
            text = "🔎 Kelimeleri bul • Bölümleri tamamla • Altın kazan"
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

        root.addView(gameButton("🎯  OYUNA BAŞLA\nBölüm $level" ) { startGame() }, LinearLayout.LayoutParams(-1, dp(68)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(gameButton("🗺️  BÖLÜMLER\n1 - 100" ) { showLevels() }, LinearLayout.LayoutParams(-1, dp(68)).apply { setMargins(0, 0, 0, dp(8)) })
        val infoRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        infoRow.addView(gameButton("🏆\nBAŞARILAR") { showAchievements() }, LinearLayout.LayoutParams(0, dp(62), 1f).apply { setMargins(0, 0, dp(4), dp(8)) })
        infoRow.addView(gameButton("📊\nİSTATİSTİK") { showStats() }, LinearLayout.LayoutParams(0, dp(62), 1f).apply { setMargins(dp(4), 0, 0, dp(8)) })
        root.addView(infoRow)

        root.addView(gameButton("⚙️  AYARLAR" ) { showSettings() }, LinearLayout.LayoutParams(-1, dp(60)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(gameButton("❓  NASIL OYNANIR?" ) { help() }, LinearLayout.LayoutParams(-1, dp(60)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(gameButton("🚪  ÇIKIŞ" ) { exitGame() }, LinearLayout.LayoutParams(-1, dp(58)))

        val note = TextView(this).apply {
            text = "Her bölümde gizlenmiş kelimeleri bul.\nHarfleri sırayla seç ve kelimeyi tamamla!"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.GRAY)
        }
        root.addView(note, LinearLayout.LayoutParams(-1, dp(65)))
        addAd()
        setContentView(root)
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
        infoText = title("Bölüm $level", 18f)
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

        val navigation = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        navigation.addView(gameButton("← OYUNA GERİ DÖN") { startGame() }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(0, dp(5), dp(4), 0) })
        navigation.addView(gameButton("⌂ ANA MENÜ") { showMenu() }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(4), dp(5), dp(4), 0) })
        navigation.addView(gameButton("🚪 ÇIKIŞ") { exitGame() }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(4), dp(5), 0, 0) })
        root.addView(navigation)
        addAd()
        setContentView(root)
        renderPuzzle()
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
            AlertDialog.Builder(this)
                .setTitle("🏆 Bölüm Tamamlandı!")
                .setMessage("Bölüm $completedLevel\n$starsLine\n\n+$stars × 5 bonus altın$bestLine\n\nSıradaki bölüm: $level")
                .setPositiveButton("DEVAM ET") { _, _ -> startGame() }
                .setNegativeButton("BÖLÜMLER") { _, _ -> showLevels() }
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
        val base = wordBank[(level - 1) % wordBank.size]
        val count = when {
            level <= 10 -> 5
            level <= 30 -> 6
            level <= 60 -> 7
            else -> 8
        }
        val words = base.distinct()
            .filter { it.length in if (level <= 10) 3..7 else 4..8 }
            .sortedByDescending { it.length }
            .take(count)
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

    private fun randomLetter(): Char = "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ".random()
    private inner class WordGridView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var puzzle: WordPuzzle? = null
        private var cellSize = 0f
        private var gap = dp(3).toFloat()
        private var active = false

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
                val r=i/GRID_SIZE; val c=i%GRID_SIZE
                val left=ox+c*(cellSize+gap); val top=oy+r*(cellSize+gap)
                val selected=selectedCells.contains(i)
                val foundCell=isFoundCell(i)
                paint.style=Paint.Style.FILL
                paint.color=when { i==hintCell -> Color.rgb(255,222,102); selected -> Color.rgb(205,184,247); foundCell -> Color.rgb(215,242,222); else -> Color.WHITE }
                canvas.drawRoundRect(left,top,left+cellSize,top+cellSize,dp(9).toFloat(),dp(9).toFloat(),paint)
                paint.color=Color.rgb(48,35,60)
                paint.textSize=cellSize*0.48f
                val cy=top+cellSize/2f-(paint.ascent()+paint.descent())/2f
                canvas.drawText(p.grid[r][c].toString(),left+cellSize/2f,cy,paint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val p=puzzle ?: return true
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    selectedCells.clear(); swipeDr=0; swipeDc=0; active=true; addFromPoint(event.x,event.y); hintCell=-1; invalidate(); updateSelectionText(); return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if(active) { addFromPoint(event.x,event.y); invalidate(); updateSelectionText() }; return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if(active) { active=false; finishSwipeSelection() }; return true
                }
            }
            return true
        }

        private fun addFromPoint(x:Float,y:Float) {
            val size=minOf(width,height); cellSize=(size-gap*(GRID_SIZE-1))/GRID_SIZE; val total=cellSize*GRID_SIZE+gap*(GRID_SIZE-1); val ox=(width-total)/2f; val oy=(height-total)/2f
            val c=((x-ox)/(cellSize+gap)).toInt(); val r=((y-oy)/(cellSize+gap)).toInt()
            if(r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE) return
            val localX=(x-ox)-c*(cellSize+gap); val localY=(y-oy)-r*(cellSize+gap)
            if(localX<0 || localY<0 || localX>cellSize || localY>cellSize) return
            selectCell(r*8+c)
        }
    }

}
