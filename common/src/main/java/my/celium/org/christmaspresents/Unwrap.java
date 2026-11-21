package my.celium.org.christmaspresents;

/**
 * The opening animation's timeline. Pure maths, shared by the renderer (client) and the block
 * entity (server: when to play sounds and hand out the gifts).
 *
 * <p>Opening starts with {@link #APPROACH} ticks in which the player steps up to the present and
 * reaches for it. The phase methods below take the time since the unwrapping itself began (the
 * opening time minus {@link #APPROACH}); negative means the present is still closed.
 *
 * <ol>
 *   <li>0-9: the bow comes undone: it spins, wobbles and shrinks away, the lid ribbons with it.</li>
 *   <li>5-12: the ribbons on the sides slide down.</li>
 *   <li>8-24: the lid pops up, flips and tumbles off behind the present onto the ground.</li>
 *   <li>14-26: the four sides fold open flat, with a little bounce.</li>
 *   <li>18-36: the gifts rise out of the open box, spinning.</li>
 *   <li>32-40: the paper shrinks away; at 40 the gifts become real items.</li>
 * </ol>
 */
public final class Unwrap {
    /** Ticks the player takes to step up to the present before the bow comes undone. */
    public static final int APPROACH = 8;
    /** Length of the whole opening; the gifts are handed out at this tick. */
    public static final int TICKS = APPROACH + 40;
    /** The lid pops off: confetti and a sound (opening time). */
    public static final int LID_POP = APPROACH + 9;
    /** The gifts start to rise: a chime (opening time). */
    public static final int GIFTS = APPROACH + 18;
    /** When the gifts start to rise, in unwrapping time. */
    public static final int GIFTS_RISE = 18;

    private Unwrap() {
    }

    /** Bow (and lid ribbon) scale: 1 closed, 0 untied. */
    public static float bowScale(float t) {
        return 1 - easeIn(clamp01(t / 9f));
    }

    /** Bow spin while it unties, radians. */
    public static float bowSpin(float t) {
        return clamp01(t / 9f) * 3.2f;
    }

    /** Side ribbon length: 1 full, 0 gone. */
    public static float sideRibbon(float t) {
        return 1 - easeInOut(clamp01((t - 5) / 7f));
    }

    /**
     * How far the lid has risen, in pixels (up is positive). It pops up, then falls until it
     * rests on the ground: from {@code boxHeight + 0.5} down to its own height.
     */
    public static float lidLift(float t, float boxHeight, float lidHeight) {
        float up = 9 * easeOut(clamp01((t - 8) / 6f));
        float fall = easeIn(clamp01((t - 14) / 9f)) * (9 + boxHeight + 0.5f - lidHeight);
        return up - fall + bounce(t, 23, 1.2f);
    }

    /**
     * How far the lid has flown sideways and backwards, in pixels: it lands in the free corner
     * behind the present, where no side folds down.
     */
    public static float lidAway(float t, float boxSize) {
        return easeInOut(clamp01((t - 11) / 12f)) * (boxSize + 1.5f);
    }

    /** The lid's somersault, radians: one full turn, so it lands the right way up. */
    public static float lidFlip(float t) {
        return easeInOut(clamp01((t - 10) / 13f)) * (float) (Math.PI * 2);
    }

    /** The lid's turn while it flies, radians. */
    public static float lidSpin(float t) {
        return easeOut(clamp01((t - 9) / 15f)) * 0.5f;
    }

    /** How far the sides have folded open, radians: 0 upright, pi/2 flat. */
    public static float wallFold(float t) {
        float p = clamp01((t - 14) / 10f);
        float angle = easeOut(p) * (float) (Math.PI / 2);
        return angle - bounce(t, 24, 0.22f);
    }

    /** How high the gifts float above the floor of the box, in pixels. */
    public static float giftRise(float t) {
        return easeOut(clamp01((t - 18) / 14f)) * 13;
    }

    /** Gift size: they grow as they rise. */
    public static float giftScale(float t) {
        return 0.25f + 0.25f * easeOut(clamp01((t - 18) / 10f));
    }

    /** Gift spin, radians. */
    public static float giftSpin(float t) {
        return Math.max(0, t - 18) * 0.18f;
    }

    /** Paper and lid scale at the very end: 1 normal, 0 gone. */
    public static float paperFade(float t) {
        return 1 - easeIn(clamp01((t - 32) / 8f));
    }

    public static float clamp01(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    public static float easeOut(float p) {
        float q = 1 - p;
        return 1 - q * q * q;
    }

    public static float easeIn(float p) {
        return p * p * p;
    }

    public static float easeInOut(float p) {
        return p < 0.5f ? 4 * p * p * p : 1 - (float) Math.pow(-2 * p + 2, 3) / 2;
    }

    /** A small hop starting at {@code start}: up and back down over four ticks. */
    static float bounce(float t, float start, float height) {
        float p = (t - start) / 4f;
        if (p <= 0 || p >= 1) {
            return 0;
        }
        return (float) Math.sin(p * Math.PI) * height;
    }
}
