package tr.edu.iyte.esgfx.cases;

import java.util.Locale;

public final class ProductIDUtil {
    private ProductIDUtil() {}

   
    public static String format(long id) {
        return format(id, 6);
    }

   
    public static String format(long id, int minDigits) {
        if (id < 0) throw new IllegalArgumentException("id must be >= 0");
        return "P" + String.format(Locale.ROOT, "%0" + minDigits + "d", id);
    }
}

