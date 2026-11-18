package com.almali.app;

import android.content.Context;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Yalnızca kullanıcının tanımladığı HTTPS JSON akışını okur.
 * scope=demo zorunlu: Resmî Trendyol API'siymiş gibi davranılmaz.
 * Haricî HTML sayfalarından fiyat kazımak YOKTUR.
 */
public final class DemoPriceFeed {
    private DemoPriceFeed() { }

    public static String check(Context context, PriceStore.Tracked item) throws Exception {
        if (item.feedUrl == null || item.feedUrl.trim().isEmpty()) throw new IllegalArgumentException("Test akışı URL'si yok");
        URL url = new URL(item.feedUrl.trim());
        if (!"https".equalsIgnoreCase(url.getProtocol())) throw new IllegalArgumentException("Yalnızca HTTPS bağlantısı kabul edilir");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(7000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Almali-Beta-DemoFeed/0.2");
        String json;
        try {
            if (conn.getResponseCode() != 200) throw new IllegalArgumentException("HTTP " + conn.getResponseCode());
            String ct = conn.getContentType();
            if (ct != null && !ct.toLowerCase(java.util.Locale.ROOT).contains("json") && !ct.toLowerCase(java.util.Locale.ROOT).contains("text/plain"))
                throw new IllegalArgumentException("JSON dosyası bekleniyordu");
            try (InputStream is = conn.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = is.read(buffer)) != -1) {
                    bytes.write(buffer, 0, n);
                    if (bytes.size() > 32768) throw new IllegalArgumentException("Veri 32 KB sınırını aştı");
                }
                json = bytes.toString(StandardCharsets.UTF_8.name());
            }
        } finally { conn.disconnect(); }
        JSONObject obj = new JSONObject(json);
        if (!"demo".equals(obj.optString("scope", ""))) throw new IllegalArgumentException("scope=demo gerekli");
        if (!item.productCode.equals(obj.optString("product_code", ""))) throw new IllegalArgumentException("Ürün kodu uyuşmuyor");
        if (!"TRY".equals(obj.optString("currency", ""))) throw new IllegalArgumentException("TRY dışında para birimi desteklenmiyor");
        double price = obj.getDouble("price");
        String snapshot = obj.getString("snapshot_id").trim();
        String merchant = obj.optString("merchant", "Demo mağaza").trim();
        if (snapshot.length() < 3 || snapshot.length() > 100) throw new IllegalArgumentException("snapshot_id 3–100 karakter olmalı");
        if (merchant.isEmpty() || merchant.length() > 100) throw new IllegalArgumentException("Mağaza adı geçersiz");
        if (!Double.isFinite(price) || price <= 0 || price > 1_000_000_000d) throw new IllegalArgumentException("Fiyat geçersiz");
        // Değişiklik varsa veri ekle. Aynı snapshot tekrar işlendiğinde bildirim tekrarlanmaz.
        try (PriceStore db = new PriceStore(context)) {
            PriceStore.Tracked fresh = db.get(item.id);
            if (fresh == null || !item.feedUrl.equals(fresh.feedUrl)) return "Takip silindi / akış değişti";
            PriceStore.RecordResult result = db.record(item.id, price, merchant, "DEMO_FEED", snapshot);
            db.checkResult(item.id, true, result.inserted ? "Yeni TEST fiyatı kaydedildi" : "Yeni kayıt yok (aynı snapshot)");
            if (result.alert) PriceAlertNotifier.notifyPrice(context, fresh, price, "DEMO_FEED");
            return result.inserted ? "Yeni TEST fiyatı kaydedildi" : "Değişiklik yok";
        }
    }
}
