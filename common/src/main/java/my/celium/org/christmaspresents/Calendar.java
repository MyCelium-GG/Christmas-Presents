package my.celium.org.christmaspresents;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;

/**
 * Dates, always by the server's real date: the optional server-wide "only open on Christmas" rule,
 * and the "do not open until" day a player can put on each present.
 */
public final class Calendar {
    private Calendar() {
    }

    /** The days a present can be locked until, picked in the wrapping menu. */
    public enum Lock {
        ANY_TIME(null),
        CHRISTMAS_EVE(MonthDay.of(12, 24)),
        CHRISTMAS_DAY(MonthDay.of(12, 25)),
        NEW_YEAR(MonthDay.of(1, 1));

        public final MonthDay day;

        Lock(MonthDay day) {
            this.day = day;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Lock of(String id) {
            for (Lock lock : values()) {
                if (lock.id().equals(id)) {
                    return lock;
                }
            }
            return ANY_TIME;
        }
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

    /**
     * The day a present wrapped today with this lock may be opened, as an epoch day, or -1 for no
     * lock. That is the next time the day comes round, today included, so a present wrapped on
     * Christmas morning "until Christmas Day" opens right away.
     */
    static long opensOn(Lock lock, LocalDate today) {
        if (lock.day == null) {
            return -1;
        }
        LocalDate date = lock.day.atYear(today.getYear());
        if (date.isBefore(today)) {
            date = lock.day.atYear(today.getYear() + 1);
        }
        return date.toEpochDay();
    }

    /** Whole days left until {@code opensOn}; 0 or less means it may open. */
    static long daysLeft(long opensOn, LocalDate today) {
        return opensOn < 0 ? 0 : opensOn - today.toEpochDay();
    }
}
