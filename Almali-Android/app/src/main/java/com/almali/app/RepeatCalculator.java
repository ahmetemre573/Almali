package com.almali.app;

import java.util.Calendar;

/** Saf Java; Android bağımlılığı yok, test edilebilir. */
public final class RepeatCalculator {
    private RepeatCalculator() {}

    public static long next(long plannedDue, long purchasedAt, String repeat, int customDays) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(plannedDue);
        return next(plannedDue, purchasedAt, repeat, customDays,
            date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.MONTH));
    }

    public static long next(long plannedDue, long purchasedAt, String repeat,
                            int customDays, int anchorDay, int anchorMonth) {
        if (plannedDue <= 0 || "NONE".equals(repeat)) return -1L;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(plannedDue);
        for (int i = 0; i < 2000; i++) {
            switch (repeat) {
                case "DAILY": c.add(Calendar.DAY_OF_YEAR, 1); break;
                case "WEEKLY": c.add(Calendar.WEEK_OF_YEAR, 1); break;
                case "MONTHLY":
                    c.set(Calendar.DAY_OF_MONTH, 1);
                    c.add(Calendar.MONTH, 1);
                    c.set(Calendar.DAY_OF_MONTH, Math.min(Math.max(1, anchorDay), c.getActualMaximum(Calendar.DAY_OF_MONTH)));
                    break;
                case "YEARLY":
                    c.set(Calendar.DAY_OF_MONTH, 1);
                    c.add(Calendar.YEAR, 1);
                    c.set(Calendar.MONTH, Math.max(0, Math.min(11, anchorMonth)));
                    c.set(Calendar.DAY_OF_MONTH, Math.min(Math.max(1, anchorDay), c.getActualMaximum(Calendar.DAY_OF_MONTH)));
                    break;
                case "CUSTOM": c.add(Calendar.DAY_OF_YEAR, Math.max(1, customDays)); break;
                default: return -1L;
            }
            if (c.getTimeInMillis() > purchasedAt) return c.getTimeInMillis();
        }
        c.setTimeInMillis(purchasedAt);
        c.add(Calendar.DAY_OF_YEAR, Math.max(1, "CUSTOM".equals(repeat) ? customDays : 1));
        return c.getTimeInMillis();
    }
}
