package com.almali.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import java.util.List;

public final class AlarmScheduler {
    public static final String EXTRA_ID = "product_id";
    private AlarmScheduler() {}

    private static PendingIntent alarmIntent(Context context, long id) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(EXTRA_ID, id);
        intent.setAction("com.almali.app.REMINDER." + id);
        return PendingIntent.getBroadcast(context, (int)id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean canBeExact(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return mgr.canScheduleExactAlarms();
    }

    public static void schedule(Context context, Product p) {
        if (p == null || p.id <= 0) return;
        cancel(context, p.id);
        if (p.purchased || p.dueAt <= System.currentTimeMillis()) return;
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = alarmIntent(context, p.id);
        if (canBeExact(context)) {
            try {
                mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.dueAt, pi);
                return;
            } catch (SecurityException ignored) { /* permission revoked mid-call */ }
        }
        mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.dueAt, pi);
    }

    public static void cancel(Context context, long id) {
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(alarmIntent(context, id));
    }

    public static void rescheduleAll(Context context) {
        try (Store db = new Store(context)) {
            List<Product> products = db.all();
            for (Product p : products) schedule(context, p);
        }
    }
}
