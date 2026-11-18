# Almalı — Android alışveriş & hatırlatma uygulaması

**Sürüm:** 0.1.0 (ilk çalışan kaynak proje)

Bu proje Android telefonlara kurulabilen **yerel Android uygulamasıdır**. Web sayfası veya demo HTML değildir. Java ile Android'in yerel API'leri kullanılarak yazılmıştır; internet, kayıt veya sunucu gerektirmez.

> **Önemli durum notu:** Bu teslimatta Android Studio proje **kaynak kodu** ve otomatik APK derleme iş akışı vardır. Buradaki çalışma ortamında Android SDK/Gradle ve internete erişim bulunmadığından APK dosyası derlenemedi, gerçek Android cihaz testi yapılamadı. Kodun Android üzerinde test edilmiş veya APK'sının hazır olduğu iddia edilmiyor. `RepeatCalculator` testleri yerel Java 17 üzerinde çalıştırılarak başarılı bulundu.

## Özellikler

- **Bugün / Yaklaşan / İstekler / Geçmiş** ekranları.
- Ürün adı, kategori, tahmini fiyat, öncelik ve not ekleme.
- İsteğe bağlı tarih ve saat; tarihsiz öğeler istek listesine gider.
- Tek seferlik, günlük, haftalık, aylık, yıllık ve X günde bir hatırlatma. 31 Ocak gibi ay sonları ve 29 Şubat için tarih sabitleme mantığı içerir.
- Ürün alındığında satın alma geçmişine kaydetme; tekrarlanan ürünün sonraki tarihini oluşturma.
- Listede **Aldım, Ertele, Düzenle/Sil** işlemleri.
- Android bildirimlerinden **Aldım / 1 saat ertele / Yarın** butonları.
- İnternetsiz SQLite saklama.
- Android 13+ bildirim izni; Android 12+ kesin alarm izni, izin yoksa yaklaşık zamanlı alarm.
- Telefon yeniden açıldığında ve saat/saat dilimi değiştiğinde gelecekteki hatırlatmaları yeniden planlama.
- Planlanan bütçe ve toplam satın alma harcaması.

## Android Studio ile APK oluşturma

1. Bilgisayara **Android Studio** (JDK 17 ve Android SDK ile) kur.
2. ZIP dosyasını bir klasöre çıkar.
3. Android Studio → **Open** → `Almali-Android` klasörünü seç.
4. SDK Platform **Android API 35** kurulu değilse SDK Manager'dan kur. Gradle senkronizasyonu için internet gereklidir.
5. Gradle sürümü olarak **8.11.1**, Android Gradle Plugin sürümü olarak **8.9.2** kullanılır. Proje gradle wrapper JAR dosyası içermediğinden, Android Studio yerel Gradle kurulumu veya GitHub Actions iş akışı gerekir. Android Studio'da Gradle dağıtımı sorulursa `8.11.1` seç/kur.
6. Build → **Build APK(s)** komutunu çalıştır.
7. Oluşan dosya: `app/build/outputs/apk/debug/app-debug.apk`.
8. APK'yı Android telefona kopyala, güvenilir kaynaktan yerel APK kurulumuna izin ver ve yükle.
9. İlk açılışta bildirim iznine onay ver. Ayarlar → **Bildirim / alarm izinleri** → **Tam zamanlı alarm** üzerinden özel alarm iznini de isteğe bağlı aç.

Alternatif CLI (Gradle 8.11.1 kuruluyken ve Android SDK yapılandırılmışken):

```bash
gradle assembleDebug
```

## GitHub Actions ile APK derleme

Bilgisayarda Android Studio yoksa projeyi kendi GitHub depona yükle. Repository'nin **Actions** sekmesinden **Android APK derlemesi** iş akışını çalıştır. Tamamlandığında `almali-debug-apk` adlı artifact'i indir ve ZIP içindeki `app-debug.apk` dosyasını telefona yükle.

Workflow: `.github/workflows/build-apk.yml`. Bu iş akışı burada çalıştırılmadı; derlemeyi GitHub'ın runner'ı gerçekleştirecek.

## Bildirim sisteminin teknik tasarımı

- **AlarmScheduler:** Her ürün için ürün ID'siyle eşleşen tek `PendingIntent` planlar, ürün düzenlendiğinde önce eski alarmı iptal eder.
- **ReminderReceiver:** Alarm geldiğinde SQLite'dan ürünü tekrar okuyup doğrular, sonra bildirim gösterir.
- **ActionReceiver:** Bildirimdeki eylemi işler; SQLite'yı günceller; sonraki alarmı planlar ve mevcut bildirimi kapatır.
- **RestoreReceiver:** `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` sonrası kaydedilmiş gelecekteki alarmları yeniden oluşturur.
- **Store:** SQLite `products` ve `purchase_history`; yinelenen işlemlerde tek DB transaction kullanılır.

**Android kısıtları:**
- Android 13+ bildirim iznini kullanıcı reddederse bildirimi göremez.
- Android 12+ cihazlarda kesin alarm izni yoksa `setAndAllowWhileIdle` kullanılır ve bildirim saatinde gecikme yaşanabilir.
- Üretici pil tasarrufu, force-stop ve sistem ayarları zamanlanmış alarmları engelleyebilir. Uygulamanın kullanıcı tarafından **zorla durdurulması** sonrasında Android alarmları yeniden etkinleştirmeyebilir; uygulamayı tekrar açmak gerekebilir.
- Telefon açıldıktan sonra geçmişte kalan öğeler bildirim yağmuruna dönüştürülmez; **Gecikti** olarak listelenir.
- İlk sürümde otomatik bulut yedekleme devre dışıdır; veriler cihazda saklanır, uygulamayı kaldırmak verileri silebilir.
- Tekrar sıklığı, **satın alma onayından sonra** yenilenir. Bir ürünü almadan gecikirse `Gecikti` olarak kalır.

## Kod konumu

```text
app/src/main/java/com/almali/app/
  MainActivity.java       — Android arayüzü
  Product.java            — Ürün modeli
  Store.java              — SQLite veri katmanı
  RepeatCalculator.java   — Tekrarlama tarih hesapları
  AlarmScheduler.java     — AlarmManager zamanlayıcı
  ReminderReceiver.java   — Bildirim gösterimi
  ActionReceiver.java     — Bildirim aksiyonları
  RestoreReceiver.java    — Yeniden başlatma / saat değişikliği
```

## Yerel Java testlerini çalıştırma

```bash
mkdir -p tests/out
javac -d tests/out app/src/main/java/com/almali/app/RepeatCalculator.java tests/RepeatCalculatorTest.java
java -cp tests/out RepeatCalculatorTest
```

## Sonraki geliştirmeler

- Ürün adını arama, filtreleme ve barkod okuma.
- Kullanıcının istediği günleri ayrı ayrı seçebileceği haftalık tekrar.
- Fiyat grafikleri, aylık bütçe limitleri ve yedekleme.
- UI/UX iyileştirmeleri, erişilebilirlik ve gerçek cihaz test otomasyonu.
- Hedef tarihsiz alışverişlerin de `Aldım` işlemiyle geçmişe alınabilmesi.

## Kaynaklar

- Android Developers: https://developer.android.com/develop/background-work/services/alarms
- Android Developers: https://developer.android.com/develop/ui/views/notifications/time-sensitive
- Android Developers: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
