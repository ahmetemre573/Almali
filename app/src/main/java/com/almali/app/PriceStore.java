package com.almali.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Fiyat beta verileri ayrı DB'de: mevcut Almalı alışverişleri asla silinmez. */
public final class PriceStore extends SQLiteOpenHelper {
    /** A single offer in a user-supplied external feed; not independently verified. */
    public static final class Offer {
        public String merchant = "";
        public String url = "";
        public double price;
        public double shipping;
        public double total;
        public long observedAt;
    }

    public static final String OLD_SPICE_URL = "https://www.trendyol.com/old-spice/captain-erkek-dus-jeli-ve-sampuan-3-u-1-arada-vucut-sac-yuz-1000-ml-ekstra-xl-p-797577544";
    public static final String OLD_SPICE_CODE = "797577544";

    public static final class Tracked {
        public long id;
        public String name = "";
        public String productCode = "";
        public String productUrl = "";
        public String imageUrl = "";
        public String feedUrl = "";
        public double targetPrice;
        public long lastCheckAt;
        public String lastCheckResult = "";
        public double alertedPrice;
    }

    public static final class Sample {
        public long id;
        public long trackedId;
        public double price;
        public String merchant;
        public String origin;
        public long observedAt;
    }

    public static final class RecordResult {
        public final boolean inserted;
        public final boolean alert;
        RecordResult(boolean inserted, boolean alert) {
            this.inserted = inserted;
            this.alert = alert;
        }
    }

    public PriceStore(Context c) { super(c, "almali_prices.db", null, 2); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tracked (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, product_code TEXT NOT NULL DEFAULT '', product_url TEXT NOT NULL DEFAULT '', image_url TEXT NOT NULL DEFAULT '', feed_url TEXT NOT NULL DEFAULT '', target_price REAL NOT NULL DEFAULT 0, last_check_at INTEGER NOT NULL DEFAULT 0, last_check_result TEXT NOT NULL DEFAULT '', alerted_price REAL NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE observations (id INTEGER PRIMARY KEY AUTOINCREMENT, tracked_id INTEGER NOT NULL, price REAL NOT NULL, merchant TEXT NOT NULL, origin TEXT NOT NULL, observed_at INTEGER NOT NULL, snapshot_id TEXT UNIQUE)");
        db.execSQL("CREATE INDEX observations_by_item ON observations(tracked_id, observed_at DESC)");
        createOfferTable(db);
    }
    private static void createOfferTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS current_offers (tracked_id INTEGER NOT NULL, merchant TEXT NOT NULL, price REAL NOT NULL, shipping REAL NOT NULL, total REAL NOT NULL, page_url TEXT NOT NULL, observed_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS current_offers_by_item ON current_offers(tracked_id, total)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createOfferTable(db);  // keep all existing observations
    }

    public void replaceOffers(long trackedId, List<Offer> rows) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("current_offers", "tracked_id=?", new String[]{String.valueOf(trackedId)});
            for (Offer o : rows) {
                ContentValues v = new ContentValues();
                v.put("tracked_id", trackedId); v.put("merchant", o.merchant);
                v.put("price", o.price); v.put("shipping", o.shipping); v.put("total", o.total);
                v.put("page_url", o.url); v.put("observed_at", o.observedAt);
                db.insertOrThrow("current_offers", null, v);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public List<Offer> offers(long trackedId) {
        List<Offer> offers = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("current_offers", null, "tracked_id=?",
              new String[]{String.valueOf(trackedId)}, null, null, "total ASC")) {
            while (c.moveToNext()) {
                Offer row = new Offer();
                row.merchant = c.getString(c.getColumnIndexOrThrow("merchant"));
                row.price = c.getDouble(c.getColumnIndexOrThrow("price"));
                row.shipping = c.getDouble(c.getColumnIndexOrThrow("shipping"));
                row.total = c.getDouble(c.getColumnIndexOrThrow("total"));
                row.url = c.getString(c.getColumnIndexOrThrow("page_url"));
                row.observedAt = c.getLong(c.getColumnIndexOrThrow("observed_at"));
                offers.add(row);
            }
        }
        return offers;
    }

    public void seedFirstItem(Context context) {
        if (context.getSharedPreferences("almali_price_beta", Context.MODE_PRIVATE).getBoolean("seeded_old_spice", false)) return;
        Tracked t = new Tracked();
        t.name = "Old Spice Captain 3'ü 1 Arada 1000 ml";
        t.productCode = OLD_SPICE_CODE;
        t.productUrl = OLD_SPICE_URL;
        t.targetPrice = 300;
        save(t);
        context.getSharedPreferences("almali_price_beta", Context.MODE_PRIVATE).edit().putBoolean("seeded_old_spice", true).apply();
    }

    private static Tracked tracked(Cursor c) {
        Tracked t = new Tracked();
        t.id = c.getLong(c.getColumnIndexOrThrow("id"));
        t.name = c.getString(c.getColumnIndexOrThrow("name"));
        t.productCode = c.getString(c.getColumnIndexOrThrow("product_code"));
        t.productUrl = c.getString(c.getColumnIndexOrThrow("product_url"));
        t.imageUrl = c.getString(c.getColumnIndexOrThrow("image_url"));
        t.feedUrl = c.getString(c.getColumnIndexOrThrow("feed_url"));
        t.targetPrice = c.getDouble(c.getColumnIndexOrThrow("target_price"));
        t.lastCheckAt = c.getLong(c.getColumnIndexOrThrow("last_check_at"));
        t.lastCheckResult = c.getString(c.getColumnIndexOrThrow("last_check_result"));
        t.alertedPrice = c.getDouble(c.getColumnIndexOrThrow("alerted_price"));
        return t;
    }

    public List<Tracked> all() {
        List<Tracked> items = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("tracked", null, null, null, null, null, "id DESC")) {
            while (c.moveToNext()) items.add(tracked(c));
        }
        return items;
    }

    public Tracked get(long id) {
        try (Cursor c = getReadableDatabase().query("tracked", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? tracked(c) : null;
        }
    }

    public long save(Tracked t) {
        ContentValues v = new ContentValues();
        v.put("name", t.name);
        v.put("product_code", t.productCode);
        v.put("product_url", t.productUrl);
        v.put("image_url", t.imageUrl);
        v.put("feed_url", t.feedUrl);
        v.put("target_price", t.targetPrice);
        v.put("last_check_at", t.lastCheckAt);
        v.put("last_check_result", t.lastCheckResult);
        v.put("alerted_price", t.alertedPrice);
        if (t.id == 0) t.id = getWritableDatabase().insertOrThrow("tracked", null, v);
        else {
            Tracked previous = get(t.id);
            getWritableDatabase().update("tracked", v, "id=?", new String[]{String.valueOf(t.id)});
            // Price offers belong to a particular source. Never show old data under a new URL.
            if (previous != null && !previous.feedUrl.equals(t.feedUrl))
                getWritableDatabase().delete("current_offers", "tracked_id=?", new String[]{String.valueOf(t.id)});
        }
        return t.id;
    }

    public void remove(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("observations", "tracked_id=?", new String[]{String.valueOf(id)});
            db.delete("current_offers", "tracked_id=?", new String[]{String.valueOf(id)});
            db.delete("tracked", "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public Sample latest(long id) {
        try (Cursor c = getReadableDatabase().query("observations", null, "tracked_id=?", new String[]{String.valueOf(id)}, null, null, "observed_at DESC, id DESC", "1")) {
            return c.moveToFirst() ? sample(c) : null;
        }
    }

    private static Sample sample(Cursor c) {
        Sample s = new Sample();
        s.id = c.getLong(c.getColumnIndexOrThrow("id"));
        s.trackedId = c.getLong(c.getColumnIndexOrThrow("tracked_id"));
        s.price = c.getDouble(c.getColumnIndexOrThrow("price"));
        s.merchant = c.getString(c.getColumnIndexOrThrow("merchant"));
        s.origin = c.getString(c.getColumnIndexOrThrow("origin"));
        s.observedAt = c.getLong(c.getColumnIndexOrThrow("observed_at"));
        return s;
    }

    public List<Sample> samples(long id, int limit) {
        List<Sample> samples = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("observations", null, "tracked_id=?", new String[]{String.valueOf(id)}, null, null, "observed_at DESC, id DESC", String.valueOf(Math.max(1, Math.min(limit, 500))))) {
            while (c.moveToNext()) samples.add(sample(c));
        }
        return samples;
    }

    /**
     * Tek işlem: aynı demo snapshot ikinci kez fiyat kaydı oluşturmaz.
     * MANUAL kayıtları snapshotId=null olmalıdır. Uyarı yalnızca hedef altında
     * ilk gözlemde veya daha düşük yeni bir seviyeye inilince üretilir.
     */
    public RecordResult record(long id, double price, String merchant, String origin, String snapshotId) {
        if (!Double.isFinite(price) || price <= 0 || price > 1_000_000_000d) throw new IllegalArgumentException("Geçersiz fiyat");
        if (!"MANUAL".equals(origin) && !"DEMO_FEED".equals(origin) && !"EXTERNAL_FEED".equals(origin)) throw new IllegalArgumentException("Kaynak tipi geçersiz");
        if (merchant == null || merchant.trim().isEmpty()) throw new IllegalArgumentException("Mağaza adı boş");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Tracked t;
            try (Cursor c = db.query("tracked", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
                if (!c.moveToFirst()) return new RecordResult(false, false);
                t = tracked(c);
            }
            ContentValues vals = new ContentValues();
            vals.put("tracked_id", id);
            vals.put("price", price);
            vals.put("merchant", merchant.trim());
            vals.put("origin", origin);
            vals.put("observed_at", System.currentTimeMillis());
            if (snapshotId != null) vals.put("snapshot_id", id + ":" + snapshotId);
            long inserted = db.insertWithOnConflict("observations", null, vals, SQLiteDatabase.CONFLICT_IGNORE);
            if (inserted == -1) {
                db.setTransactionSuccessful();
                return new RecordResult(false, false);
            }
            boolean alert = PriceRules.shouldAlert(price, t.targetPrice, t.alertedPrice);
            ContentValues update = new ContentValues();
            double newAlert = PriceRules.nextAlertPrice(price, t.targetPrice, t.alertedPrice);
            if (Double.compare(newAlert, t.alertedPrice) != 0) update.put("alerted_price", newAlert);
            if (update.size() > 0) db.update("tracked", update, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return new RecordResult(true, alert);
        } finally { db.endTransaction(); }
    }

    public void checkResult(long id, boolean ok, String detail) {
        ContentValues v = new ContentValues();
        v.put("last_check_at", System.currentTimeMillis());
        v.put("last_check_result", (ok ? "BAŞARILI: " : "HATA: ") + detail);
        getWritableDatabase().update("tracked", v, "id=?", new String[]{String.valueOf(id)});
    }
}
