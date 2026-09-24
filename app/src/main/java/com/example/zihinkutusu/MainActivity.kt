package com.example.zihinkutusu

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
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
import kotlin.math.max
import kotlin.random.Random

data class WordPuzzle(val words: List<String>, val grid: Array<CharArray>)

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var grid: GridLayout
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

    private val prefs by lazy { getSharedPreferences("game", 0) }

    // Türkçe karakterler özellikle korunur: Ç Ğ İ Ö Ş Ü ve ı/i ayrımı önemlidir.
    private val wordBank = listOf(
        listOf("KALEM", "KİTAP", "OKUL"),
        listOf("MASA", "KAPI", "SAAT"),
        listOf("ELMA", "ARMUT", "MUZ"),
        listOf("DENİZ", "GÜNEŞ", "BULUT"),
        listOf("KEDİ", "KÖPEK", "KUŞ"),
        listOf("ARABA", "YOL", "KÖPRÜ"),
        listOf("EV", "ODA", "BAHÇE"),
        listOf("ÇAY", "KAHVE", "EKMEK"),
        listOf("ANNE", "BABA", "AİLE"),
        listOf("MUTLU", "SEVGİ", "DOST"),
        listOf("KALEM", "DEFTER", "SİLGİ"),
        listOf("TELEFON", "EKRAN", "MESAJ"),
        listOf("KIRMIZI", "MAVİ", "YEŞİL"),
        listOf("KIŞ", "BAHAR", "YAZ"),
        listOf("SABAH", "AKŞAM", "GECE"),
        listOf("TATLI", "TUZLU", "EKŞİ"),
        listOf("BİLGİ", "ZEKÂ", "SORU"),
        listOf("OYUN", "KAZAN", "PUAN"),
        listOf("ALTIN", "PARA", "KASA"),
        listOf("HIZLI", "DİKKAT", "AKIL")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        MobileAds.initialize(this) {}
        showMenu()
    }

    private fun load() {
        level = prefs.getInt("level", 1).coerceIn(1, 100)
        score = prefs.getInt("score", 0)
        coins = prefs.getInt("coins", 50)
        lives = prefs.getInt("lives", 5)
    }

    private fun save() {
        prefs.edit().putInt("level", level).putInt("score", score).putInt("coins", coins).putInt("lives", lives).apply()
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
        root.addView(gameButton("❓  NASIL OYNANIR?" ) { help() }, LinearLayout.LayoutParams(-1, dp(60)))

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
        }, LinearLayout.LayoutParams(-1, dp(32)))

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

    private fun help() {
        AlertDialog.Builder(this)
            .setTitle("🔎 Nasıl Oynanır?")
            .setMessage("1. Üstte istenen kelimeleri gör.\n\n2. Harfleri yan yana veya çapraz komşu olacak şekilde sırayla seç.\n\n3. Kelime tamamlandığında oyun otomatik olarak bulur; BUL butonu da kullanılabilir.\n\n4. Doğru kelime +20 puan ve +5 altın verir.\n\n5. İpucu 10 altın karşılığında bir kelimenin ilk harfini gösterir.\n\nTüm kelimeleri bulunca bölüm tamamlanır.")
            .setPositiveButton("TAMAM", null).show()
    }

    private fun startGame() {
        found = mutableSetOf()
        selectedCells = mutableListOf()
        hintCell = -1
        currentPuzzle = createPuzzle(level)
        root = base()

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(gameButton("☰") { showMenu() }, LinearLayout.LayoutParams(dp(55), dp(48)))
        infoText = title("Bölüm $level", 18f)
        top.addView(infoText, LinearLayout.LayoutParams(0, dp(48), 1f))
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
        }, LinearLayout.LayoutParams(-1, dp(28)))

        wordListBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        root.addView(wordListBox, LinearLayout.LayoutParams(-1, dp(45)))

        grid = GridLayout(this).apply { columnCount = 8; rowCount = 8; alignmentMode = GridLayout.ALIGN_BOUNDS }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(0, dp(4), 0, dp(4)) })

        selectedText = TextView(this).apply {
            text = "Seçilen: —"
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(78, 43, 126))
            background = rounded(Color.WHITE, 14f)
        }
        root.addView(selectedText, LinearLayout.LayoutParams(-1, dp(42)).apply { setMargins(0, 0, 0, dp(5)) })

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bar.addView(gameButton("🧹\nTEMİZLE") { clearSelection() }, LinearLayout.LayoutParams(0, dp(58), 1f).apply { setMargins(0, 0, dp(4), 0) })
        bar.addView(gameButton("🔎\nBUL") { checkWord() }, LinearLayout.LayoutParams(0, dp(58), 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        bar.addView(gameButton("💡\nİPUCU") { hint() }, LinearLayout.LayoutParams(0, dp(58), 1f).apply { setMargins(dp(4), 0, 0, 0) })
        root.addView(bar)
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

        grid.removeAllViews()
        val cellSize = max(dp(34), (resources.displayMetrics.widthPixels - dp(42)) / 8)
        for (i in 0 until 64) {
            val r = i / 8
            val c = i % 8
            val ch = p.grid[r][c]
            val v = TextView(this).apply {
                text = ch.toString()
                textSize = 20f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(48, 35, 60))
                val bgColor = when {
                    i == hintCell -> Color.rgb(255, 222, 102)
                    selectedCells.contains(i) -> Color.rgb(205, 184, 247)
                    isFoundCell(i) -> Color.rgb(215, 242, 222)
                    else -> Color.WHITE
                }
                background = rounded(bgColor, 10f)
                elevation = dp(1).toFloat()
                setOnClickListener { selectCell(i) }
            }
            grid.addView(v, GridLayout.LayoutParams().apply {
                width = cellSize
                height = cellSize
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
            val anim = AlphaAnimation(0f, 1f).apply { duration = 120 }
            v.startAnimation(anim)
        }
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

        // Kelime seçimi gerçek kelime bulmaca mantığında komşu hücrelerden ilerler.
        if (selectedCells.isNotEmpty()) {
            val last = selectedCells.last()
            val lr = last / 8
            val lc = last % 8
            val r = index / 8
            val c = index % 8
            if (kotlin.math.abs(lr - r) > 1 || kotlin.math.abs(lc - c) > 1) {
                Toast.makeText(this, "Harfleri yan yana veya çapraz seç.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        selectedCells.add(index)
        hintCell = -1
        val p = currentPuzzle ?: return
        val selected = selectedCells.joinToString("") { idx -> p.grid[idx / 8][idx % 8].toString() }
        if (p.words.contains(selected) && !found.contains(selected)) {
            acceptWord(selected)
        } else {
            renderPuzzle()
        }
    }

    private fun acceptWord(word: String) {
        val p = currentPuzzle ?: return
        found.add(word)
        score += 20
        coins += 5
        selectedCells.clear()
        Toast.makeText(this, "🎉 $word bulundu! +20 puan +5 altın", Toast.LENGTH_SHORT).show()
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
        val extra = if (level >= 25) 1 else 0
        val words = (base + if (extra == 1) listOf("AKIL") else emptyList()).distinct().take(4)
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
}
