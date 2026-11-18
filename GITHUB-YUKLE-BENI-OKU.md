# Almalı 2.1 Beta — GitHub'a yükleme (0 TL)

**Önceki Almalı 2.0 Beta APK'yı silme.** Farklı GitHub debug imzaları nedeniyle 2.1 ayrı `com.almali.app.beta21` paketi olarak oluşturulur ve kayıtların aynı kurulumda korunması sağlanamaz. Şimdilik deneme için ayrı uygulama kullanacağız.

## 1. Dosyaları doğru yere yükle

1. ZIP'i Windows bilgisayarında ayıkla.
2. `Almali-Android` klasörünü aç; **içindeki** dosya/klasörleri seç.
3. GitHub'daki `Almali` deposuna gir → `Code → Add file → Upload files`.
4. Klasör yapısını koruyarak `app`, `.github`, `docs`, `tests`, `tools` ve kökteki Gradle dosyalarını yükle.
5. `Commit changes` ile kaydet.

Beklenen yol: `Almali/app/src/main/java/com/almali/app/DemoPriceFeed.java`.

## 2. Derle

- `Actions → Almalı 2.1 Android APK` iş akışını aç.
- Yeşil tikli yeni derlemeye bas → sayfanın altındaki `Artifacts → almali-v21-debug-apk` dosyasını indir.
- ZIP'i ayıkla → `app-debug.apk` dosyasını Samsung'a aktar → yükle.

## 3. İlk test

- **Almalı 2.1 Beta** ayrı bir uygulama olarak görünmeli.
- İlk Old Spice ürününün detayını aç. DEMO JSON bağlantısını girersen eski test sorunsuz çalışmalı.
- `docs/EXTERNAL-FEED.md` içindeki biçimde gerçekten kullanım izni olan bir fiyat kaynağı **edinmedikçe** gerçek fiyatlar gelmez.

## Bilinen sınır

Bu proje gerçek Trendyol fiyatını otomatik taramaz; Trendyol'un tüketiciye açık, ücretsiz ve izinli ürün-fiyat API'si doğrulanmadı. `external` JSON biçimi veri kaynağı doğruluğunun garantisi değildir.
