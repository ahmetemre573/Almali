import com.almali.app.RepeatCalculator;
import java.util.Calendar;

public class RepeatCalculatorTest {
    static void verify(boolean ok, String label) {
        if (!ok) throw new AssertionError("FAIL: " + label);
        System.out.println("OK: " + label);
    }
    public static void main(String[] args) {
        Calendar c=Calendar.getInstance();
        c.clear(); c.set(2026,Calendar.NOVEMBER,18,18,0);
        long start=c.getTimeInMillis();
        verify(RepeatCalculator.next(start,start,"NONE",7)==-1,"tek seferlik tekrar yok");
        Calendar target=(Calendar)c.clone();target.add(Calendar.DAY_OF_YEAR,1);
        verify(RepeatCalculator.next(start,start,"DAILY",7)==target.getTimeInMillis(),"günlük tekrar");
        target=(Calendar)c.clone();target.add(Calendar.WEEK_OF_YEAR,1);
        verify(RepeatCalculator.next(start,start,"WEEKLY",7)==target.getTimeInMillis(),"haftalık tekrar");
        target=(Calendar)c.clone();target.add(Calendar.MONTH,1);
        verify(RepeatCalculator.next(start,start,"MONTHLY",7)==target.getTimeInMillis(),"aylık tekrar");
        target=(Calendar)c.clone();target.add(Calendar.DAY_OF_YEAR,14);
        verify(RepeatCalculator.next(start,start,"CUSTOM",14)==target.getTimeInMillis(),"özel tekrar");
        long old=start-10*24L*60L*60L*1000L;
        verify(RepeatCalculator.next(old,start,"DAILY",1)>start,"geç kalan tekrar gelecek tarihe ilerler");
        Calendar jan31=Calendar.getInstance();jan31.clear();jan31.set(2027,Calendar.JANUARY,31,10,0);
        long feb28=RepeatCalculator.next(jan31.getTimeInMillis(), jan31.getTimeInMillis(), "MONTHLY",7,31,Calendar.JANUARY);
        Calendar march31=Calendar.getInstance();march31.clear();march31.set(2027,Calendar.MARCH,31,10,0);
        long marchResult=RepeatCalculator.next(feb28,feb28,"MONTHLY",7,31,Calendar.JANUARY);
        verify(marchResult==march31.getTimeInMillis(),"31 Ocak → 28 Şubat → 31 Mart kaymasız tekrar");
        Calendar leap=Calendar.getInstance();leap.clear();leap.set(2024,Calendar.FEBRUARY,29,8,30);
        long next2025=RepeatCalculator.next(leap.getTimeInMillis(),leap.getTimeInMillis(),"YEARLY",7,29,Calendar.FEBRUARY);
        long next2028=RepeatCalculator.next(next2025,next2025,"YEARLY",7,29,Calendar.FEBRUARY);
        // Next 2025->2026, 2026->2027, 2027->2028
        next2028=RepeatCalculator.next(next2028,next2028,"YEARLY",7,29,Calendar.FEBRUARY);
        next2028=RepeatCalculator.next(next2028,next2028,"YEARLY",7,29,Calendar.FEBRUARY);
        Calendar expected=Calendar.getInstance();expected.clear();expected.set(2028,Calendar.FEBRUARY,29,8,30);
        verify(next2028==expected.getTimeInMillis(),"29 Şubat yıllık tekrar dört yılda bir 29 Şubat'a döner");
        System.out.println("Bütün Java zaman hesaplama testleri geçti.");
    }
}
