import com.almali.app.PriceRules;
public final class PriceRulesTest {
    static void test(boolean ok, String message){if(!ok) throw new AssertionError(message);System.out.println("OK: "+message);}
    public static void main(String[] args){
        test(!PriceRules.shouldAlert(379,300,0),"Hedef üstü alarm yok");
        test(PriceRules.shouldAlert(299,300,0),"Hedef altına ilk düşüş alarm");
        test(!PriceRules.shouldAlert(299,300,299),"Aynı fiyat tekrar alarm değil");
        test(PriceRules.shouldAlert(279,300,299),"Daha düşük fiyat alarm");
        test(PriceRules.nextAlertPrice(350,300,299)==0,"Hedef üstüne çıkınca reset");
        test(PriceRules.shouldAlert(295,300,0),"Reset sonrası yeniden alarm");
        test(!PriceRules.shouldAlert(Double.NaN,300,0),"NaN fiyat reddedilir");
        test(!PriceRules.shouldAlert(0,300,0),"Sıfır fiyat reddedilir");
        test(!PriceRules.shouldAlert(10,0,0),"Hedef fiyat yoksa alarm yok");
    }
}
