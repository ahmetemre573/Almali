# Gerçek Android cihaz testi — sürüm 0.1

APK derlendikten sonra aşağıdaki kontroller gerçek bir Android telefonda yapılmalıdır. Bu listedeki testler **bu ortamda henüz çalıştırılmadı**.

- [ ] Yeni ürün ekle, uygulamayı kapatıp aç; ürün hâlâ listede.
- [ ] Bir ürün için 2 dakika sonrası alarm ayarla; ekran kilitli ve uygulama arka plandayken bildirim gelsin.
- [ ] Bildirimde **1 saat ertele** butonuna bas; planlanan tarih güncellensin.
- [ ] Bildirimde **Yarın** butonuna bas; yeni alarm ayarlansın.
- [ ] Bildirimde **Aldım** butonuna bas; işlem geçmişe eklensin.
- [ ] Tek seferlik ürün satın alınırsa listeden çıksın.
- [ ] Her hafta tekrar eden ürün alınınca gelecek haftanın hatırlatması kurulsun.
- [ ] Ürün düzenlenince eski alarm iptal edilsin.
- [ ] Ürün silinince eski alarm iptal edilsin; geçmiş satın almalar korunsun.
- [ ] İstek listesine tarihsiz ürün ekle; alarm üretilmesin.
- [ ] Telefon yeniden başlat; gelecek alarm korunmuş olsun.
- [ ] Android 13+ bildirim izni reddedildiğinde uygulama çökmesin.
- [ ] Android 14+ kesin alarm izni reddedilirse yaklaşık alarm kurulsun.
- [ ] Koyu mod ve büyük font ölçeğinde metinler okunabilir kalsın (ilk sürümde tema genişletmesi gerekebilir).

> Bildirim saatlerinin kesinliği Android sürümüne, kullanıcının verdiği izinlere ve cihaz üreticisinin güç yönetimine bağlıdır. `AlarmManager` zamanlama sistemi bile mutlak teslim garantisi vermez.

## 2.0 Beta fiyat testleri

- [ ] Eski Almalı uygulaması yerinde, yeni Almalı Beta ayrı kuruldu.
- [ ] Old Spice ürün kodu 797577544, hedef fiyatı 300 TL, başlangıçta fiyat yok.
- [ ] Elle 379 TL ve 299,90 TL kayıtları grafikte gösterildi.
- [ ] Uygulama kapatılıp açıldığında kayıtlar kaybolmadı.
- [ ] 299,90 TL manuel kayıt, izin varsa uygun kaynak etiketiyle bildirim verdi.
- [ ] DEMO JSON üzerinden anlık kontrol yeni kayıt ekledi.
- [ ] Aynı snapshot_id tekrar sorgulanınca ikinci kayıt veya bildirim üretmedi.
- [ ] Hatalı ürün kodu/para birimi/URL hata olarak göründü.
- [ ] WorkManager yaklaşık saatlik aralıkta deneme yaptı (tam saat garanti değil).
