package my.celium.org.christmaspresents;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;

/** The optional "only open on Christmas" rule, by the server's real date. */
public final class Calendar {
    private Calendar() {
    }

    /** @return a translation key to show instead of opening, or null when the present may open */
    public static String waitMessage() {
        if (!ChristmasPresentsMod.onlyOnChristmas()) {
            return null;
        }
        return isChristmas(LocalDate.now()) ? null : "christmaspresents.wait";
    }

    /** December 24 to 26 counts as Christmas, so every tradition gets its morning. */
    public static boolean isChristmas(LocalDate date) {
        MonthDay day = MonthDay.from(date);
        return !day.isBefore(MonthDay.of(12, 24)) && !day.isAfter(MonthDay.of(12, 26));
    }

    public static long daysUntilChristmas() {
        return daysUntilChristmas(LocalDate.now());
    }

    static long daysUntilChristmas(LocalDate today) {
        LocalDate eve = LocalDate.of(today.getYear(), 12, 24);
        if (today.isAfter(LocalDate.of(today.getYear(), 12, 26))) {
            eve = eve.plusYears(1);
        }
        return Math.max(0, ChronoUnit.DAYS.between(today, eve));
    }
}
