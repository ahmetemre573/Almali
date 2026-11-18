# Almalı 2.0 Beta yükleme adımları

1. ZIP dosyasını bilgisayarda **Tümünü ayıkla** ile çıkartın.
2. `Almali-Android` klasörünün içine girin. **Klasörün kendisini değil içindekileri** yükleyin.
3. GitHub'daki kendi `Almali` deponuzun **Code** sayfasını açın.
4. `Add file` > `Upload files` seçin. Klasör içindeki **tüm klasör ve dosyaları** yükleme alanına sürükleyin: `app`, `.github`, `tests`, `docs`, `build.gradle`, `settings.gradle` vb.
5. `Commit changes` seçin.
6. `Actions` > **Almalı Beta Android APK** işlemine girin. En yeni çalıştırma yeşile dönünce açın.
7. `Artifacts` altından **almali-beta-debug-apk** indirin. ZIP'i ayıklayın ve `app-debug.apk` dosyasını telefonunuza atıp kurun.
8. Telefonda **Almalı Beta** simgesi açılır. Eski **Almalı** silinmez; hatırlatmaları olduğu gibi kalır.
9. `Fiyat Takip (Beta)` > `Old Spice` > `+ Fiyat kaydet` ile 379 TL, ardından 299,90 TL test edin.
10. Otomatik DEMO testi için `docs/DEMO-TEST.md` dosyasındaki talimatlara bakın.

**UYARI:** Bu sürüm canlı Trendyol fiyatı çekmez. `299,90 TL` test girdisi gerçek mağaza fiyatı değildir.

**HATA:** Actions kırmızıya dönerse `build` bölümündeki ilk kırmızı hata satırını paylaşın. Bu sürümün Android APK derlemesi mevcut ortamda çalıştırılamadı; Java mantık testleri geçti.
