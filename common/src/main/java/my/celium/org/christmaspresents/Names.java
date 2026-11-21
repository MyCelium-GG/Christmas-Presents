package my.celium.org.christmaspresents;

/** Player names typed into the "To" field. */
public final class Names {
    /** Minecraft names are at most 16 characters. */
    public static final int MAX = 16;

    private Names() {
    }

    /** Keeps only characters a Minecraft name can have, at most {@link #MAX} of them. */
    public static String clean(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (char c : name.trim().toCharArray()) {
            if (out.length() >= MAX) {
                break;
            }
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                out.append(c);
            }
        }
        return out.toString();
    }
}
