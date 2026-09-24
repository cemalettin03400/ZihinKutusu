# Zihin Kutusu 5.1

Kelime bulmaca tarzında Android oyun.

## Oyun
- 100 bölüm
- 8x8 harf bulmacası
- Her bölümde 3-4 gizli kelime
- Harflere sırayla dokunarak kelime oluşturma
- Puan, altın ve can sistemi
- İpucu sistemi
- Bölüm ilerlemesi cihazda kaydedilir
- Google test banner reklamı

## GitHub Actions
`.github/workflows/build.yml` dosyası ile GitHub Actions üzerinden APK ve AAB üretilebilir.

APK: `app-debug.apk`
AAB: `app-release.aab`

Reklam kimlikleri şu an Google'ın TEST reklam kimlikleridir. Play Store'a çıkmadan önce kendi AdMob kimliklerin kullanılmalıdır.


- Oyundan çıkış için onaylı ÇIKIŞ düğmesi eklendi.


Türkçe karakterler: Ç, Ğ, İ, I, Ö, Ş, Ü ve ı/i ayrımı korunur. Kelime sayısı bölüm ilerledikçe 4, 5, 6 ve 7 kelimeye çıkar.

## Zihin Kutusu 5.4
- Parmağı kaydırarak komşu ve çapraz harfleri seçme
- Kelimeyi parmak kaldırınca otomatik kontrol etme
- Uzun kelime, seri ve hızlı çözüm puan bonusları
- Türkçe karakter desteği: Ç, Ğ, İ, Ö, Ş, Ü
