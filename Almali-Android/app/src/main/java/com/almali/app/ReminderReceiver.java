package com.almali.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;

public final class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "shopping_reminders";
    public static final String ACTION_BUY = "com.almali.app.BUY";
    public static final String ACTION_SNOOZE_HOUR = "com.almali.app.SNOOZE_HOUR";
    public static final String ACTION_SNOOZE_DAY = "com.almali.app.SNOOZE_DAY";

    public static void createChannel(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Alışveriş hatırlatmaları", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Almanız gereken ürünlerin zamanlanmış hatırlatmaları");
        nm.createNotificationChannel(channel);
    }

    public static void dismiss(Context c, long id) {
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel((int)id);
    }

    private static PendingIntent action(Context c, long id, String action, int index) {
        Intent intent = new Intent(c, ActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(AlarmScheduler.EXTRA_ID, id);
        return PendingIntent.getBroadcast(c, (int)(id * 10 + index), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L);
        if (id <= 0) return;
        try (Store db = new Store(context)) {
            Product p = db.get(id);
            if (p == null || p.purchased || p.dueAt <= 0) return;
            // Stale alarm from previous edit/snooze: don't show an early reminder.
            if (p.dueAt > System.currentTimeMillis() + 60_000L) return;
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
            createChannel(context);
            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openPi = PendingIntent.getActivity(context, (int)id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification n = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Almayı unutma: " + p.name)
                .setContentText("Almalı · " + p.category + " · Alışveriş zamanı")
                .setStyle(new Notification.BigTextStyle().bigText("\"" + p.name + "\" alışverişini yapma zamanı. Aldığında tamamlandı olarak işaretle veya hatırlatmayı ertele."))
                .setContentIntent(openPi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_notification), "Aldım", action(context, id, ACTION_BUY, 1)).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_notification), "1 saat ertele", action(context, id, ACTION_SNOOZE_HOUR, 2)).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_notification), "Yarın", action(context, id, ACTION_SNOOZE_DAY, 3)).build())
                .build();
            NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify((int)id, n);
        }
    }
}
