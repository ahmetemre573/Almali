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
