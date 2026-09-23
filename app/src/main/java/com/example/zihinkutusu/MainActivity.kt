package com.example.zihinkutusu

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var grid: GridLayout
    private lateinit var info: TextView
    private lateinit var livesText: TextView
    private var level=1; private var moves=20; private var score=0; private var coins=50; private var lives=5
    private var selected=-1
    private val colors=arrayOf(Color.rgb(239,83,80),Color.rgb(66,165,245),Color.rgb(102,187,106),Color.rgb(255,202,40),Color.rgb(171,71,188),Color.rgb(38,198,218))
    private val cells=IntArray(36)
    private val prefs by lazy { getSharedPreferences("game",0) }

    override fun onCreate(b: Bundle?){super.onCreate(b); load(); MobileAds.initialize(this){}; showMenu()}

    private fun load(){level=prefs.getInt("level",1);score=prefs.getInt("score",0);coins=prefs.getInt("coins",50);lives=prefs.getInt("lives",5)}
    private fun save(){prefs.edit().putInt("level",level).putInt("score",score).putInt("coins",coins).putInt("lives",lives).apply()}
    private fun base(): LinearLayout = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,24,18,16);setBackgroundColor(Color.rgb(247,245,252))}
    private fun btn(t:String, a:()->Unit)=Button(this).apply{text=t;textSize=16f;setOnClickListener{a()}}

    private fun showMenu(){
        root=base()
        val title=TextView(this).apply{text="🧠 ZİHİN KUTUSU";textSize=30f;gravity=Gravity.CENTER;setTextColor(Color.rgb(80,45,130))}
        root.addView(title,LinearLayout.LayoutParams(-1,100))
        val sub=TextView(this).apply{text="Renkleri eşleştir • Bölümleri tamamla • Altın kazan";gravity=Gravity.CENTER;textSize=15f}
        root.addView(sub,LinearLayout.LayoutParams(-1,70))
        root.addView(btn("▶ OYNA"){startGame()},LinearLayout.LayoutParams(-1,65))
        root.addView(btn("🗺 BÖLÜMLER"){showLevels()},LinearLayout.LayoutParams(-1,65))
        root.addView(btn("ℹ NASIL OYNANIR?"){help()},LinearLayout.LayoutParams(-1,65))
        val ad=AdView(this).apply{adUnitId="ca-app-pub-3940256099942544/6300978111";setAdSize(com.google.android.gms.ads.AdSize.BANNER);loadAd(AdRequest.Builder().build())}
        root.addView(ad,LinearLayout.LayoutParams(-1,60))
        setContentView(root)
    }
    private fun showLevels(){
        root=base()
        root.addView(TextView(this).apply{text="🗺 BÖLÜMLER";textSize=28f;gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,75))
        val scroll=ScrollView(this); val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        for(i in 1..100){
            val unlocked=i<=level
            box.addView(btn("${if(unlocked)"🔓" else "🔒"} Bölüm $i ${if(i<level)"  ✓" else ""}"){
                if(unlocked){level=i;startGame()} else Toast.makeText(this,"Önceki bölümü tamamla.",Toast.LENGTH_SHORT).show()
            },LinearLayout.LayoutParams(-1,55))
        }
        scroll.addView(box);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(btn("← Ana Menü"){showMenu()},LinearLayout.LayoutParams(-1,60));setContentView(root)
    }
    private fun help(){AlertDialog.Builder(this).setTitle("Nasıl Oynanır?").setMessage("Aynı renkteki iki kutuya dokun. Eşleşirse puan ve altın kazanırsın. Yanlış seçim hamle azaltır. İpucu 10 altın, ekstra can 20 altın. Her 100 puanda bölüm ilerler.").setPositiveButton("Tamam",null).show()}

    private fun startGame(){
        moves=(16+level*2).coerceAtMost(40);selected=-1
        for(i in cells.indices)cells[i]=Random.nextInt(colors.size)
        root=base()
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        top.addView(btn("☰"){showMenu()},LinearLayout.LayoutParams(65,55))
        info=TextView(this).apply{textSize=15f;gravity=Gravity.CENTER}
        top.addView(info,LinearLayout.LayoutParams(0,55,1f))
        root.addView(top)
        livesText=TextView(this).apply{textSize=14f;gravity=Gravity.CENTER}
        root.addView(livesText,LinearLayout.LayoutParams(-1,42))
        grid=GridLayout(this).apply{columnCount=6;rowCount=6}
        root.addView(grid,LinearLayout.LayoutParams(-1,0,1f))
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        bar.addView(btn("💡 İpucu"){hint()},LinearLayout.LayoutParams(0,58,1f))
        bar.addView(btn("❤️ Can"){buyLife()},LinearLayout.LayoutParams(0,58,1f))
        bar.addView(btn("🔄"){startGame()},LinearLayout.LayoutParams(0,58,1f))
        root.addView(bar)
        val ad=AdView(this).apply{adUnitId="ca-app-pub-3940256099942544/6300978111";setAdSize(com.google.android.gms.ads.AdSize.BANNER);loadAd(AdRequest.Builder().build())}
        root.addView(ad,LinearLayout.LayoutParams(-1,60))
        setContentView(root);render()
    }
    private fun render(){
        grid.removeAllViews()
        info.text="Bölüm $level • ⭐ $score • 🪙 $coins"
        livesText.text="❤️ $lives   •   🎯 $moves hamle"
        for(i in cells.indices){
            val v=TextView(this).apply{
                gravity=Gravity.CENTER;text=if(i==selected)"✓" else "";textSize=23f;setTextColor(Color.WHITE)
                val bg=GradientDrawable();bg.setColor(colors[cells[i]]);bg.cornerRadius=18f;background=bg
                setOnClickListener{tap(i)}
            }
            val p=GridLayout.LayoutParams().apply{width=0;height=0;columnSpec=GridLayout.spec(i%6,1f);rowSpec=GridLayout.spec(i/6,1f);setMargins(4,4,4,4)}
            grid.addView(v,p)
            val anim=AlphaAnimation(0f,1f);anim.duration=180;v.startAnimation(anim)
        }
        save()
    }
    private fun tap(i:Int){
        if(selected<0){selected=i;render();return}
        if(selected==i)return
        if(cells[selected]==cells[i]){
            score+=10;coins+=2;cells[selected]=Random.nextInt(colors.size);cells[i]=Random.nextInt(colors.size)
            if(score>=level*100){level=(level+1).coerceAtMost(100);coins+=20;Toast.makeText(this,"🎉 Bölüm tamamlandı! +20 altın",Toast.LENGTH_SHORT).show();startGame();return}
        }else{
            moves--;if(moves<=0){lives--;save();if(lives<=0){Toast.makeText(this,"❤️ Canların bitti. Menüden yeni can al.",Toast.LENGTH_LONG).show();render();return};Toast.makeText(this,"Hamleler bitti! -1 can",Toast.LENGTH_SHORT).show();startGame();return}
        }
        selected=-1;render()
    }
    private fun hint(){
        if(coins<10){Toast.makeText(this,"Yeterli altın yok.",Toast.LENGTH_SHORT).show();return}
        coins-=10
        for(i in 0 until 36)for(j in i+1 until 36)if(cells[i]==cells[j]){selected=i;render();Toast.makeText(this,"İşaretli kutunun aynı rengini bul!",Toast.LENGTH_LONG).show();return}
    }
    private fun buyLife(){if(coins>=20){coins-=20;lives++;save();render();Toast.makeText(this,"❤️ +1 can",Toast.LENGTH_SHORT).show()}else Toast.makeText(this,"20 altın gerekiyor.",Toast.LENGTH_SHORT).show()}
}