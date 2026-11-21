package my.celium.org.christmaspresents;

import java.util.List;
import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * How a present looks: paper and ribbon colour, the paper's pattern, the box shape and the bow on
 * top. Stored on the block entity and, for the item, in vanilla's custom model data component
 * (strings: shape, pattern, bow; colours: paper, ribbon), which also drives the item model.
 *
 * @param paper  the paper colour, 0xRRGGBB
 * @param ribbon the ribbon colour, 0xRRGGBB
 * @param pattern the print on the paper
 * @param shape  the box shape
 * @param bow    what sits on top
 */
public record PresentStyle(int paper, int ribbon, Pattern pattern, Shape shape, Bow bow) {
    /** Red paper, gold ribbon, a classic bow on a cube. */
    public static final PresentStyle DEFAULT = new PresentStyle(0xC62828, 0xF2C94C, Pattern.PLAIN, Shape.CUBE, Bow.BOW);

    /** The colours offered in the wrapping menu: Christmas colours first, then the rest. */
    public static final List<Integer> COLORS = List.of(
            0xC62828, 0x8E1B1B, 0x2E7D32, 0x1B5E20, 0xF2C94C, 0xD4A017, 0xFAFAFA, 0xC0C7CF,
            0x1E88E5, 0x0D2B6B, 0x8E24AA, 0xEC6FA8, 0xF57C00, 0x6D4C41, 0x37474F, 0x151515);

    public PresentStyle {
        paper &= 0xFFFFFF;
        ribbon &= 0xFFFFFF;
        pattern = pattern == null ? Pattern.PLAIN : pattern;
        shape = shape == null ? Shape.CUBE : shape;
        bow = bow == null ? Bow.BOW : bow;
    }

    public PresentStyle withPaper(int color) {
        return new PresentStyle(color, ribbon, pattern, shape, bow);
    }

    public PresentStyle withRibbon(int color) {
        return new PresentStyle(paper, color, pattern, shape, bow);
    }

    public PresentStyle withPattern(Pattern value) {
        return new PresentStyle(paper, ribbon, value, shape, bow);
    }

    public PresentStyle withShape(Shape value) {
        return new PresentStyle(paper, ribbon, pattern, value, bow);
    }

    public PresentStyle withBow(Bow value) {
        return new PresentStyle(paper, ribbon, pattern, shape, value);
    }

    /** The paper's print. */
    public enum Pattern {
        PLAIN, STRIPES, DOTS, STARS;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Pattern of(String id) {
            return parse(Pattern.class, id, PLAIN);
        }
    }

    /** What sits on the lid. */
    public enum Bow {
        BOW, BIG_BOW, ROSETTE, KNOT;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Bow of(String id) {
            return parse(Bow.class, id, BOW);
        }
    }

    /**
     * The box, in pixels: width (x), height and depth (z), and how tall the lid is. Also a block
     * state property, so the hitbox matches the box.
     */
    public enum Shape implements StringRepresentable {
        CUBE(10, 10, 10, 3),
        TALL(8, 14, 8, 3),
        WIDE(14, 7, 11, 3),
        SMALL(7, 7, 7, 2);

        public final int width;
        public final int height;
        public final int depth;
        public final int lid;

        Shape(int width, int height, int depth, int lid) {
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.lid = lid;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String getSerializedName() {
            return id();
        }

        public static Shape of(String id) {
            return parse(Shape.class, id, CUBE);
        }
    }

    /** The next or previous value of an enum, wrapping around. */
    public static <E extends Enum<E>> E cycle(E value, int step) {
        E[] all = value.getDeclaringClass().getEnumConstants();
        return all[Math.floorMod(value.ordinal() + step, all.length)];
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String id, E fallback) {
        if (id == null) {
            return fallback;
        }
        for (E value : type.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(id)) {
                return value;
            }
        }
        return fallback;
    }
}
