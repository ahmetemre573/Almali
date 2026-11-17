package com.almali.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(247, 248, 253);
    private static final int INK = Color.rgb(32, 39, 65);
    private static final int MUTED = Color.rgb(119, 127, 149);
    private static final int PRIMARY = Color.rgb(83, 100, 233);
    private static final int BORDER = Color.rgb(233, 236, 246);
    private static final int GREEN = Color.rgb(27, 139, 105);
    private final Locale tr = new Locale("tr", "TR");
    private Store store;
    private LinearLayout root;
    private int tab = 0;

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable s = new GradientDrawable();
        s.setColor(color);
        s.setCornerRadius(dp(radius));
        return s;
    }

    private GradientDrawable outline(int color, int stroke, int radius) {
        GradientDrawable s = shape(color, radius);
        s.setStroke(dp(1), stroke);
        return s;
    }

    private TextView label(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setIncludeFontPadding(false);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private LinearLayout horizontal() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private void pad(View view, int left, int top, int right, int bottom) {
        view.setPadding(dp(left), dp(top), dp(right), dp(bottom));
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private TextView action(String s, boolean primary) {
        TextView b = label(s, 13, primary ? Color.WHITE : PRIMARY, true);
        b.setGravity(Gravity.CENTER);
        pad(b, 13, 10, 13, 10);
        b.setBackground(primary ? shape(PRIMARY, 11) : outline(Color.WHITE, BORDER, 11));
        return b;
    }

    private String money(double amount) {
        return NumberFormat.getCurrencyInstance(tr).format(amount);
    }

    private String date(long when) {
        return new SimpleDateFormat("d MMM yyyy, HH:mm", tr).format(new Date(when));
    }

    private long startOfTomorrow() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    private String dueText(long when) {
        long now = System.currentTimeMillis();
        if (when < now) return "Gecikti · " + date(when);
        if (when < startOfTomorrow()) return "Bugün · " + new SimpleDateFormat("HH:mm", tr).format(new Date(when));
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfTomorrow());
        c.add(Calendar.DAY_OF_YEAR, 1);
        if (when < c.getTimeInMillis()) return "Yarın · " + new SimpleDateFormat("HH:mm", tr).format(new Date(when));
        return date(when);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Store(this);
        ReminderReceiver.createChannel(this);
        root = vertical();
        root.setBackgroundColor(BG);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        render();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (store != null && root != null) render();
    }

    @Override protected void onDestroy() {
        if (store != null) store.close();
        super.onDestroy();
    }

    private void render() {
        root.removeAllViews();
        List<Product> all = store.all();
        int today = 0, late = 0, future = 0;
        double sum = 0;
        long now = System.currentTimeMillis(), tomorrow = startOfTomorrow();
        for (Product p : all) {
            if (p.purchased || p.dueAt <= 0) continue;
            if (p.dueAt < now) late++;
            if (p.dueAt < tomorrow) today++;
            else future++;
            sum += p.estimatedPrice;
        }

        LinearLayout top = horizontal();
        pad(top, 22, 19, 22, 16);
        LinearLayout brand = vertical();
        TextView title = label("Almalı", 29, INK, true);
        brand.addView(title);
        TextView subtitle = label("Alışverişin kontrol altında.", 12, MUTED, false);
        LinearLayout.LayoutParams st = lp(-2, -2); st.topMargin = dp(5);
        brand.addView(subtitle, st);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView settings = label("⚙", 27, PRIMARY, false);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(outline(Color.WHITE, BORDER, 15));
        top.addView(settings, lp(46, 46));
        settings.setOnClickListener(v -> settingsDialog());
        root.addView(top);

        LinearLayout hero = vertical();
        GradientDrawable banner = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF6574F3, 0xFF4B52CE});
        banner.setCornerRadius(dp(23));
        hero.setBackground(banner);
        pad(hero, 20, 18, 20, 17);
        TextView h = label("Planın hazır ✨", 18, Color.WHITE, true);
        hero.addView(h);
        TextView d = label("İhtiyacın olanları zamanında al.", 12, 0xFFDDE3FF, false);
        LinearLayout.LayoutParams dlp = lp(-2, -2); dlp.topMargin = dp(6);
        hero.addView(d, dlp);
        LinearLayout metrics = horizontal();
        LinearLayout.LayoutParams mlp = lp(-1, -2); mlp.topMargin = dp(19);
        hero.addView(metrics, mlp);
        metric(metrics, String.valueOf(today), "Bugün");
        metric(metrics, String.valueOf(late), "Geciken");
        metric(metrics, money(sum), "Planlanan bütçe");
        LinearLayout.LayoutParams hp = lp(-1, -2);
        hp.setMargins(dp(18), dp(2), dp(18), dp(18));
        root.addView(hero, hp);

        LinearLayout tabs = horizontal();
        pad(tabs, 15, 0, 15, 10);
        String[] titles = {"Bugün", "Yaklaşan", "İstekler", "Geçmiş"};
        for (int i=0; i<titles.length; i++) {
            final int selected=i;
            TextView item = label(titles[i], 12, tab==i ? Color.WHITE : MUTED, tab==i);
            item.setGravity(Gravity.CENTER);
            item.setBackground(tab==i ? shape(PRIMARY, 13) : shape(0xFFECEFFA, 13));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(40), 1);
            tp.setMargins(dp(3), 0, dp(3), 0);
            tabs.addView(item,tp);
            item.setOnClickListener(v -> { tab=selected; render(); });
        }
        root.addView(tabs);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout list = vertical();
        pad(list, 18, 4, 18, 18);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        if (tab == 3) {
            showHistory(list);
        } else {
            ArrayList<Product> chosen = new ArrayList<>();
            for (Product p : all) {
                if (p.purchased) continue;
                if (tab == 2 && p.isWish()) chosen.add(p);
                if (tab == 0 && p.dueAt > 0 && p.dueAt < tomorrow) chosen.add(p);
                if (tab == 1 && p.dueAt >= tomorrow) chosen.add(p);
            }
            Collections.sort(chosen, (a,b)-> {
                if (tab == 0) return Long.compare(a.dueAt, b.dueAt);
                if (tab == 1) return Long.compare(a.dueAt, b.dueAt);
                return Integer.compare(b.priority,a.priority);
            });
            String heading = tab==0 ? "Bugünkü alışverişlerin" : (tab==1 ? "Sıradaki alışverişler" : "Bir gün alacaklarım");
            sectionHeading(list, heading, chosen.size() + " ürün");
            if (chosen.isEmpty()) empty(list, tab==0 ? "Bugün için alışveriş yok 🎉" : (tab==1 ? "Yaklaşan alışverişin yok" : "İstek listene ilk ürünü ekle"));
            for (Product p : chosen) productCard(list, p);
        }

        LinearLayout bottom = horizontal();
        pad(bottom, 18, 11, 18, 14);
        bottom.setBackgroundColor(Color.WHITE);
        TextView b = action("+ Yeni ürün ekle", true);
        b.setTextSize(15);
        bottom.addView(b, lp(-1, 50));
        b.setOnClickListener(v -> edit(null));
        root.addView(bottom);
    }

    private void metric(LinearLayout row, String value, String title) {
        LinearLayout col = vertical();
        TextView number = label(value, value.length()>10?14:21, Color.WHITE, true);
        col.addView(number);
        TextView caption = label(title, 10, 0xFFDFE4FF, false);
        LinearLayout.LayoutParams clp = lp(-2,-2); clp.topMargin = dp(5);
        col.addView(caption, clp);
        row.addView(col, new LinearLayout.LayoutParams(0, -2, title.equals("Planlanan bütçe") ? 1.6f:1f));
    }

    private void sectionHeading(LinearLayout parent, String title, String count) {
        LinearLayout row = horizontal();
        TextView t = label(title, 19, INK, true);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        TextView c = label(count, 12, MUTED, false);
        row.addView(c);
        LinearLayout.LayoutParams rp = lp(-1, -2); rp.setMargins(dp(2), dp(8), dp(2), dp(15));
        parent.addView(row, rp);
    }

    private void empty(LinearLayout parent, String message) {
        LinearLayout card = vertical();
        card.setGravity(Gravity.CENTER);
        card.setBackground(outline(Color.WHITE,BORDER,19));
        pad(card, 18, 40, 18, 40);
        TextView emoji = label("🛍️", 35, INK, false); emoji.setGravity(Gravity.CENTER);
        card.addView(emoji);
        TextView text = label(message, 15, INK, true); text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams t = lp(-1,-2); t.topMargin=dp(15);
        card.addView(text,t);
        TextView hint = label("+ Yeni ürün ekle ile başlayabilirsin", 12, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = lp(-1,-2); hp.topMargin=dp(7);
        card.addView(hint,hp);
        parent.addView(card, lp(-1,-2));
    }

    private void productCard(LinearLayout parent, Product p) {
        LinearLayout card = vertical();
        card.setBackground(outline(Color.WHITE,BORDER,19));
        pad(card, 16, 16, 16, 15);
        LinearLayout header = horizontal();
        TextView icon = label(tab==2?"♡":"✓", 20, PRIMARY, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(0xFFF0F2FF, 13));
        header.addView(icon, lp(43,43));
        LinearLayout info = vertical();
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0,-2,1);
        ip.leftMargin=dp(12);
        header.addView(info,ip);
        info.addView(label(p.name, 16, INK, true));
        String priority = p.priority == 2 ? " • Önemli" : "";
        TextView cat = label(p.category + priority, 12, p.priority==2?0xFFDD7043:MUTED, false);
        LinearLayout.LayoutParams cp=lp(-1,-2);cp.topMargin=dp(6);
        info.addView(cat,cp);
        if (p.estimatedPrice > 0) header.addView(label(money(p.estimatedPrice), 13, INK, true));
        card.addView(header);
        TextView due = label(p.isWish()?"Hedef tarih belirlenmedi":("◷  " + dueText(p.dueAt)), 12, p.dueAt>0 && p.dueAt<System.currentTimeMillis()?0xFFD2554B:MUTED, false);
        LinearLayout.LayoutParams dueLp=lp(-1,-2);dueLp.topMargin=dp(14);
        card.addView(due,dueLp);
        if (!p.isWish() && !"NONE".equals(p.repeat)) {
            TextView rep=label("↻  " + repeatLabel(p.repeat, p.customDays), 11, GREEN, false);
            LinearLayout.LayoutParams rp=lp(-1,-2);rp.topMargin=dp(7);
            card.addView(rep,rp);
        }
        if (p.note != null && !p.note.trim().isEmpty()) {
            TextView note=label(p.note, 12, MUTED, false);
            LinearLayout.LayoutParams nlp=lp(-1,-2);nlp.topMargin=dp(7);
            card.addView(note,nlp);
        }
        LinearLayout actions=horizontal();
        LinearLayout.LayoutParams alp=lp(-1,-2); alp.topMargin=dp(14);
        card.addView(actions,alp);
        if (!p.isWish()) {
            TextView buy=action("Aldım", true);
            actions.addView(buy);
            buy.setOnClickListener(v -> purchaseDialog(p));
            TextView postpone=action("Ertele", false);
            LinearLayout.LayoutParams plp=lp(-2,-2);plp.leftMargin=dp(8);
            actions.addView(postpone,plp);
            postpone.setOnClickListener(v -> postponeDialog(p));
        }
        TextView edit=action(p.isWish()?"Planla / Düzenle":"Düzenle", false);
        LinearLayout.LayoutParams elp=lp(-2,-2);
        if (actions.getChildCount()>0) elp.leftMargin=dp(8);
        actions.addView(edit,elp);
        edit.setOnClickListener(v -> edit(p));
        LinearLayout.LayoutParams cardLp=lp(-1,-2);cardLp.bottomMargin=dp(11);
        parent.addView(card,cardLp);
    }

    private String repeatLabel(String repeat, int custom) {
        switch(repeat) {
            case "DAILY": return "Her gün";
            case "WEEKLY": return "Her hafta";
            case "MONTHLY": return "Her ay";
            case "YEARLY": return "Her yıl";
            case "CUSTOM": return custom + " günde bir";
            default: return "Tek seferlik";
        }
    }

    private void showHistory(LinearLayout list) {
        List<Store.History> h = store.history();
        double sum = 0;
        for (Store.History item:h) sum+=item.amount;
        sectionHeading(list,"Satın alma geçmişi",h.size()+" işlem");
        TextView total = label("Toplam kayıtlı harcama: " + money(sum), 13, GREEN, true);
        LinearLayout.LayoutParams tlp=lp(-1,-2);tlp.bottomMargin=dp(15);
        list.addView(total,tlp);
        if(h.isEmpty()) { empty(list,"Henüz tamamlanan alışveriş yok"); return; }
        for (Store.History item : h) {
            LinearLayout card=horizontal();card.setBackground(outline(Color.WHITE,BORDER,15));pad(card,15,13,15,13);
            LinearLayout txt=vertical();
            txt.addView(label(item.name,15,INK,true));
            TextView time=label(date(item.purchasedAt)+" · "+item.category,11,MUTED,false);
            LinearLayout.LayoutParams tp=lp(-1,-2);tp.topMargin=dp(6);txt.addView(time,tp);
            card.addView(txt,new LinearLayout.LayoutParams(0,-2,1));
            card.addView(label(money(item.amount),13,GREEN,true));
            LinearLayout.LayoutParams clp=lp(-1,-2);clp.bottomMargin=dp(10);
            list.addView(card,clp);
        }
    }

    private void purchaseDialog(Product p) {
        EditText price = field("Gerçek satın alma fiyatı (₺)", String.valueOf(p.estimatedPrice), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout content=vertical();pad(content,22,16,22,3);
        content.addView(label("Ürünü satın aldığında ödenen tutarı kaydedebilirsin.",13,MUTED,false));
        LinearLayout.LayoutParams ep=lp(-1,50);ep.topMargin=dp(14);content.addView(price,ep);
        new AlertDialog.Builder(this)
            .setTitle("Aldım: " + p.name)
            .setView(content)
            .setNegativeButton("Vazgeç",null)
            .setPositiveButton("Satın alındı",(d,w)->{
                try {
                    double paid=parsePrice(price.getText().toString());
                    if (store.purchase(p.id,paid)) {
                        AlarmScheduler.schedule(this,store.get(p.id));
                        ReminderReceiver.dismiss(this,p.id);
                        toast("Satın alma kaydedildi ✓");
                        render();
                    }
                } catch(Exception ex) { toast("Fiyat okunamadı"); }
            }).show();
    }

    private void postponeDialog(Product p) {
        String[] options={"1 saat sonra", "Yarın", "3 gün sonra", "Tarih seç..."};
        new AlertDialog.Builder(this).setTitle("Hatırlatmayı ertele")
            .setItems(options,(d,which)->{
                if(which==3) { edit(p);return; }
                Calendar c=Calendar.getInstance();
                c.setTimeInMillis(Math.max(System.currentTimeMillis(),p.dueAt));
                if(which==0) c.add(Calendar.HOUR_OF_DAY,1);
                if(which==1) c.add(Calendar.DAY_OF_YEAR,1);
                if(which==2) c.add(Calendar.DAY_OF_YEAR,3);
                p.dueAt=c.getTimeInMillis();
                store.save(p);
                AlarmScheduler.schedule(this,p);
                ReminderReceiver.dismiss(this,p.id);
                toast("Yeni hatırlatma: "+date(p.dueAt));
                render();
            }).show();
    }

    private EditText field(String hint, String value, int inputType) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setHint(hint);
        e.setInputType(inputType);
        if(value!=null)e.setText(value);
        e.setBackground(outline(0xFFFAFBFF,BORDER,10));
        pad(e,12,9,12,9);
        return e;
    }

    private Spinner spinner(String[] options, int selected) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        s.setAdapter(adapter);
        s.setSelection(selected);
        return s;
    }

    private void fieldLabel(LinearLayout parent, String name, View view, int height) {
        TextView label=label(name,12,INK,true);
        LinearLayout.LayoutParams la=lp(-1,-2);la.topMargin=dp(13);
        parent.addView(label,la);
        LinearLayout.LayoutParams va=lp(-1,height);va.topMargin=dp(6);
        parent.addView(view,va);
    }

    private static double parsePrice(String raw) {
        String v=raw.trim().replace(",",".");
        double n=v.isEmpty()?0:Double.parseDouble(v);
        if (!Double.isFinite(n) || n < 0) throw new NumberFormatException();
        return n;
    }

    private void edit(Product existing) {
        Product p=existing==null?new Product():existing;
        final long[] selectedDate={p.dueAt>0?p.dueAt:defaultDate()};
        ScrollView sc=new ScrollView(this);
        LinearLayout fields=vertical();pad(fields,20,6,20,12);
        sc.addView(fields);
        EditText name=field("Örn. Diş macunu",p.name,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        fieldLabel(fields,"Ürün adı *",name,50);
        String[] categories={"Genel","Market","Kişisel bakım","Giyim","Teknoloji","Ev","Spor","Diğer"};
        int categoryIndex=0;
        for(int i=0;i<categories.length;i++)if(categories[i].equals(p.category))categoryIndex=i;
        Spinner cat=spinner(categories,categoryIndex); fieldLabel(fields,"Kategori",cat,48);
        EditText price=field("0.00",p.estimatedPrice>0?String.valueOf(p.estimatedPrice):"",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        fieldLabel(fields,"Tahmini fiyat (₺)",price,50);
        Spinner priority=spinner(new String[]{"Düşük","Normal","Yüksek"},p.priority);
        fieldLabel(fields,"Öncelik",priority,48);
        CheckBox wish=new CheckBox(this);
        wish.setText("Uzun vadeli istek listesine ekle (tarihsiz)");
        wish.setTextSize(13);wish.setTextColor(INK);wish.setButtonTintList(android.content.res.ColorStateList.valueOf(PRIMARY));
        wish.setChecked(p.isWish() || (existing==null && tab==2));
        LinearLayout.LayoutParams wishLp=lp(-1,-2);wishLp.topMargin=dp(15);
        fields.addView(wish,wishLp);
        TextView dateBtn=action("Hatırlatma: " + date(selectedDate[0]),false);
        fieldLabel(fields,"Hatırlatma zamanı",dateBtn,54);
        dateBtn.setGravity(Gravity.CENTER_VERTICAL);pad(dateBtn,13,8,13,8);
        dateBtn.setEnabled(!wish.isChecked());dateBtn.setAlpha(wish.isChecked()?0.45f:1f);
        String[] repeatOptions={"Tek seferlik","Her gün","Her hafta","Her ay","Her yıl","X günde bir"};
        String[] repeatCodes={"NONE","DAILY","WEEKLY","MONTHLY","YEARLY","CUSTOM"};
        int repeatIndex=0;
        for(int i=0;i<repeatCodes.length;i++) if(repeatCodes[i].equals(p.repeat))repeatIndex=i;
        Spinner repeat=spinner(repeatOptions,repeatIndex);
        fieldLabel(fields,"Tekrar sıklığı",repeat,48);
        EditText days=field("Kaç günde bir?",String.valueOf(p.customDays),InputType.TYPE_CLASS_NUMBER);
        fieldLabel(fields,"Özel tekrar aralığı (yalnızca X günde bir)",days,50);
        dateBtn.setOnClickListener(v -> {
            Calendar initial=Calendar.getInstance();initial.setTimeInMillis(selectedDate[0]);
            new DatePickerDialog(this,(dp,year,month,day)->{
                Calendar next=Calendar.getInstance();next.setTimeInMillis(selectedDate[0]);
                next.set(Calendar.YEAR,year);next.set(Calendar.MONTH,month);next.set(Calendar.DAY_OF_MONTH,day);
                new TimePickerDialog(this,(tp,hour,minute)->{
                    next.set(Calendar.HOUR_OF_DAY,hour);next.set(Calendar.MINUTE,minute);
                    next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
                    selectedDate[0]=next.getTimeInMillis();dateBtn.setText("Hatırlatma: " + date(selectedDate[0]));
                },initial.get(Calendar.HOUR_OF_DAY),initial.get(Calendar.MINUTE),true).show();
            },initial.get(Calendar.YEAR),initial.get(Calendar.MONTH),initial.get(Calendar.DAY_OF_MONTH)).show();
        });
        wish.setOnCheckedChangeListener((button, checked)->{
            dateBtn.setEnabled(!checked);dateBtn.setAlpha(checked?0.45f:1f);
            repeat.setEnabled(!checked);days.setEnabled(!checked);
        });
        repeat.setEnabled(!wish.isChecked());days.setEnabled(!wish.isChecked());
        EditText note=field("Ek bilgi (isteğe bağlı)",p.note,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        fieldLabel(fields,"Not",note,50);
        AlertDialog.Builder builder=new AlertDialog.Builder(this)
            .setTitle(existing==null?"Yeni ürün ekle":"Ürünü düzenle")
            .setView(sc)
            .setNegativeButton("Vazgeç",null)
            .setPositiveButton("Kaydet",null);
        if(existing!=null)builder.setNeutralButton("Sil",(d,w)->{
            new AlertDialog.Builder(this).setTitle("Ürünü sil?")
                .setMessage("Ürün listeden kaldırılır. Önceki satın alma kayıtları saklanır.")
                .setNegativeButton("Vazgeç",null)
                .setPositiveButton("Sil",(dd,ww)->{
                    AlarmScheduler.cancel(this,p.id);
                    ReminderReceiver.dismiss(this,p.id);
                    store.delete(p.id);render();
                }).show();
        });
        AlertDialog dialog=builder.create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String text=name.getText().toString().trim();
            if(text.isEmpty()) {name.setError("Ürün adı gerekli");return;}
            if(text.length()>150) {name.setError("En fazla 150 karakter");return;}
            double estimated;
            try {estimated=parsePrice(price.getText().toString());}
            catch(NumberFormatException e){price.setError("Geçerli bir fiyat gir");return;}
            boolean isWish=wish.isChecked();
            if(!isWish && selectedDate[0]<=System.currentTimeMillis()) {
                toast("Hatırlatma zamanını gelecek bir tarih seç");return;
            }
            int custom;
            try {custom=Integer.parseInt(days.getText().toString().trim());}
            catch(Exception ex) {days.setError("Gün sayısı gir");return;}
            if(custom<1 || custom>3650){days.setError("1 ile 3650 gün arasında olmalı");return;}
            if (!isWish && (p.id == 0 || p.dueAt != selectedDate[0])) {
                Calendar anchor = Calendar.getInstance();
                anchor.setTimeInMillis(selectedDate[0]);
                p.anchorDay = anchor.get(Calendar.DAY_OF_MONTH);
                p.anchorMonth = anchor.get(Calendar.MONTH);
            }
            p.name=text;p.category=categories[cat.getSelectedItemPosition()];p.estimatedPrice=estimated;
            p.priority=priority.getSelectedItemPosition();p.dueAt=isWish?-1:selectedDate[0];
            p.repeat=isWish?"NONE":repeatCodes[repeat.getSelectedItemPosition()];
            p.customDays=custom;p.note=note.getText().toString().trim();p.purchased=false;
            store.save(p);AlarmScheduler.schedule(this,p);ReminderReceiver.dismiss(this,p.id);
            dialog.dismiss();render();toast("Ürün kaydedildi ✓");
        }));
        dialog.show();
    }

    private long defaultDate() {
        Calendar c=Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR,1);
        c.set(Calendar.HOUR_OF_DAY,18);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }

    private void settingsDialog() {
        LinearLayout body=vertical();pad(body,22,10,22,15);
        TextView details=label("Bildirim durumu: " + notificationStatus() + "\n\nTam zamanlı alarm: " + (AlarmScheduler.canBeExact(this)?"Açık ✓":"İzin gerekli / yaklaşık zamanlama"),14,INK,false);
        body.addView(details);
        TextView explanation=label("Bildirimler cihazda saklanır. İnternet gerektirmez. Kesin alarm izni kapalıysa Android bildirimi geciktirebilir.",12,MUTED,false);
        LinearLayout.LayoutParams ep=lp(-1,-2);ep.topMargin=dp(15);body.addView(explanation,ep);
        TextView perms=action("Bildirim / alarm izinleri",true);
        LinearLayout.LayoutParams pp=lp(-1,-2);pp.topMargin=dp(19);body.addView(perms,pp);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("Ayarlar ve bildirimler").setView(body)
            .setPositiveButton("Kapat",null).create();
        perms.setOnClickListener(v->{dlg.dismiss();permissionDialog();});
        dlg.show();
    }

    private String notificationStatus() {
        return Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED ? "Açık ✓":"İzin gerekli";
    }

    private void permissionDialog() {
        new AlertDialog.Builder(this).setTitle("Hatırlatma izinleri")
            .setMessage("Android 13+ bildirim izni ister. Android 12+ modellerde dakikası dakikasına alarm için özel erişim gerekebilir. İzin yoksa yaklaşık zamanlı bildirim kurulacak.")
            .setNegativeButton("Kapat",null)
            .setNeutralButton("Bildirim izni",(d,w)->{
                if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},42);
                else if(Build.VERSION.SDK_INT>=26) {
                    Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    i.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);
                }
            })
            .setPositiveButton("Tam zamanlı alarm",(d,w)->{
                if(Build.VERSION.SDK_INT>=31 && !AlarmScheduler.canBeExact(this)) {
                    try {
                        Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        i.setData(Uri.parse("package:"+getPackageName()));startActivity(i);
                    } catch(Exception e) {toast("Alarm izin ayarları açılamadı");}
                } else toast("Tam zamanlı alarm zaten kullanılabilir");
            }).show();
    }

    private void toast(String message) {Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
}
