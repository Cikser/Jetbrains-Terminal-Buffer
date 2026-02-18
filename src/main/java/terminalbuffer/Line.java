package terminalbuffer;

import java.util.Arrays;



class LineContent{
    char[] characters;
    int[] attributes;
}

public class Line {
    private final char[] characters;
    private final int[] attributes;

    private boolean isWrapped;

    boolean isWrapped() {
        return isWrapped;
    }

    void setWrapped() {
        isWrapped = true;
    }

    Line(int width, int currentAttributes){
        characters = new char[width];
        attributes = new int[width];

        for(int i = 0; i < width; i++){
            characters[i] = ' ';
            attributes[i] = currentAttributes | Style.StyleFlag.EMPTY.value;
        }

        isWrapped = false;
    }

    char getChar(int i){
        return characters[i];
    }

    int getAttributes(int i){
        return attributes[i];
    }

    void set(int i, char character, int attributes){
        this.characters[i] = character;
        this.attributes[i] = attributes & ~Style.StyleFlag.EMPTY.value;
    }

    @Override
    public String toString() {
        return new String(characters);
    }

    void fill(char character, int attr){
        Arrays.fill(characters, character);
        if(character != ' '){
            attr |= Style.StyleFlag.EMPTY.value;
        }
        Arrays.fill(attributes, attr);
    }

    LineContent insertAndOverflow(int index, String text, int[] attr, int textStartIndex, int textEndIndex){
        int capacity = characters.length;
        char[] textChars = text.toCharArray();
        int textLen = textEndIndex - textStartIndex;
        int textOverflowSize = (index + textLen) > capacity ? textLen - (capacity - index) : 0;
        int textOverflowStart = textStartIndex + textLen - textOverflowSize;

        int lineStartIndex = index;
        int lineEndIndex = empty() ? index : capacity;
        int lineOverflowSize = lineEndIndex - lineStartIndex;

        int elementsToCopy = lineOverflowSize - textLen;
        if(elementsToCopy > 0){
            lineStartIndex += elementsToCopy;
            lineOverflowSize -= elementsToCopy;
        }

        int overflowSize = textOverflowSize + lineOverflowSize;

        LineContent lc = createLineContent(overflowSize, textChars, attr, lineStartIndex, lineOverflowSize, textOverflowStart, textOverflowSize);
        shiftExistingContentAndInsertText(elementsToCopy, textChars, attr, index, textStartIndex, textLen, textOverflowStart);

        return lc;
    }

    private LineContent createLineContent(int overflowSize, char[] text, int[] attr,  int lineStartIndex, int lineOverflowSize, int textOverflowStart, int textOverflowSize){
        if(overflowSize <= 0) return null;
        LineContent lc = new LineContent();
        lc.characters = new char[overflowSize];
        lc.attributes = new int[overflowSize];

        System.arraycopy(text, textOverflowStart, lc.characters, 0, textOverflowSize);
        System.arraycopy(attr, textOverflowStart, lc.attributes, 0, textOverflowSize);
        System.arraycopy(characters, lineStartIndex, lc.characters, textOverflowSize, lineOverflowSize);
        System.arraycopy(attributes, lineStartIndex, lc.attributes, textOverflowSize, lineOverflowSize);
        return lc;
    }

    void shiftExistingContentAndInsertText(int copiedSize, char[] text, int[] attr, int lineStartIndex, int textStartIndex, int textLen, int textOverflowStart){
        if(copiedSize > 0){
            System.arraycopy(characters, lineStartIndex, characters, lineStartIndex + textLen, copiedSize);
            System.arraycopy(attributes, lineStartIndex, attributes, lineStartIndex + textLen, copiedSize);
        }
        System.arraycopy(text, textStartIndex, characters, lineStartIndex, textOverflowStart - textStartIndex);
        System.arraycopy(attr, textStartIndex, attributes, lineStartIndex, textOverflowStart - textStartIndex);
    }

    boolean empty(){
        for (int attr : attributes) {
            if ((attr & Style.StyleFlag.EMPTY.value) == 0) return false;
        }
        return true;
    }

    boolean isEmpty(int i){
        return (attributes[i] & Style.StyleFlag.EMPTY.value) != 0;
    }

    public static final char WIDE_PLACEHOLDER = '\u0000';

    void setWide(int col, char character, int attributes) {

        attributes &= ~Style.StyleFlag.EMPTY.value;

        this.characters[col] = character;
        this.attributes[col] = attributes;

        this.characters[col + 1] = WIDE_PLACEHOLDER;
        this.attributes[col + 1] = attributes;
    }

}
