package terminalbuffer;

import java.util.EnumSet;

public class Style {

    public enum StyleFlag {
        NONE(0),
        BOLD(1),
        ITALIC(1 << 1),
        UNDERLINE(1 << 2);

        public final int value;

        StyleFlag(int value) {
            this.value = value;
        }
    }

    public enum Color {
        BLACK(0), RED(1), GREEN(2), YELLOW(3),
        BLUE(4), MAGENTA(5), CYAN(6), WHITE(7),
        GRAY(8), BRIGHT_RED(9), BRIGHT_GREEN(10),
        BRIGHT_YELLOW(11), BRIGHT_BLUE(12),
        BRIGHT_MAGENTA(13), BRIGHT_CYAN(14), BRIGHT_WHITE(15);

        public final int value;

        Color(int value) {
            this.value = value;
        }
    }

    public static int setAttributes(Color fg, Color bg, EnumSet<StyleFlag> style){
        int attributes = 0;
        attributes |= fg.value;
        attributes |= (bg.value << 4);
        for(StyleFlag flag : style){
            attributes |= flag.value << 8;
        }
        return attributes;
    }

    public static int setAttributes(Color fg, Color bg, StyleFlag style){
        int attributes = 0;
        attributes |= fg.value;
        attributes |= (bg.value << 4);
        attributes |= (style.value << 8);
        return attributes;
    }

    public static int setForeground(int attributes, Color fg){
        return (attributes & ~0xF) | fg.value;
    }

    public static int setBackground(int attributes, Color bg){
        return (attributes & ~(0xF << 4)) | bg.value;
    }

    public static int addAttribute(int attributes, EnumSet<StyleFlag> style){
        for(StyleFlag flag : style){
            attributes |= flag.value << 8 ;
        }
        return attributes;
    }

    public static int clearStyle(int attributes){
        return attributes & (0xFF);
    }

}
