package my.celium.org.christmaspresents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PresentLogicTest {
    @Test
    void bowUntiesInTheFirstNineTicks() {
        assertEquals(1f, Unwrap.bowScale(0), 1e-6);
        assertEquals(0f, Unwrap.bowScale(9), 1e-6);
    }

    @Test
    void sidesEndUpFlat() {
        assertEquals(0f, Unwrap.wallFold(0), 1e-6);
        assertEquals((float) (Math.PI / 2), Unwrap.wallFold(Unwrap.TICKS), 1e-4);
    }

    @Test
    void lidLandsOnTheGroundTheRightWayUp() {
        for (PresentStyle.Shape shape : PresentStyle.Shape.values()) {
            float lift = Unwrap.lidLift(40, shape.height, shape.lid);
            assertEquals(-(shape.height + 0.5f - shape.lid), lift, 1e-4, shape.name());
        }
        assertEquals((float) (Math.PI * 2), Unwrap.lidFlip(40), 1e-4);
    }

    @Test
    void giftsAppearAfterTheSidesStartFolding() {
        assertEquals(0f, Unwrap.giftRise(Unwrap.GIFTS_RISE), 1e-6);
        assertTrue(Unwrap.giftRise(Unwrap.GIFTS_RISE + 10) > 0);
        assertEquals(Unwrap.APPROACH + Unwrap.GIFTS_RISE, Unwrap.GIFTS);
    }

    @Test
    void christmasIsTheTwentyFourthToTheTwentySixth() {
        assertFalse(Calendar.isChristmas(LocalDate.of(2026, 12, 23)));
        assertTrue(Calendar.isChristmas(LocalDate.of(2026, 12, 24)));
        assertTrue(Calendar.isChristmas(LocalDate.of(2026, 12, 26)));
        assertFalse(Calendar.isChristmas(LocalDate.of(2026, 12, 27)));
        assertEquals(33, Calendar.daysUntilChristmas(LocalDate.of(2026, 11, 21)));
        assertEquals(362, Calendar.daysUntilChristmas(LocalDate.of(2026, 12, 27)));
    }

    @Test
    void namesKeepOnlyNameCharacters() {
        assertEquals("Steve_2", Names.clean("  Ste ve_2! "));
        assertEquals("abcdefghijklmnop", Names.clean("abcdefghijklmnopqrst"));
        assertEquals("", Names.clean(null));
    }

    @Test
    void stylesCycleAndParse() {
        assertEquals(PresentStyle.Bow.BIG_BOW, PresentStyle.cycle(PresentStyle.Bow.BOW, 1));
        assertEquals(PresentStyle.Bow.KNOT, PresentStyle.cycle(PresentStyle.Bow.BOW, -1));
        assertEquals(PresentStyle.Shape.TALL, PresentStyle.Shape.of("tall"));
        assertEquals(PresentStyle.Pattern.PLAIN, PresentStyle.Pattern.of("nonsense"));
        assertEquals(0x123456, new PresentStyle(0xFF123456, 0, null, null, null).paper());
    }
}
