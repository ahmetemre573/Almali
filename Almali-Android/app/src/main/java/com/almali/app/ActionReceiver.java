package com.almali.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L);
        if (id <= 0) return;
        String action = intent.getAction();
        if (action == null) return;
        try (Store db = new Store(context)) {
            Product p = db.get(id);
            if (p == null || p.purchased) return;
            if (ReminderReceiver.ACTION_BUY.equals(action)) {
                if (db.purchase(id, p.estimatedPrice)) {
                    AlarmScheduler.schedule(context, db.get(id));
                }
            } else if (ReminderReceiver.ACTION_SNOOZE_HOUR.equals(action) || ReminderReceiver.ACTION_SNOOZE_DAY.equals(action)) {
                p.dueAt = System.currentTimeMillis() +
                    (ReminderReceiver.ACTION_SNOOZE_HOUR.equals(action) ? 60L * 60_000L : 24L * 60L * 60_000L);
                db.save(p);
                AlarmScheduler.schedule(context, p);
            } else {
                return;
            }
            ReminderReceiver.dismiss(context, id);
        }
    }
}
