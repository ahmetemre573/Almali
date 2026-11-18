# Almalı 2.0 — 0 TL fiyat otomasyonu testi

**Bu çalışma gerçek Trendyol, Amazon veya başka bir e-ticaret mağazasından fiyat okumaz.**
`docs/demo-feed.json` yalnızca kendi kontrol ettiğiniz bir test akışıdır.

## Testin amacı

1. Old Spice Captain 1000 ml (kod `797577544`) ürününü takip listesinde görüntülemek.
2. Hedef fiyatı örnek olarak `300 TL` belirlemek.
3. Elle fiyat eklemek, fiyat geçmişini ve grafiği görmek.
4. Kontrol ettiğiniz HTTPS JSON kaynağından veri çekmek.
5. Fiyat hedef altına inerse **TEST bildirimi** görmek.
6. Android'in izin verdiği zamanda yaklaşık bir saatte bir kontrol yapılmasını test etmek.

## İlk elle test

- `Almalı Beta` uygulamasını açın → `Fiyat Takip (Beta)`.
- Old Spice ürününün eklendiğini ve başlangıçta **fiyatı bilinmiyor** yazdığını kontrol edin.
- Ürüne girin → `+ Fiyat kaydet` → mağaza `Trendyol`, fiyat `379`.
- Sonra fiyatı `299,90` olarak ayrıca kaydedin; bildirim izni varsa **elle kaydedildi** mesajlı hedef fiyat bildirimi gelmeli.
- Geçmiş grafiğinde iki fiyat noktası görünmeli.

## Ücretsiz otomatik TEST akışı kurma

> GitHub'daki Android kaynak projeniz **Private** olabilir ve öyle kalabilir. JSON akışı için ayrıca **public** bir depo kullanın. İki depoda da şifre/anahtar paylaşmayın.

1. GitHub'da yeni, boş ve **Public** bir `almali-fiyat-test` deposu oluşturun.
2. Bu depoya `docs/demo-feed.json` içeriğini `old-spice.json` adıyla yükleyin.
3. GitHub'da `old-spice.json` dosyasını açıp **Raw** bağlantısını kopyalayın. `https://raw.githubusercontent.com/.../old-spice.json` şeklinde olmalı.
4. Telefonunuzda `Almalı Beta` → `Fiyat Takip` → Old Spice → `Düzenle` → `DEMO JSON HTTPS adresi` alanına bu URL'yi yapıştırın.
5. Kaydedin ve `Test akışını şimdi sorgula` düğmesine basın.
6. TEST verisi `299,90 TL` olarak fiyat geçmişine kaydolacak ve hedef fiyat alarmı gösterilebilecek.
7. `old-spice.json` dosyasındaki `price` alanını `279.90`, `snapshot_id` alanını `old-spice-test-002` olarak değiştirip GitHub'a kaydedin.
8. Yeniden sorgulayınca ikinci test verisi kaydedilmeli. **snapshot_id değiştirilmezse tekrar kayıt oluşmaz.**

Saatlik sorgulama WorkManager ile planlanır. Doze/pil optimizasyonları ve ağ erişimi nedeniyle **tam 60 dakikada bir çalışacağı garanti edilmez**. Uygulama yüklü ve açıkken `Şimdi sorgula` hızlı fonksiyon testidir.

## JSON biçimi

```json
{
  "scope": "demo",
  "product_code": "797577544",
  "currency": "TRY",
  "merchant": "TEST MAGAZASI - GERCEK FIYAT DEGIL",
  "price": 299.90,
  "snapshot_id": "old-spice-test-001"
}
```

- `scope` değeri **demo** olmalıdır.
- `product_code` takip edilen ürünün koduyla aynı olmalıdır.
- Para birimi sadece `TRY`.
- `snapshot_id` bir fiyat gözleminde benzersiz olmalı; aynı değerin tekrar okunması bildirim oluşturmaz.
- Veri kaynağı HTTPS JSON olmalı, en fazla 32 KB olmalı.
- Bu yöntem mağaza fiyatı karşılaştırması veya gerçek tarihsel veri sağlamaz. Gerçek veri kaynağı için lisans/izin ve güvenilir API gereklidir.
