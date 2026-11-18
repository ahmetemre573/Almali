package com.almali.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * HTTPS JSON price feed reader. Does not scrape websites or bypass access controls.
 * scope=demo: synthetic QA feed. scope=external: user-supplied third-party feed,
 * clearly labelled UNVERIFIED in the UI; source permission is user's responsibility.
 */
public final class DemoPriceFeed {
    private DemoPriceFeed() { }

    static String asHttps(String text) {
        try {
            URL url = new URL(text);
            if (!"https".equalsIgnoreCase(url.getProtocol()) || url.getHost().isEmpty())
                throw new IllegalArgumentException("Yalnızca HTTPS bağlantısı kabul edilir");
            if (url.getUserInfo() != null) throw new IllegalArgumentException("Bağlantıya parola eklemeyin");
            return url.toExternalForm();
        } catch (Exception error) { throw new IllegalArgumentException("Geçersiz HTTPS adresi"); }
    }

    private static String fetch(String feedUrl) throws Exception {
        URL url = new URL(asHttps(feedUrl));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Almali-Beta-PriceFeed/0.3");
        try {
            if (conn.getResponseCode() != 200) throw new IllegalArgumentException("HTTP " + conn.getResponseCode());
            String ct = conn.getContentType();
            if (ct != null && !ct.toLowerCase(Locale.ROOT).contains("json") && !ct.toLowerCase(Locale.ROOT).contains("text/plain"))
                throw new IllegalArgumentException("JSON dosyası bekleniyordu");
            try (InputStream stream = conn.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096]; int n;
                while ((n = stream.read(buf)) != -1) {
                    bytes.write(buf, 0, n);
                    if (bytes.size() > 65536) throw new IllegalArgumentException("Akış 64 KB sınırını aştı");
                }
                return bytes.toString(StandardCharsets.UTF_8.name());
            }
        } finally { conn.disconnect(); }
    }

    private static double validMoney(double v) {
        if (!Double.isFinite(v) || v <= 0 || v > 1_000_000_000d)
            throw new IllegalArgumentException("Geçersiz fiyat");
        return v;
    }

    private static String validSnapshot(JSONObject obj) throws Exception {
        String snapshot = obj.getString("snapshot_id").trim();
        if (snapshot.length() < 3 || snapshot.length() > 100)
            throw new IllegalArgumentException("snapshot_id uzunluğu 3–100 olmalı");
        return snapshot;
    }

    private static String shortName(String s) {
        if (s == null || s.trim().isEmpty() || s.trim().length() > 100)
            throw new IllegalArgumentException("Satıcı veya kaynak adı geçersiz");
        return s.trim();
    }

    public static String check(Context context, PriceStore.Tracked item) throws Exception {
        if (item.feedUrl == null || item.feedUrl.trim().isEmpty()) throw new IllegalArgumentException("Fiyat akışı adresi boş");
        JSONObject obj = new JSONObject(fetch(item.feedUrl.trim()));
        String scope = obj.optString("scope", "");
        if (!"demo".equals(scope) && !"external".equals(scope))
            throw new IllegalArgumentException("Yalnızca demo veya external JSON akışı kabul edilir");
        if (item.productCode == null || item.productCode.isEmpty() || !item.productCode.equals(obj.optString("product_code", "")))
            throw new IllegalArgumentException("Ürün kodu uyuşmuyor");
        if (!"TRY".equals(obj.optString("currency", "")))
            throw new IllegalArgumentException("TRY dışında para birimi desteklenmiyor");
        String snapshot = validSnapshot(obj);
        boolean external = "external".equals(scope);
        double bestPrice;
        String bestMerchant;
        List<PriceStore.Offer> offers = new ArrayList<>();

        if (!external) {
            bestPrice = validMoney(obj.getDouble("price"));
            bestMerchant = shortName(obj.optString("merchant", "Demo mağaza"));
        } else {
            // All data in external feeds is user-supplied: not independently authenticated.
            shortName(obj.getString("source_name"));
            long observed;
            try { observed = Instant.parse(obj.getString("observed_at")).toEpochMilli(); }
            catch (Exception e) { throw new IllegalArgumentException("observed_at ISO-8601 olmalı"); }
            long now = System.currentTimeMillis();
            if (observed > now + 600_000L || observed < now - 48L * 3_600_000L)
                throw new IllegalArgumentException("Fiyat verisi 48 saatten eski veya gelecekte");
            JSONArray array = obj.getJSONArray("offers");
            if (array.length() < 1 || array.length() > 25)
                throw new IllegalArgumentException("Teklif sayısı 1–25 olmalı");
            for (int i = 0; i < array.length(); i++) {
                JSONObject offer = array.getJSONObject(i);
                if (!item.productCode.equals(offer.getString("product_code")))
                    throw new IllegalArgumentException("Teklifte farklı ürün kodu var");
                if (!offer.optBoolean("in_stock", false)) continue;
                PriceStore.Offer row = new PriceStore.Offer();
                row.merchant = shortName(offer.getString("merchant"));
                row.price = validMoney(offer.getDouble("price"));
                row.shipping = offer.getDouble("shipping");
                if (!Double.isFinite(row.shipping) || row.shipping < 0 || row.shipping > 1_000_000d)
                    throw new IllegalArgumentException("Kargo fiyatı geçersiz");
                row.total = validMoney(row.price + row.shipping);
                row.url = asHttps(offer.getString("url"));
                row.observedAt = observed;
                offers.add(row);
            }
            if (offers.isEmpty()) throw new IllegalArgumentException("Stokta uygun teklif yok");
            offers.sort(Comparator.comparingDouble(a -> a.total));
            PriceStore.Offer best = offers.get(0);
            bestPrice = best.total;
            bestMerchant = best.merchant;
        }

        try (PriceStore db = new PriceStore(context)) {
            PriceStore.Tracked fresh = db.get(item.id);
            if (fresh == null || !item.feedUrl.equals(fresh.feedUrl)) return "Takip silindi veya akış değişti";
            String origin = external ? "EXTERNAL_FEED" : "DEMO_FEED";
            PriceStore.RecordResult result = db.record(item.id, bestPrice, bestMerchant, origin, snapshot);
            if (external) db.replaceOffers(item.id, offers);
            db.checkResult(item.id, true, result.inserted ? "Yeni fiyat kaydı" : "Aynı snapshot; yeni kayıt yok");
            if (result.alert) PriceAlertNotifier.notifyPrice(context, fresh, bestPrice, origin);
            return result.inserted ? (external ? "Dış kaynak fiyatı kaydedildi (doğrulanmadı)" : "Yeni TEST fiyatı kaydedildi") : "Değişiklik yok";
        }
    }
}
