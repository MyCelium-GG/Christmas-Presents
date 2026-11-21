package my.celium.org.christmaspresents;

import my.celium.org.stats.ModStats;

/**
 * Anonymous stats for the FastStats project {@code christmaspresents}, sent through Mycel under
 * its consent: how many presents get wrapped and opened.
 */
public final class ChristmasPresentsStats {
    /** Project token from faststats.dev (public, ships in the jar). Empty until the project exists. */
    private static final String TOKEN = "";

    private static volatile ModStats stats;

    private ChristmasPresentsStats() {
    }

    static void start() {
        if (TOKEN.isEmpty()) {
            return;
        }
        stats = ModStats.builder(ChristmasPresentsMod.META, TOKEN)
                .counters("presents")
                .errors("my.celium.org.christmaspresents.")
                .start();
    }

    static void wrapped() {
        count("wrapped");
    }

    static void opened() {
        count("opened");
    }

    private static void count(String what) {
        ModStats current = stats;
        if (current != null) {
            current.count("presents", what);
        }
    }
}
