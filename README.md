# Almalı 2.1 Beta — Haricî fiyat akışı & satıcı karşılaştırma altyapısı

Bu sürüm, **Almalı 2.0 Beta** üzerine geliştirilmiş Android kaynak kodudur. Yeni `applicationId=com.almali.app.beta21` kullanır, böylece farklı debug imzası nedeniyle mevcut Almalı Beta 2.0 uygulamasını silmen gerekmez. 2.1 ayrı uygulamadır; mevcut 2.0'ın kayıtlarını otomatik olarak aktarmaz.

## Eklenenler

- Önceki fiyat kayıtları, grafik, hedef fiyat uyarısı, **DEMO** akışı aynen bulunur.
- Kullanıcının girdiği **haricî HTTPS JSON fiyat akışını** (`scope=external`) okur.
- Bir ürüne ait en fazla 25 satıcı teklifini değerlendirir; stokta olan teklifleri fiyat+kargo toplamına göre sıralar.
- Teklifleri mağaza bağlantısıyla gösterir; **veri kaynağının doğrulanmadığını** açıkça belirtir.
- Yanlış ürün kodu, geçersiz fiyat, HTTP bağlantısı, eski gözlem (48+ saat), farklı para birimi ve aynı snapshot kontrolü eklenmiştir.
- `external` kaynaklar da hedef altı fiyat bildirimi üretebilir; bildirimde haricî verinin doğrulanmadığı belirtilir.
- SQLite veritabanında `current_offers` eklenmiştir. Version-1'dan version-2'ye geçişin SQL'i mevcut fiyat gözlemlerini silmez.

## Bu sürümün henüz yapmadığı şey

**Gerçek Trendyol fiyatını kendi başına çekmiyor.** Ücretsiz ve otomatik erişimine izin veren bir fiyat API'si bulmadan bu özellik etkin değil. Dış kaynak beyanları mağazadan doğrulanmış sayılmaz. Ürün adı, fotoğrafı ve geçmiş fiyatlar otomatik oluşturulmaz. Önceki demo sorgusu çalışmayı sürdürür.

## Test / GitHub

1. ZIP'i aç; `Almali-Android` içindeki `app`, `.github`, `docs`, `tests`, `tools` ve diğer kök dosyaları GitHub deponun köküne yükle.
2. GitHub → Actions → **Almalı 2.1 Android APK** işleminde başarılı derlemeye git.
3. Artifacts → `almali-v21-debug-apk` ZIP'ini indir; `app-debug.apk` dosyasını telefona aktar.
4. Telefonunda **Almalı 2.1 Beta** adlı ayrı uygulamayı aç.
5. Test için DEMO JSON adresini (eski testteki gibi) kullanabilirsin.
6. Bir sağlayıcıdan açıkça izinli canlı JSON akışı aldığında `docs/EXTERNAL-FEED.md` biçimiyle bağla.

**Önemli:** GitHub Actions başarıyla derlese bile Android telefonda kurulum ve işlev testi gerekir. Paket burada Android SDK olmadığı için APK olarak derlenmedi. Fiyat verisinin gerçekten mağazadan geldiğini veya kaynak lisansının uygunluğunu otomatik olarak kanıtlayamayız.

## Kaynaklar

- Teknik haricî JSON şartnamesi: `docs/EXTERNAL-FEED.md`
- Python doğrulama: `tools/validate_external_feed.py`
- Örnek TEST kaynağı: `docs/demo-feed.json`

## Gerçek otomatik fiyat karşılaştırması neden yok?

Trendyol'un resmî API'si satıcı/entegrasyon odaklıdır; herkese açık ücretsiz tüketici fiyat API'si olduğu doğrulanmadı. Bu sürüm **Trendyol HTML sayfalarından otomatik fiyat kazımaz**, gerçek fiyat gibi gösterilen sahte veriler üretmez ve geçmiş fiyat verisi uydurmaz. Gerçek sağlayıcı bağlanmadan otomatik gerçek fiyat, mağazalar arası canlı en ucuz fiyat veya gerçek indirim alarmı vaat etmiyoruz.

## Derleme sorunları

- Hata varsa GitHub Actions → başarısız `build` adımı → **ilk kırmızı hata satırını** gönderin.
- Yeni sürümü yine GitHub Actions'ta oluştururken debug imzası farklı olabilir; **Beta'nın sonraki sürümünü üzerine kurmak da garanti değil**. Kalıcı güvenli imzalama/yedekleme bir sonraki teknik adımdır.
