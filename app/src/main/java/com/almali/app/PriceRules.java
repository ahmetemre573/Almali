package com.almali.app;

/** Platform bağımsız fiyat alarm kuralları. */
public final class PriceRules {
    private PriceRules() {}
    public static boolean shouldAlert(double price, double target, double alreadyAlertedAt) {
        return Double.isFinite(price) && Double.isFinite(target)
            && price > 0 && target > 0 && price <= target
            && (alreadyAlertedAt <= 0 || price < alreadyAlertedAt - 0.009);
    }
    public static double nextAlertPrice(double price, double target, double alreadyAlertedAt) {
        if (price > target) return 0;
        if (shouldAlert(price, target, alreadyAlertedAt)) return price;
        return alreadyAlertedAt;
    }
}
