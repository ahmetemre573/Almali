package com.almali.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import java.text.NumberFormat;
import java.util.Locale;

public final class PriceAlertNotifier {
    private static final String CHANNEL = "price_alerts_beta";
    private PriceAlertNotifier() { }

    public static void notifyPrice(Context context, PriceStore.Tracked item, double price, String origin) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Fiyat takibi", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Kaydedilen fiyatlar hedef fiyatın altına inince bildirim");
        manager.createNotificationChannel(channel);
        Intent open = new Intent(context, PriceTrackingActivity.class);
        open.putExtra("tracked_id", item.id);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int request = (int)(100000 + item.id % 100000);
        PendingIntent pi = PendingIntent.getActivity(context, request, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String amount = NumberFormat.getCurrencyInstance(new Locale("tr", "TR")).format(price);
        boolean demo = "DEMO_FEED".equals(origin);
        boolean external = "EXTERNAL_FEED".equals(origin);
        String title = demo ? "TEST fiyat alarmı: " + item.name
            : external ? "Haricî fiyat alarmı: " + item.name
            : "Kaydettiğin fiyat hedefte: " + item.name;
        String text = amount + (demo ? " · TEST JSON, gerçek mağaza fiyatı değil"
            : external ? " · Kullanıcı kaynaklı veri; mağazada doğrula"
            : " · Elle kaydedilen fiyat");
        Notification notification = new Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .build();
        manager.notify(request, notification);
    }
}
