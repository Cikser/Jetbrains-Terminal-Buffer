package terminalbuffer;

public class Style {

    public static final int NONE = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 1 << 1;
    public static final int UNDERLINE = 1 << 2;

    public static class Color{
        public static final int BLACK = 0;
        public static final int RED = 1;
        public static final int GREEN = 2;
        public static final int YELLOW = 3;
        public static final int BLUE = 4;
        public static final int MAGENTA = 5;
        public static final int CYAN = 6;
        public static final int WHITE = 7;
        public static final int GRAY = 8;
        public static final int BRIGHT_RED = 9;
        public static final int BRIGHT_GREEN = 10;
        public static final int BRIGHT_YELLOW = 11;
        public static final int BRIGHT_BLUE = 12;
        public static final int BRIGHT_MAGENTA = 13;
        public static final int BRIGHT_CYAN = 14;
        public static final int BRIGHT_WHITE = 15;
    }

    public static int setAttributes(int fg, int bg, int style){
        int attributes = 0;
        attributes |= fg;
        attributes |= (bg << 4);
        attributes |= (style << 8);
        return attributes;
    }

    public static int setForeground(int attributes, int fg){
        return (attributes & ~0xF) | fg;
    }

    public static int setBackground(int attributes, int bg){
        return (attributes & ~(0xF << 4)) | bg;
    }

    public static int addAttribute(int attributes, int style){
        return attributes | style;
    }

    public static int clearAttributes(int attributes){
        return attributes & ~(0xFF);
    }

}
