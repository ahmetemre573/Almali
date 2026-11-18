package com.almali.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Türkçe fiyat takip beta arayüzü. Trendyol web sayfalarından fiyat kazımaz. */
public final class PriceTrackingActivity extends Activity {
    private static final int BG = 0xFFF7F8FD;
    private static final int TEXT = 0xFF202741;
    private static final int MUTED = 0xFF777F95;
    private static final int PRIMARY = 0xFF5364E9;
    private static final int BORDER = 0xFFE9ECF6;
    private static final int GREEN = 0xFF1B8B69;
    private final Locale tr = new Locale("tr", "TR");
    private PriceStore db;
    private LinearLayout root;
    private long selectedId;

    private int dp(float n) { return (int)(getResources().getDisplayMetrics().density*n + 0.5f); }
    private LinearLayout vertical() { LinearLayout v = new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v; }
    private LinearLayout horizontal() { LinearLayout v = new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v; }
    private LinearLayout.LayoutParams size(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void pad(View v,int left,int top,int right,int bottom){v.setPadding(dp(left),dp(top),dp(right),dp(bottom));}
    private GradientDrawable bg(int color,int rad){GradientDrawable d = new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(rad));return d;}
    private GradientDrawable border(int color,int radius){GradientDrawable d = bg(color,radius);d.setStroke(dp(1),BORDER);return d;}
    private TextView text(String message,int sp,int color,boolean bold){
        TextView v = new TextView(this);v.setText(message);v.setTextSize(sp);v.setTextColor(color);v.setIncludeFontPadding(false);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private TextView button(String s,boolean solid){TextView b=text(s,13,solid?Color.WHITE:PRIMARY,true);b.setGravity(Gravity.CENTER);pad(b,14,11,14,11);b.setBackground(solid?bg(PRIMARY,12):border(Color.WHITE,12));return b;}
    private void gap(LinearLayout v,int n){View g = new View(this);v.addView(g,size(1,n));}
    private String money(double price){return NumberFormat.getCurrencyInstance(tr).format(price);}
    private String date(long n){return new SimpleDateFormat("d MMM yyyy HH:mm",tr).format(new Date(n));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private boolean https(String value){try {return "https".equalsIgnoreCase(Uri.parse(value).getScheme());}catch(Exception e){return false;}}
    private static double priceNumber(String input) {
        String t=input.trim().replace(" ","").replace("₺","").replace("TL","");
        if(t.contains(",") && t.contains(".")) {
            if(t.lastIndexOf(',') > t.lastIndexOf('.')) t=t.replace(".","").replace(",",".");
            else t=t.replace(",","");
        } else t=t.replace(",",".");
        double v=Double.parseDouble(t);
        if(!Double.isFinite(v)||v<=0||v>1_000_000_000d)throw new NumberFormatException();
        return v;
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        db = new PriceStore(this);
        db.seedFirstItem(this);
        root=vertical();root.setBackgroundColor(BG);setContentView(root);
        if(Build.VERSION.SDK_INT>=30) root.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(0,bars.top,0,bars.bottom);return insets;
        });
        selectedId=getIntent().getLongExtra("tracked_id",0);
        PriceFeedWorker.configure(this);
        render();
    }
    @Override protected void onResume(){super.onResume();if(root!=null && db!=null)render();}
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
    @Override public void onBackPressed(){if(selectedId>0){selectedId=0;render();}else super.onBackPressed();}

    private void render(){
        root.removeAllViews();
        LinearLayout top=horizontal();pad(top,18,20,18,16);
        TextView back=button("‹ Geri",false);top.addView(back);back.setOnClickListener(v->{if(selectedId>0){selectedId=0;render();}else finish();});
        LinearLayout caption=vertical();LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.leftMargin=dp(13);top.addView(caption,cp);
        caption.addView(text("Fiyat Takip",24,TEXT,true));
        gap(caption,4);caption.addView(text("Almalı 2.0 · Ücretsiz beta",11,MUTED,false));
        root.addView(top);
        ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);scroll.setFillViewport(true);
        LinearLayout content=vertical();pad(content,18,4,18,25);
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        if(selectedId>0){
            PriceStore.Tracked item=db.get(selectedId);
            if(item==null){selectedId=0;showAll(content);}else showDetail(content,item);
        }else showAll(content);
    }

    private void note(LinearLayout parent,String message){
        TextView box=text(message,12,0xFF45517D,false);
        box.setLineSpacing(dp(2),1f);pad(box,14,13,14,13);box.setBackground(bg(0xFFEFF1FF,14));parent.addView(box,size(-1,-2));
    }
    private void headline(LinearLayout parent,String message){TextView t=text(message,19,TEXT,true);LinearLayout.LayoutParams p=size(-1,-2);p.topMargin=dp(15);p.bottomMargin=dp(12);parent.addView(t,p);}

    private void showAll(LinearLayout list){
        note(list,"BETA · Bu ekrandaki fiyatlar yalnızca senin kaydettiğin veya DEMO JSON akışından alınan fiyatlardır. Canlı Trendyol/Amazon fiyatı çekilmiyor.");
        gap(list,13);
        TextView add=button("+ Fiyat takibine ürün ekle",true);list.addView(add,size(-1,46));add.setOnClickListener(v->edit(null));
        List<PriceStore.Tracked> all=db.all();
        headline(list,"Takip ettiklerim  ·  "+all.size());
        if(all.isEmpty())note(list,"Henüz ürün yok. Fiyat takibine ürün ekleyerek başlayabilirsin.");
        for(PriceStore.Tracked t:all) itemCard(list,t);
        gap(list,8);
        note(list,"Otomatik kontrol yalnızca senin eklediğin HTTPS DEMO JSON akışları için çalışır. Android'in pil kısıtlamaları nedeniyle saatlik kontroller gecikebilir.");
    }

    private void itemCard(LinearLayout list,PriceStore.Tracked item){
        LinearLayout card=vertical();pad(card,15,15,15,15);card.setBackground(border(Color.WHITE,18));
        LinearLayout row=horizontal();card.addView(row);
        imageOrPlaceholder(row,item,58);
        LinearLayout title=vertical();LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(12);row.addView(title,tp);
        title.addView(text(item.name,15,TEXT,true));gap(title,6);
        title.addView(text("Hedef fiyat: "+money(item.targetPrice),13,GREEN,true));
        PriceStore.Sample sample=db.latest(item.id);
        gap(card,13);
        card.addView(text(sample==null?"Henüz fiyat kaydı yok":"Son kayıt: "+money(sample.price)+" · "+sample.merchant,13,sample==null?MUTED:TEXT,sample!=null));
        if(sample!=null){gap(card,4);card.addView(text(date(sample.observedAt)+" · "+origin(sample.origin),11,MUTED,false));}
        gap(card,12);
        TextView open=button("Fiyat geçmişi ve detaylar  ›",false);card.addView(open,size(-1,42));
        open.setOnClickListener(v->{selectedId=item.id;render();});
        LinearLayout.LayoutParams p=size(-1,-2);p.bottomMargin=dp(12);list.addView(card,p);
    }

    private String origin(String s){return "DEMO_FEED".equals(s)?"TEST veri akışı":"Elle girildi";}

    private void imageOrPlaceholder(LinearLayout row,PriceStore.Tracked item,int wh){
        if(https(item.imageUrl)){
            ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(bg(0xFFF0F2FF,12));row.addView(image,size(wh,wh));
            // Opt-in direct HTTPS image only; image source entered by user, not fabricated product metadata.
            new Thread(()->{
                HttpURLConnection conn=null;
                try {
                    conn=(HttpURLConnection)new URL(item.imageUrl).openConnection();
                    conn.setConnectTimeout(5000);conn.setReadTimeout(5000);conn.setInstanceFollowRedirects(false);
                    if(conn.getResponseCode()!=200)return;
                    String type=conn.getContentType();if(type==null||!type.startsWith("image/"))return;
                    try(InputStream stream=conn.getInputStream();ByteArrayOutputStream data=new ByteArrayOutputStream()){
                        byte[] b=new byte[4096];int n;
                        while((n=stream.read(b))!=-1){data.write(b,0,n);if(data.size()>1_000_000)return;}
                        Bitmap bitmap=BitmapFactory.decodeByteArray(data.toByteArray(),0,data.size());
                        if(bitmap!=null)runOnUiThread(()->image.setImageBitmap(bitmap));
                    }
                }catch(Exception ignored){}finally{if(conn!=null)conn.disconnect();}
            }).start();
        }else{
            TextView pic=text("🧴",31,PRIMARY,false);pic.setGravity(Gravity.CENTER);pic.setBackground(bg(0xFFF0F2FF,12));row.addView(pic,size(wh,wh));
        }
    }

    private void showDetail(LinearLayout list,PriceStore.Tracked item){
        note(list,"Kaynak durumu: gerçek mağaza fiyatları otomatik çekilmiyor. Test akışından gelen fiyatlar DEMO etiketi taşır.");
        gap(list,15);
        LinearLayout top=horizontal();list.addView(top);
        imageOrPlaceholder(top,item,74);
        LinearLayout info=vertical();LinearLayout.LayoutParams il=new LinearLayout.LayoutParams(0,-2,1);il.leftMargin=dp(14);top.addView(info,il);
        info.addView(text(item.name,18,TEXT,true));gap(info,7);
        info.addView(text("Hedef: "+money(item.targetPrice),15,GREEN,true));
        if(!item.productCode.isEmpty()){gap(info,6);info.addView(text("Ürün kodu: "+item.productCode,12,MUTED,false));}
        gap(list,16);
        LinearLayout first=horizontal();list.addView(first);
        TextView save=button("+ Fiyat kaydet",true);first.addView(save,new LinearLayout.LayoutParams(0,dp(46),1));save.setOnClickListener(v->manualPrice(item));
        TextView edit=button("Düzenle",false);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(46),1);ep.leftMargin=dp(8);first.addView(edit,ep);edit.setOnClickListener(v->edit(item));
        if(https(item.productUrl)){
            gap(list,9);TextView page=button("Ürünün mağaza sayfasını aç ↗",false);list.addView(page,size(-1,42));
            page.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(item.productUrl))));
        }
        List<PriceStore.Sample> samples=db.samples(item.id,100);
        headline(list,"Kaydettiğin fiyat geçmişi");
        if(samples.isEmpty())note(list,"Henüz fiyat kaydı yok. İlk fiyatı elle ekle. Grafiğin oluşması için en az iki kayıt gerekli.");
        else {
            PriceGraph chart=new PriceGraph(samples);
            chart.setBackground(border(Color.WHITE,17));list.addView(chart,size(-1,215));
            gap(list,9);list.addView(text("Grafik, kaydedilen örnekleri gösterir; canlı piyasa verisi değildir.",11,MUTED,false));
            headline(list,"Son fiyat kayıtları");
            for(int i=0;i<Math.min(samples.size(),20);i++){
                PriceStore.Sample s=samples.get(i);
                LinearLayout row=horizontal();pad(row,13,12,13,12);row.setBackground(border(Color.WHITE,13));
                LinearLayout description=vertical();row.addView(description,new LinearLayout.LayoutParams(0,-2,1));
                description.addView(text(s.merchant,13,TEXT,true));gap(description,5);
                description.addView(text(date(s.observedAt)+" · "+origin(s.origin),11,MUTED,false));
                row.addView(text(money(s.price),13,GREEN,true));
                LinearLayout.LayoutParams rp=size(-1,-2);rp.bottomMargin=dp(7);list.addView(row,rp);
            }
        }
        headline(list,"Saatlik otomasyon testi");
        if(item.feedUrl.isEmpty()){
            note(list,"Henüz DEMO JSON akışı eklenmemiş. Düzenle → Demo veri akışı URL'si bölümünden kendi kontrolündeki HTTPS JSON dosyasını tanımlayabilirsin.");
        }else{
            note(list,"Bu bağlantı yalnızca TEST JSON verisi içindir. Her saat kontrol edilmeye çalışılır; Android kesin zaman garantisi vermez.");
            gap(list,9);
            list.addView(text("Son sorgu: "+(item.lastCheckAt>0?date(item.lastCheckAt):"Henüz sorgulanmadı"),12,MUTED,false));
            gap(list,5);list.addView(text(item.lastCheckResult.isEmpty()?"Durum bilgisi yok":item.lastCheckResult,12,MUTED,false));
            gap(list,10);
            TextView now=button("Test akışını şimdi sorgula",true);list.addView(now,size(-1,46));
            now.setOnClickListener(v->{
                now.setEnabled(false);now.setText("Sorgulanıyor…");
                new Thread(()->{
                    String result;
                    try{result=DemoPriceFeed.check(getApplicationContext(),item);}
                    catch(Exception e){result="HATA: "+(e.getMessage()==null?"Bağlantı hatası":e.getMessage());
                        try(PriceStore backgroundDb=new PriceStore(getApplicationContext())){backgroundDb.checkResult(item.id,false,e.getMessage()==null?"Bağlantı sorunu":e.getMessage());}
                    }
                    String msg=result;
                    runOnUiThread(()->{toast(msg);render();});
                }).start();
            });
        }
        gap(list,18);
        TextView delete=button("Ürünü takipten kaldır",false);list.addView(delete,size(-1,44));
        delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Takip silinsin mi?")
            .setMessage("Bu ürünün fiyat geçmişi de silinir. Eski alışveriş hatırlatmaların etkilenmez.")
            .setNegativeButton("Vazgeç",null)
            .setPositiveButton("Sil",(d,w)->{db.remove(item.id);PriceFeedWorker.configure(this);selectedId=0;render();}).show());
    }

    private EditText field(LinearLayout box,String label,String value,boolean decimal){
        TextView l=text(label,12,MUTED,true);LinearLayout.LayoutParams ll=size(-1,-2);ll.topMargin=dp(11);box.addView(l,ll);
        EditText f=new EditText(this);f.setText(value==null?"":value);f.setTextSize(14);f.setSingleLine(true);f.setTextColor(TEXT);
        f.setInputType(decimal?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        f.setSelectAllOnFocus(true);pad(f,11,8,11,8);f.setBackground(border(Color.WHITE,11));
        LinearLayout.LayoutParams fp=size(-1,48);fp.topMargin=dp(6);box.addView(f,fp);return f;
    }
    private void edit(PriceStore.Tracked current){
        PriceStore.Tracked item=current==null?new PriceStore.Tracked():current;
        ScrollView scroll=new ScrollView(this);LinearLayout fields=vertical();pad(fields,18,5,18,18);scroll.addView(fields);
        EditText name=field(fields,"Ürün adı",item.name,false);
        EditText code=field(fields,"Ürün kodu (isteğe bağlı; test akışında gerekli)",item.productCode,false);
        EditText url=field(fields,"Ürün bağlantısı (HTTPS)",item.productUrl,false);
        EditText target=field(fields,"Hedef fiyat (TL)",item.targetPrice>0?String.valueOf(item.targetPrice):"",true);
        EditText image=field(fields,"Ürün fotoğrafı HTTPS adresi (isteğe bağlı)",item.imageUrl,false);
        EditText feed=field(fields,"DEMO JSON HTTPS adresi (isteğe bağlı)",item.feedUrl,false);
        gap(fields,9);
        fields.addView(text("Dikkat: DEMO JSON bağlantısı GERÇEK mağaza fiyatı değildir. Örnek biçim için projedeki docs/demo-feed.json dosyasına bak.",12,MUTED,false));
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle(current==null?"Fiyat takibine ürün ekle":"Fiyat takibini düzenle")
            .setView(scroll).setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
            String n=name.getText().toString().trim(),c=code.getText().toString().trim(),u=url.getText().toString().trim();
            String im=image.getText().toString().trim(),f=feed.getText().toString().trim();
            if(n.isEmpty()||n.length()>150){name.setError("Ürün adı 1–150 karakter olmalı");return;}
            if(c.length()>100){code.setError("En fazla 100 karakter");return;}
            if(!u.isEmpty()&&!https(u)){url.setError("HTTPS adresi olmalı");return;}
            if(!im.isEmpty()&&!https(im)){image.setError("HTTPS adresi olmalı");return;}
            if(!f.isEmpty()&&!https(f)){feed.setError("HTTPS adresi olmalı");return;}
            if(!f.isEmpty()&&c.isEmpty()){code.setError("Test akışı için ürün kodu gerekli");return;}
            double amount;try{amount=priceNumber(target.getText().toString());}catch(Exception e){target.setError("Örnek: 300 veya 299,90");return;}
            if(Math.abs(item.targetPrice-amount)>0.009)item.alertedPrice=0;
            item.name=n;item.productCode=c;item.productUrl=u;item.imageUrl=im;item.feedUrl=f;
            item.targetPrice=amount;
            db.save(item);
            PriceFeedWorker.configure(this);
            dialog.dismiss();render();toast("Fiyat takibi kaydedildi");
        }));
        dialog.show();
    }

    private void manualPrice(PriceStore.Tracked item){
        LinearLayout body=vertical();pad(body,17,6,17,15);
        EditText price=field(body,"Gördüğün fiyat (TL)","",true);
        EditText merchant=field(body,"Mağaza / satıcı adı","Trendyol",false);
        gap(body,10);
        body.addView(text("Bu fiyat elle girilen gözlemdir; piyasadaki en ucuz veya otomatik güncel fiyat olarak gösterilmez.",12,MUTED,false));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Fiyat kaydet")
            .setView(body).setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
            double amount;try{amount=priceNumber(price.getText().toString());}catch(Exception e){price.setError("Geçerli fiyat: 299,90");return;}
            String m=merchant.getText().toString().trim();if(m.isEmpty()||m.length()>100){merchant.setError("Mağaza adı gerekli (en fazla 100)");return;}
            PriceStore.RecordResult r=db.record(item.id,amount,m,"MANUAL",null);
            if(r.alert)PriceAlertNotifier.notifyPrice(this,item,amount,"MANUAL");
            dialog.dismiss();render();toast("Elle görülen fiyat kaydedildi ✓");
        }));
        dialog.show();
    }

    /** Basit ve veri sayısı sınırlı grafik; canlı fiyat izlenimi yaratmaz. */
    private final class PriceGraph extends View {
        private final List<PriceStore.Sample> samples;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        PriceGraph(List<PriceStore.Sample> raw){super(PriceTrackingActivity.this);samples=new ArrayList<>(raw);Collections.reverse(samples);}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            int w=getWidth(),h=getHeight();float left=dp(49),right=w-dp(14),top=dp(22),bottom=h-dp(35);
            if(samples.size()==0)return;
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            for(PriceStore.Sample s:samples){min=Math.min(min,s.price);max=Math.max(max,s.price);}
            if(max-min<1){max+=Math.max(1,max*.08);min=Math.max(0,min-Math.max(1,min*.08));}
            float yrange=(float)(max-min);
            paint.setColor(0xFFE6E9F4);paint.setStrokeWidth(dp(1));paint.setStyle(Paint.Style.STROKE);
            for(int i=0;i<=3;i++){
                float y=top+(bottom-top)*i/3f;canvas.drawLine(left,y,right,y,paint);
            }
            paint.setStyle(Paint.Style.FILL);paint.setColor(MUTED);paint.setTextSize(dp(10));
            for(int i=0;i<=3;i++){
                float y=top+(bottom-top)*i/3f;float value=(float)(max-(max-min)*i/3);
                canvas.drawText(String.format(Locale.US,"%.0f",value),dp(8),y+dp(4),paint);
            }
            Path path=new Path();
            for(int i=0;i<samples.size();i++){
                float x=samples.size()==1?(left+right)/2:left+(right-left)*i/(samples.size()-1f);
                float y=bottom-(float)((samples.get(i).price-min)/yrange)*(bottom-top);
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            paint.setColor(PRIMARY);paint.setStrokeWidth(dp(2.5f));paint.setStyle(Paint.Style.STROKE);canvas.drawPath(path,paint);
            paint.setStyle(Paint.Style.FILL);
            for(int i=0;i<samples.size();i++){
                float x=samples.size()==1?(left+right)/2:left+(right-left)*i/(samples.size()-1f);
                float y=bottom-(float)((samples.get(i).price-min)/yrange)*(bottom-top);
                canvas.drawCircle(x,y,dp(3.6f),paint);
            }
            paint.setColor(MUTED);paint.setTextSize(dp(10));
            canvas.drawText(new SimpleDateFormat("d MMM",tr).format(new Date(samples.get(0).observedAt)),left,bottom+dp(20),paint);
            if(samples.size()>1){String end=new SimpleDateFormat("d MMM",tr).format(new Date(samples.get(samples.size()-1).observedAt));canvas.drawText(end,right-paint.measureText(end),bottom+dp(20),paint);}
        }
    }
}
