package terminalbuffer;

public class WideCharUtil {

    public static boolean isWide(int codePoint) {
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

    private static boolean isEmoji(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1F9FF) // Misc symbols, emoticons
                || (codePoint >= 0x2600 && codePoint <= 0x26FF)   // Misc symbols
                || (codePoint >= 0x2700 && codePoint <= 0x27BF);  // Dingbats
    }

    public static boolean isWide(char c) {
        return isWide((int) c);
    }
}