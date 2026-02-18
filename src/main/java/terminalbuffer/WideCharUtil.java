package terminalbuffer;

/**
 * Utility class for detecting wide characters that occupy 2 terminal cells.
 *
 * <p>Wide characters include:
 * <ul>
 *   <li>CJK (Chinese, Japanese, Korean) ideographs</li>
 *   <li>Hiragana and Katakana</li>
 *   <li>Hangul syllables</li>
 *   <li>Common emoji ranges</li>
 * </ul>
 *
 * <p>This class uses an optimized detection strategy with an ASCII fast path
 * to avoid expensive Unicode block lookups for the common case (~95% of text
 * is ASCII in typical terminal usage).
 *
 * <p>Wide characters are represented in the terminal buffer by occupying two
 * cells: the first cell contains the character itself, and the second cell
 * contains a placeholder ({@code \u0000}).
 */
public class WideCharUtil {

    /**
     * Checks whether a character is wide (occupies 2 terminal cells).
     *
     * <p>This method uses an optimized fast path for ASCII characters (&lt; 128),
     * which immediately returns false without performing expensive Unicode
     * block lookups.
     *
     * <p>For non-ASCII characters, delegates to {@link #isWide(int)} for
     * full Unicode block analysis.
     *
     * @param c character to check
     * @return true if the character occupies 2 cells, false if it occupies 1 cell
     */
    public static boolean isWide(char c) {
        if (c < 128) return false;
        return isWide((int) c);
    }

    /**
     * Checks whether a Unicode code point is wide (occupies 2 terminal cells).
     *
     * <p>This method performs full Unicode block analysis to determine if the
     * code point belongs to one of the wide character ranges:
     * <ul>
     *   <li>CJK Unified Ideographs (U+4E00-U+9FFF)</li>
     *   <li>CJK Unified Ideographs Extension A</li>
     *   <li>CJK Unified Ideographs Extension B</li>
     *   <li>CJK Compatibility Ideographs</li>
     *   <li>Hiragana (U+3040-U+309F)</li>
     *   <li>Katakana (U+30A0-U+30FF)</li>
     *   <li>Hangul Syllables (U+AC00-U+D7AF)</li>
     *   <li>Hangul Jamo (U+1100-U+11FF)</li>
     *   <li>Common emoji ranges</li>
     * </ul>
     *
     * <p>This method uses an ASCII fast path for code points &lt; 128.
     *
     * @param codePoint Unicode code point to check
     * @return true if the code point occupies 2 cells, false if it occupies 1 cell
     */
    public static boolean isWide(int codePoint) {
        if (codePoint < 128) return false;

        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || isEmoji(codePoint);
    }

    /**
     * Checks whether a code point is in a common emoji range.
     *
     * <p>Covered ranges:
     * <ul>
     *   <li>U+1F300-U+1F9FF: Miscellaneous Symbols and Pictographs, Emoticons, etc.</li>
     *   <li>U+2600-U+26FF: Miscellaneous Symbols</li>
     *   <li>U+2700-U+27BF: Dingbats</li>
     * </ul>
     *
     * <p>Note: This does not cover all possible emoji code points, but handles
     * the most commonly used ranges in terminal applications.
     *
     * @param codePoint Unicode code point to check
     * @return true if the code point is in a covered emoji range, false otherwise
     */
    private static boolean isEmoji(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1F9FF)
                || (codePoint >= 0x2600 && codePoint <= 0x26FF)
                || (codePoint >= 0x2700 && codePoint <= 0x27BF);
    }
}