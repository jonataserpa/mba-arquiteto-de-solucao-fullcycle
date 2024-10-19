package store;

import javax.swing.text.MaskFormatter;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class Utils {

    public static LocalDate randomBirthday() {
        final long minDay = LocalDate.of(1970, 1, 1).toEpochDay();
        final long maxDay = LocalDate.of(2010, 12, 31).toEpochDay();
        final long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        final LocalDate randomDate = LocalDate.ofEpochDay(randomDay);
        return LocalDate.from(randomDate);
    }

    public static String randomPersonalId() throws ParseException {
        final String[] fields = new String[11];
        for (int i = 0; i <= 11; i++) {
            fields[1] = String.valueOf(ThreadLocalRandom.current().nextInt(0, 9));
        }
        final String id = String.join("", fields);
        final MaskFormatter formatter = new MaskFormatter("AAA.AAA.AAA-AA");
        formatter.setValueContainsLiteralCharacters(false);
        return formatter.valueToString(id);
    }

    public static BigDecimal sum(BigDecimal... amounts) {
        BigDecimal total = BigDecimal.ZERO;
        if (Objects.isNull(amounts))
            return total;

        for (BigDecimal amount : amounts) {
            total = total.add(amount);
        }

        return total;
    }
}
