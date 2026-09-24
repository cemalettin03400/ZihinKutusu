package com.example.zihinkutusu

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private lateinit var root: LinearLayout
    private lateinit var grid: WordGridView
    private lateinit var selectedText: TextView
    private lateinit var wordListBox: LinearLayout
    private lateinit var infoText: TextView
    private lateinit var livesText: TextView
    private var level = 1
    private var score = 0
    private var coins = 50
    private var lives = 5
    private var found = mutableSetOf<String>()
    private var selectedCells = mutableListOf<Int>()
    private var currentPuzzle: WordPuzzle? = null
    private var hintCell = -1
    private var combo = 0
    private var levelStartTime = 0L

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

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        showMenu()
    }

    private fun load() {
        level = prefs.getInt("level", 1).coerceIn(1, 100)
        score = prefs.getInt("score", 0)
        coins = prefs.getInt("coins", 50)
        lives = prefs.getInt("lives", 5)
        combo = prefs.getInt("combo", 0)
    }

    private fun save() {
        prefs.edit().putInt("level", level).putInt("score", score).putInt("coins", coins).putInt("lives", lives).putInt("combo", combo).apply()
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
            text = "⭐ $score puan     🪙 $coins altın     ❤️ $lives can"
            textSize = 15f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(108, 63, 199), 18f)
        }
        root.addView(stats, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(4), 0, dp(10)) })

        root.addView(gameButton("🎯  OYUNA BAŞLA\nBölüm $level" ) { startGame() }, LinearLayout.LayoutParams(-1, dp(68)).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(gameButton("🗺️  BÖLÜMLER\n1 - 100" ) { showLevels() }, LinearLayout.LayoutParams(-1, dp(68)).apply { setMargins(0, 0, 0, dp(8)) })
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
            text = "Açılan bölümler: 1 - $level"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }, LinearLayout.LayoutParams(-1, dp(26)))

        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (i in 1..100) {
            val unlocked = i <= level
            val b = gameButton("${if (unlocked) "🔓" else "🔒"}  Bölüm $i ${if (i < level) "✓" else ""}") {
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
        infoText.text = "Bölüm $level   •   ⭐ $score   •   🪙 $coins"
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
        selectedText.text = "Seçilen: " + selectedCells.joinToString("") { idx -> p.grid[idx / 8][idx % 8].toString() }.ifEmpty { "—" }
        save()
    }

    private fun isFoundCell(index: Int): Boolean {
        val p = currentPuzzle ?: return false
        return p.words.filter { found.contains(it) }.any { word ->
            findWordCells(p.grid, word).contains(index)
        }
    }

    private fun selectCell(index: Int) {
        if (selectedCells.contains(index)) return
        if (selectedCells.size >= 12) return

        if (selectedCells.isNotEmpty()) {
            val last = selectedCells.last()
            val lr = last / 8; val lc = last % 8
            val r = index / 8; val c = index % 8
            if (kotlin.math.abs(lr - r) > 1 || kotlin.math.abs(lc - c) > 1) return
        }
        selectedCells.add(index)
        hintCell = -1
        updateSelectionText()
        grid.invalidate()
    }

    private fun finishSwipeSelection() {
        val p = currentPuzzle ?: return
        if (selectedCells.isEmpty()) return
        val selected = selectedCells.joinToString("") { idx -> p.grid[idx / 8][idx % 8].toString() }
        if (p.words.contains(selected) && !found.contains(selected)) {
            acceptWord(selected)
        } else {
            combo = 0
            lives = (lives - 1).coerceAtLeast(0)
            selectedCells.clear()
            save()
            if (lives == 0) Toast.makeText(this, "❤️ Canın bitti! İpucu kullanabilir veya oyuna devam edebilirsin.", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "❌ "+selected+" kelime listesinde yok. -1 can", Toast.LENGTH_SHORT).show()
            renderPuzzle()
        }
    }

    private fun updateSelectionText() {
        val p = currentPuzzle ?: return
        selectedText.text = "Seçilen: " + selectedCells.joinToString("") { idx -> p.grid[idx / 8][idx % 8].toString() }.ifEmpty { "—" }
    }

    private fun acceptWord(word: String) {
        val p = currentPuzzle ?: return
        found.add(word)
        combo += 1
        val lengthBonus = (word.length - 3).coerceAtLeast(0) * 5
        val comboBonus = (combo - 1) * 5
        val timeBonus = if (System.currentTimeMillis() - levelStartTime < 45000) 10 else 0
        val earned = 20 + lengthBonus + comboBonus + timeBonus
        score += earned
        coins += 5 + (word.length / 4)
        selectedCells.clear()
        Toast.makeText(this, "🎉 $word bulundu! +$earned puan  •  🔥 Seri x$combo", Toast.LENGTH_SHORT).show()
        if (found.size == p.words.size) {
            level = (level + 1).coerceAtMost(100)
            coins += 15
            save()
            AlertDialog.Builder(this)
                .setTitle("🏆 Bölüm Tamamlandı!")
                .setMessage("Tebrikler! +15 bonus altın\n\nSıradaki bölüm: $level")
                .setPositiveButton("DEVAM ET") { _, _ -> startGame() }
                .setNegativeButton("MENÜ") { _, _ -> showMenu() }
                .setCancelable(false).show()
        } else {
            renderPuzzle()
        }
    }

    private fun clearSelection() {
        selectedCells.clear()
        hintCell = -1
        renderPuzzle()
    }

    private fun checkWord() {
        val p = currentPuzzle ?: return
        if (selectedCells.isEmpty()) {
            Toast.makeText(this, "Önce harfleri seç.", Toast.LENGTH_SHORT).show()
            return
        }
        val selected = selectedCells.joinToString("") { idx -> p.grid[idx / 8][idx % 8].toString() }
        if (p.words.contains(selected) && !found.contains(selected)) {
            acceptWord(selected)
        } else {
            lives--
            selectedCells.clear()
            save()
            if (lives <= 0) {
                Toast.makeText(this, "❤️ Canların bitti. 20 altın karşılığı 1 can alabilirsin.", Toast.LENGTH_LONG).show()
                lives = 0
            } else {
                Toast.makeText(this, "❌ Bu kelime listede yok. -1 can", Toast.LENGTH_SHORT).show()
            }
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

    private fun createPuzzle(level: Int): WordPuzzle {
        val base = wordBank[(level - 1) % wordBank.size]
        val count = when {
            level <= 10 -> 4
            level <= 30 -> 5
            level <= 60 -> 6
            else -> 7
        }
        val words = base.distinct().filter { it.length <= 8 }.take(count)
        repeat(200) {
            val grid = Array(8) { CharArray(8) { ' ' } }
            val placements = mutableMapOf<String, List<Int>>()
            val shuffled = words.shuffled(Random(level * 1000 + it))
            var ok = true
            for (word in shuffled) {
                val placed = placeWord(grid, word)
                if (placed == null) { ok = false; break }
                placements[word] = placed
            }
            if (ok) {
                for (r in 0..7) for (c in 0..7) if (grid[r][c] == ' ') grid[r][c] = randomLetter()
                return WordPuzzle(words, grid)
            }
        }
        val grid = Array(8) { CharArray(8) { randomLetter() } }
        var row = 0
        val safeWords = words.filter { it.length <= 8 }
        for (word in safeWords) {
            if (row >= 8) break
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
            val sr = Random.nextInt(8)
            val sc = Random.nextInt(8)
            val endR = sr + dr * (word.length - 1)
            val endC = sc + dc * (word.length - 1)
            if (endR !in 0..7 || endC !in 0..7) return@repeat
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
                cells.add(r * 8 + c)
            }
            return cells
        }
        return null
    }

    private fun findWordCells(grid: Array<CharArray>, word: String): List<Int> {
        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0, 1 to 1, -1 to -1, 1 to -1, -1 to 1)
        for (r in 0..7) for (c in 0..7) for ((dr, dc) in dirs) {
            val endR = r + dr * (word.length - 1); val endC = c + dc * (word.length - 1)
            if (endR !in 0..7 || endC !in 0..7) continue
            val cells = mutableListOf<Int>()
            var ok = true
            for (k in word.indices) {
                val rr = r + dr * k; val cc = c + dc * k
                if (grid[rr][cc] != word[k]) { ok = false; break }
                cells.add(rr * 8 + cc)
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
            cellSize = (size - gap * 7f) / 8f
            val total = cellSize * 8f + gap * 7f
            val ox = (width - total) / 2f
            val oy = (height - total) / 2f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            for (i in 0 until 64) {
                val r=i/8; val c=i%8
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
                    selectedCells.clear(); active=true; addFromPoint(event.x,event.y); hintCell=-1; invalidate(); updateSelectionText(); return true
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
            val size=minOf(width,height); cellSize=(size-gap*7f)/8f; val total=cellSize*8f+gap*7f; val ox=(width-total)/2f; val oy=(height-total)/2f
            val c=((x-ox)/(cellSize+gap)).toInt(); val r=((y-oy)/(cellSize+gap)).toInt()
            if(r !in 0..7 || c !in 0..7) return
            val localX=(x-ox)-c*(cellSize+gap); val localY=(y-oy)-r*(cellSize+gap)
            if(localX<0 || localY<0 || localX>cellSize || localY>cellSize) return
            selectCell(r*8+c)
        }
    }

}
