# Almalı 2.0 Beta — Fiyat Takip ilk sürüm

**Platform:** Android 8+ (minSdk 26), Java, Android Gradle Plugin 8.9.2, WorkManager 2.10.5, SQLite. **Hedef bütçe:** 0 TL.

## Bu sürümde neler var?

- Eski Almalı hatırlatma kodlarının yanında yeni `Fiyat Takip (Beta)` bölümü.
- İlk kullanımda **Old Spice Captain 3'ü 1 Arada 1000 ml** ürününü oluşturma; hedefi 300 TL, **başlangıç fiyatı boş**.
- Başka ürün ekleme, HTTPS mağaza bağlantısını açma, kullanıcı tarafından verilen fotoğraf URL'sini gösterme.
- Elle fiyat + mağaza kaydetme, son 100 gözlemin fiyat grafiği ve tarihçesi.
- Hedefe erişildiğinde **yalnızca kayıt kaynağını belirten** Android fiyat bildirimi.
- Haricî HTTPS JSON DEMO akışını elle veya WorkManager ile yaklaşık saatte bir kontrol etme. Aynı `snapshot_id` ile tekrar kayıt ve alarm üretmeme.
- Ürün kodu / para birimi doğrulaması, bağlantı zaman aşımı, maksimum 32 KB JSON sınırı.

## Önemli: eski uygulama niçin ayrı?

İlk APK GitHub Actions'ın geçici **debug anahtarı** ile imzalandı. Yeni Actions çalıştırması farklı imza kullanabilir; bu durumda eski uygulamanın üzerine yükleme başarısız olabilir. Eski alışveriş verilerini kaybettirmemek için bu sürümün `applicationId` değeri **`com.almali.app.beta`**, uygulama etiketi **`Almalı Beta`** olarak ayarlandı.

- Telefonda hem eski **Almalı** hem yeni **Almalı Beta** yan yana durur.
- Eski uygulama ve eski hatırlatma kayıtları korunur; **yeni beta içindeki hatırlatma listesi sıfırdan başlar**. Beta ayrı uygulamanın verilerini otomatik olarak içe aktarmaz.
- İleride tek uygulamada birleşmek için veri dışa/içe aktarma ile kalıcı ve özel bir üretim imzalama anahtarı gereklidir.

## Kurulum

1. Projenin kökündeki `app`, `.github`, `build.gradle`, `settings.gradle`, `gradle.properties` vb. tüm dosyaları GitHub deponuzun **köküne** yükleyin; iç içe `Almali-Android` klasörü oluşmamalı.
2. `Actions → Android APK derlemesi` iş akışının yeşil olmasını bekleyin.
3. İş akışı sayfasındaki `Artifacts → almali-beta-debug-apk` ZIP'ini indirip ayıklayın.
4. `app-debug.apk` dosyasını telefonunuza gönderip yükleyin.
5. Yeni **Almalı Beta** uygulamasını açın → **Fiyat Takip (Beta)**.

**Bu ortamda Android SDK ve Gradle bulunmadığından APK burada üretilemedi.** Java alarm kurallarının bağımsız testleri çalıştırılabilir; APK'nın CI'da ve telefonda test edilmesi gerekir.

## İlk 3 test

1. Eski Almalı uygulamasının telefonda yerinde durduğunu doğrulayın.
2. `Fiyat Takip` ekranında Old Spice ürününü görüntüleyin, ilk fiyatı `379`, ikinci fiyatı `299,90` olarak **elle** girin. Grafikte iki kayıt ve izin verildiyse manuel fiyat alarmı oluşmalı.
3. `docs/DEMO-TEST.md` üzerinden test JSON beslemesini bağlayın ve otomatik veri hattını deneyin.

## Gerçek otomatik fiyat karşılaştırması neden yok?

Trendyol'un resmî API'si satıcı/entegrasyon odaklıdır; herkese açık ücretsiz tüketici fiyat API'si olduğu doğrulanmadı. Bu sürüm **Trendyol HTML sayfalarından otomatik fiyat kazımaz**, gerçek fiyat gibi gösterilen sahte veriler üretmez ve geçmiş fiyat verisi uydurmaz. Gerçek sağlayıcı bağlanmadan otomatik gerçek fiyat, mağazalar arası canlı en ucuz fiyat veya gerçek indirim alarmı vaat etmiyoruz.

## Derleme sorunları

- Hata varsa GitHub Actions → başarısız `build` adımı → **ilk kırmızı hata satırını** gönderin.
- Yeni sürümü yine GitHub Actions'ta oluştururken debug imzası farklı olabilir; **Beta'nın sonraki sürümünü üzerine kurmak da garanti değil**. Kalıcı güvenli imzalama/yedekleme bir sonraki teknik adımdır.
