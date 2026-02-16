package terminalbuffer;

import java.util.Arrays;

class LineContent{
    char[] characters;
    int[] attributes;
}

public class Line {
    private char[] characters;
    private int[] attributes;
    private boolean[] empty;

    Line(TerminalBuffer owner){
        characters = new char[owner.width()];
        attributes = new int[owner.width()];
        empty = new boolean[owner.width()];

        for(int i = 0; i < owner.width(); i++){
            characters[i] = ' ';
            attributes[i] = owner.currentAttributes();
            empty[i] = true;
        }
    }

    char getChar(int i){
        return characters[i];
    }

    int getAttributes(int i){
        return attributes[i];
    }

    void set(int i, char character, int attributes){
        this.characters[i] = character;
        this.attributes[i] = attributes;
        this.empty[i] = false;
    }

    @Override
    public String toString() {
        return new String(characters);
    }

    void fill(char character){
        Arrays.fill(characters, character);
        if(character != ' '){
            Arrays.fill(empty, false);
        }
    }

    LineContent insertAndOverflow(int index, String text, int[] attr, int textStartIndex, int textEndIndex){
        int capacity = characters.length;
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

        LineContent lc = createLineContent(overflowSize, text, attr, lineStartIndex, lineOverflowSize, textOverflowStart, textOverflowSize);
        copyFromTextAndMove(elementsToCopy, text, attr, index, textLen, textOverflowStart);

        return lc;
    }

    private LineContent createLineContent(int overflowSize, String text, int[] attr, int lineStartIndex, int lineOverflowSize, int textOverflowStart, int textOverflowSize){
        if(overflowSize <= 0) return null;
        LineContent lc = new LineContent();
        lc.characters = new char[overflowSize];
        lc.attributes = new int[overflowSize];

        System.arraycopy(text.toCharArray(), textOverflowStart, lc.characters, 0, textOverflowSize);
        System.arraycopy(attr, textOverflowStart, lc.attributes, 0, textOverflowSize);
        System.arraycopy(characters, lineStartIndex, lc.characters, textOverflowSize, lineOverflowSize);
        System.arraycopy(attributes, lineStartIndex, lc.attributes, textOverflowSize, lineOverflowSize);
        return lc;
    }

    void copyFromTextAndMove(int copiedSize, String text, int[] attr,  int lineStartIndex, int textLen, int textOverflowStart){
        if(copiedSize > 0){
            System.arraycopy(characters, lineStartIndex, characters, lineStartIndex + textLen, copiedSize);
            System.arraycopy(attributes, lineStartIndex, attributes, lineStartIndex + textLen, copiedSize);
        }
        System.arraycopy(text.toCharArray(), 0, characters, lineStartIndex, textOverflowStart);
        System.arraycopy(attr, 0, attributes, lineStartIndex, textOverflowStart);
    }

    boolean empty(){
        for (boolean isEmpty : empty) {
            if (!isEmpty) return false;
        }
        return true;
    }

}
