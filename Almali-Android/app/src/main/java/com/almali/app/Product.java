package com.almali.app;

public class Product {
    public long id;
    public String name = "";
    public String category = "Genel";
    public double estimatedPrice;
    public long dueAt = -1L;
    public String repeat = "NONE";
    public int customDays = 7;
    public int anchorDay = 0; // monthly / yearly original calendar day
    public int anchorMonth = 0; // yearly original month (0-11)
    public int priority = 1; // 0=düşük, 1=normal, 2=yüksek
    public String note = "";
    public boolean purchased = false;

    public boolean isWish() { return dueAt <= 0L && !purchased; }
}
