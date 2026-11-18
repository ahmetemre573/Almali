# Almalı 2.1 — Fiyat veri kaynağı sözleşmesi

**Bu uygulama Trendyol, Akakçe, Cimri veya başka bir siteyi otomatik kazımaz.** Üçüncü taraf korumalarını aşan yazılımlar kullanmaz. Kaynak izni ve fiyat doğruluğu **bağımsız olarak doğrulanmamıştır**. `external` bir resmî API onayı anlamına gelmez.

## Kullanım amacı

Bir satıcı veya veri sağlayıcı **otomatik veri kullanımına izin veren, düzenli güncellenen HTTPS JSON fiyat akışı** verirse Almalı bu bilgiyi okuyabilir. Telefon senin ayarladığın HTTPS dosyasını WorkManager ile yaklaşık saatlik aralıklarla kontrol eder. Android kesin kontrol saati garantisi vermez.

## JSON şeması (ÖRNEKTİR, fiyatlar gerçek değildir)

```json
{
  "scope": "external",
  "product_code": "797577544",
  "currency": "TRY",
  "source_name": "TEST sağlayıcısı - canlı mağaza verisi değil",
  "snapshot_id": "2026-11-18T18:00:00Z-0001",
  "observed_at": "2026-11-18T18:00:00Z",
  "offers": [
    {
      "product_code": "797577544",
      "merchant": "ÖRNEK A",
      "price": 285.00,
      "shipping": 35.00,
      "in_stock": true,
      "url": "https://example.com/urun-a"
    },
    {
      "product_code": "797577544",
      "merchant": "ÖRNEK B",
      "price": 310.00,
      "shipping": 0.00,
      "in_stock": true,
      "url": "https://example.com/urun-b"
    }
  ]
}
```

Burada en düşük kargo dahil toplam 310 TL'dir (B mağazası). Şema üretim kullanımında gerçek mağazadan yetkili olarak sağlanan ürün kodu, birim fiyat, kargo, stok ve URL değerlerini içermeli. Ürün kodu yanlışsa sonuç reddedilir; **ancak doğru yazılmış kodun doğru ürüne ait olduğunu yazılım bağımsız doğrulayamaz**. Eski (48 saat+) fiyat da reddedilir.

`source_name` ve `scope=external` yalnızca kullanıcı tarafından sağlanan beyanlardır. Uygulama fiyatları **Haricî akış · doğrulanmadı** etiketiyle gösterir. `snapshot_id` her veri güncellemesinde değişmeli; aynı kimlik yeniden alındığında fiyat kaydı ve bildirim tekrarlanmaz. `observed_at` güncel ISO-8601 UTC olmalı.

## Kaynağı bağlama

1. Kullanım izni olan ve otomatik güncellenen HTTPS JSON URL'si edin.
2. Almalı 2.1 Beta → Fiyat Takip → Ürün → Düzenle → JSON fiyat akışı adresine URL'yi gir.
3. Ürün kodunun JSON'daki `product_code` ile aynı olduğuna dikkat et.
4. **Fiyat akışını şimdi sorgula** düğmesine bas; satıcı listesinde kargo dahil toplamlar küçükten büyüğe sıralanır.
5. Uygulamanın saatlik kontrolünü daha sonra bildiriminle doğrula.

## Dürüst durum

- Mevcut testte canlı Trendyol fiyatı için **izinli ve ücretsiz tüketici API'si doğrulanmadı**.
- Aynı sebeple mevcut ZIP dosyası **tek başına** gerçek fiyatları getirmez. Yetkili veri kaynağı sağlandığında yeni APK kodu yazmadan bağlanabilir.
- Uygulamanın güncel verdiği DEMO feed kaynağı aynen çalışmaya devam eder.
- Bir web sayfasının HTML olarak herkes tarafından görülebilmesi, otomatik veri çekme ve yeniden yayımlama hakkı vermez.
- API anahtarı veya özel token varsa Android APK'ya yahut herkese açık GitHub dosyasına koyma; bunu ancak sunucu tarafında koruyarak kullan.

## Yerel sözleşme testi

```bash
python -m unittest discover -s tests -p 'test_*.py'
python tools/validate_external_feed.py /path/to/authorized-feed.json
```

Bu testler veri **şemasını** doğrular; satıcının veriyi gerçekten sağladığını veya kullanım izni bulunduğunu kanıtlamaz.
