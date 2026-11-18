package com.almali.app;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Saatte bir DENENİR; Android pil ve ağ kısıtları çalışmayı geciktirebilir. */
public final class PriceFeedWorker extends Worker {
    private static final String NAME = "almali_demo_price_feed_hourly";
    public PriceFeedWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    public static void configure(Context context) {
        boolean hasFeed = false;
        try (PriceStore db = new PriceStore(context)) {
            for (PriceStore.Tracked tracked : db.all()) {
                if (tracked.feedUrl != null && !tracked.feedUrl.trim().isEmpty()) { hasFeed = true; break; }
            }
        }
        WorkManager work = WorkManager.getInstance(context);
        if (!hasFeed) { work.cancelUniqueWork(NAME); return; }
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(PriceFeedWorker.class, 1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build();
        work.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @NonNull @Override public Result doWork() {
        List<PriceStore.Tracked> products;
        try (PriceStore db = new PriceStore(getApplicationContext())) { products = db.all(); }
        for (PriceStore.Tracked t : products) {
            if (isStopped()) break;
            if (t.feedUrl == null || t.feedUrl.trim().isEmpty()) continue;
            try {
                DemoPriceFeed.check(getApplicationContext(), t);
            } catch (Exception e) {
                try (PriceStore db = new PriceStore(getApplicationContext())) {
                    db.checkResult(t.id, false, e.getMessage() == null ? "Bağlantı sorunu" : e.getMessage());
                }
            }
        }
        return Result.success();
    }
}
