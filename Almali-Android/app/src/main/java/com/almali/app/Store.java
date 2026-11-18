package com.almali.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public final class Store extends SQLiteOpenHelper {
    public static final class History {
        public long id;
        public long purchasedAt;
        public String name;
        public String category;
        public double amount;
    }

    public Store(Context context) {
        super(context, "almali.db", null, 1);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT NOT NULL, estimated_price REAL NOT NULL DEFAULT 0, due_at INTEGER NOT NULL DEFAULT -1, repeat_rule TEXT NOT NULL DEFAULT 'NONE', custom_days INTEGER NOT NULL DEFAULT 7, anchor_day INTEGER NOT NULL DEFAULT 0, anchor_month INTEGER NOT NULL DEFAULT 0, priority INTEGER NOT NULL DEFAULT 1, note TEXT NOT NULL DEFAULT '', purchased INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE purchase_history (id INTEGER PRIMARY KEY AUTOINCREMENT, product_id INTEGER NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, amount REAL NOT NULL DEFAULT 0, purchased_at INTEGER NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    private static Product fromCursor(Cursor c) {
        Product p = new Product();
        p.id = c.getLong(c.getColumnIndexOrThrow("id"));
        p.name = c.getString(c.getColumnIndexOrThrow("name"));
        p.category = c.getString(c.getColumnIndexOrThrow("category"));
        p.estimatedPrice = c.getDouble(c.getColumnIndexOrThrow("estimated_price"));
        p.dueAt = c.getLong(c.getColumnIndexOrThrow("due_at"));
        p.repeat = c.getString(c.getColumnIndexOrThrow("repeat_rule"));
        p.customDays = c.getInt(c.getColumnIndexOrThrow("custom_days"));
        p.anchorDay = c.getInt(c.getColumnIndexOrThrow("anchor_day"));
        p.anchorMonth = c.getInt(c.getColumnIndexOrThrow("anchor_month"));
        p.priority = c.getInt(c.getColumnIndexOrThrow("priority"));
        p.note = c.getString(c.getColumnIndexOrThrow("note"));
        p.purchased = c.getInt(c.getColumnIndexOrThrow("purchased")) != 0;
        return p;
    }

    private static ContentValues values(Product p) {
        ContentValues cv = new ContentValues();
        cv.put("name", p.name);
        cv.put("category", p.category);
        cv.put("estimated_price", p.estimatedPrice);
        cv.put("due_at", p.dueAt);
        cv.put("repeat_rule", p.repeat);
        cv.put("custom_days", p.customDays);
        cv.put("anchor_day", p.anchorDay);
        cv.put("anchor_month", p.anchorMonth);
        cv.put("priority", p.priority);
        cv.put("note", p.note);
        cv.put("purchased", p.purchased ? 1 : 0);
        return cv;
    }

    public long save(Product p) {
        SQLiteDatabase db = getWritableDatabase();
        if (p.id <= 0) {
            p.id = db.insertOrThrow("products", null, values(p));
        } else {
            db.update("products", values(p), "id=?", new String[]{String.valueOf(p.id)});
        }
        return p.id;
    }

    public Product get(long id) {
        try (Cursor c = getReadableDatabase().query("products", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    public List<Product> all() {
        List<Product> items = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("products", null, null, null, null, null, "priority DESC, due_at ASC, id DESC")) {
            while (c.moveToNext()) items.add(fromCursor(c));
        }
        return items;
    }

    public List<History> history() {
        List<History> items = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("purchase_history", null, null, null, null, null, "purchased_at DESC")) {
            while (c.moveToNext()) {
                History h = new History();
                h.id = c.getLong(c.getColumnIndexOrThrow("id"));
                h.name = c.getString(c.getColumnIndexOrThrow("name"));
                h.category = c.getString(c.getColumnIndexOrThrow("category"));
                h.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
                h.purchasedAt = c.getLong(c.getColumnIndexOrThrow("purchased_at"));
                items.add(h);
            }
        }
        return items;
    }

    /** Tek transaction: çift tıklama / art arda bildirim aksiyonları yinelenen satın alımı önler. */
    public boolean purchase(long id, double amount) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Product p;
            try (Cursor c = db.query("products", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
                if (!c.moveToFirst()) return false;
                p = fromCursor(c);
            }
            if (p.purchased || p.dueAt <= 0) return false;
            long now = System.currentTimeMillis();
            ContentValues history = new ContentValues();
            history.put("product_id", id);
            history.put("name", p.name);
            history.put("category", p.category);
            history.put("amount", amount);
            history.put("purchased_at", now);
            db.insertOrThrow("purchase_history", null, history);
            long next = RepeatCalculator.next(p.dueAt, now, p.repeat, p.customDays, p.anchorDay, p.anchorMonth);
            if (next <= 0) {
                p.purchased = true;
            } else {
                p.dueAt = next;
            }
            db.update("products", values(p), "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public void delete(long id) {
        // History is an independent purchase snapshot, intentionally retained.
        getWritableDatabase().delete("products", "id=?", new String[]{String.valueOf(id)});
    }
}
