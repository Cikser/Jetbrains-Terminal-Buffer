package terminalbuffer;

import java.util.EnumSet;

/**
 * Provides text attribute encoding and color definitions for terminal text.
 *
 * <p>This class encodes foreground color, background color, and style flags
 * (bold, italic, underline) into a single 32-bit integer for efficient storage
 * and manipulation.
 *
 * <p>Bit layout of the encoded attribute value:
 * <ul>
 *   <li>Bits 0-3: Foreground color (0-15)</li>
 *   <li>Bits 4-7: Background color (0-15)</li>
 *   <li>Bits 8-10: Style flags (bold, italic, underline)</li>
 *   <li>Bit 23: EMPTY flag (internal use)</li>
 * </ul>
 *
 * @see TerminalBuffer#setAttributes
 */
public class Style {

    /**
     * Style flags for text decoration.
     *
     * <p>Multiple flags can be combined using {@link EnumSet}.
     */
    public enum StyleFlag {
        /** No style flags */
        NONE(0),
        /** Bold/bright text */
        BOLD(1),
        /** Italic text */
        ITALIC(1 << 1),
        /** Underlined text */
        UNDERLINE(1 << 2),
        /** Internal flag marking empty cells (not for external use) */
        EMPTY(1 << 23);

        public final int value;

        StyleFlag(int value) {
            this.value = value;
        }
    }

    /**
     * Standard 16-color palette for terminal text.
     *
     * <p>Includes 8 standard colors (black through white), 8 bright variants,
     * and default color aliases.
     */
    public enum Color {
        /** Black (color index 0) */
        BLACK(0),
        /** Red (color index 1) */
        RED(1),
        /** Green (color index 2) */
        GREEN(2),
        /** Yellow (color index 3) */
        YELLOW(3),
        /** Blue (color index 4) */
        BLUE(4),
        /** Magenta (color index 5) */
        MAGENTA(5),
        /** Cyan (color index 6) */
        CYAN(6),
        /** White (color index 7) */
        WHITE(7),
        /** Gray / bright black (color index 8) */
        GRAY(8),
        /** Bright red (color index 9) */
        BRIGHT_RED(9),
        /** Bright green (color index 10) */
        BRIGHT_GREEN(10),
        /** Bright yellow (color index 11) */
        BRIGHT_YELLOW(11),
        /** Bright blue (color index 12) */
        BRIGHT_BLUE(12),
        /** Bright magenta (color index 13) */
        BRIGHT_MAGENTA(13),
        /** Bright cyan (color index 14) */
        BRIGHT_CYAN(14),
        /** Bright white (color index 15) */
        BRIGHT_WHITE(15),
        /** Default background color (alias for BLACK) */
        BG_DEFAULT(0),
        /** Default foreground color (alias for WHITE) */
        FG_DEFAULT(7);

        public final int value;

        Color(int value) {
            this.value = value;
        }
    }

    /**
     * Encodes foreground color, background color, and multiple style flags
     * into a single integer attribute value.
     *
     * <p>This method is used when multiple style flags need to be combined
     * (e.g., both bold and underline).
     *
     * <p>Example:
     * <pre>{@code
     * int attrs = Style.setAttributes(
     *     Style.Color.RED,
     *     Style.Color.BLACK,
     *     EnumSet.of(StyleFlag.BOLD, StyleFlag.UNDERLINE)
     * );
     * }</pre>
     *
     * @param fg foreground color
     * @param bg background color
     * @param style set of style flags to apply
     * @return bit-packed attribute value
     */
    public static int setAttributes(Color fg, Color bg, EnumSet<StyleFlag> style){
        int attributes = 0;
        attributes |= fg.value;
        attributes |= (bg.value << 4);
        for(StyleFlag flag : style){
            attributes |= flag.value << 8;
        }
        return attributes;
    }

    /**
     * Encodes foreground color, background color, and a single style flag
     * into a single integer attribute value.
     *
     * <p>This is a convenience method for the common case of a single style flag.
     *
     * <p>Example:
     * <pre>{@code
     * int attrs = Style.setAttributes(
     *     Style.Color.GREEN,
     *     Style.Color.BLACK,
     *     StyleFlag.BOLD
     * );
     * }</pre>
     *
     * @param fg foreground color
     * @param bg background color
     * @param style single style flag to apply (use NONE for no styling)
     * @return bit-packed attribute value
     */
    public static int setAttributes(Color fg, Color bg, StyleFlag style){
        int attributes = 0;
        attributes |= fg.value;
        attributes |= (bg.value << 4);
        attributes |= (style.value << 8);
        return attributes;
    }

    /**
     * Pre-computed bit mask for the EMPTY flag.
     *
     * <p>This constant is used internally by {@link Line} to efficiently
     * check whether a cell has been written to.
     */
    static final int EMPTY_BIT = StyleFlag.EMPTY.value << 8;

}