package terminalbuffer;

import java.lang.reflect.Array;
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
        int textOverflowSize = textLen > capacity ? textLen - (capacity - index) : 0;

        int lineStartIndex = index;
        int lineEndIndex = empty() ? index : capacity;
        int lineOverflowSize = lineEndIndex - lineStartIndex;

        int elementsToCopy = lineOverflowSize - textLen;
        if(elementsToCopy > 0){
            lineStartIndex += elementsToCopy;
            lineOverflowSize -= elementsToCopy;
        }

        int overflowSize = textOverflowSize + lineOverflowSize;
        int textOverflowStart = textStartIndex + textLen - textOverflowSize;

        LineContent lc = createLineContent(overflowSize, text, attr, lineStartIndex, lineOverflowSize, textOverflowStart, textOverflowSize);
        copyFromTextAndMove(elementsToCopy, text, attr, index, textLen, textOverflowStart);

        return lc;
    }

    private LineContent createLineContent(int overflowSize, String text, int[] attr, int lineStartIndex, int lineOverflowSize, int textOverflowStart, int textOverflowSize){
        if(overflowSize <= 0) return null;
        LineContent lc = new LineContent();
        lc.characters = new char[overflowSize];
        lc.attributes = new int[overflowSize];

        System.arraycopy(characters, lineStartIndex, lc.characters, 0, lineOverflowSize);
        System.arraycopy(attributes, lineStartIndex, lc.attributes, 0, lineOverflowSize);
        System.arraycopy(text.toCharArray(), textOverflowStart, lc.characters, lineOverflowSize, textOverflowSize);
        System.arraycopy(attr, textOverflowStart, lc.attributes, lineOverflowSize, textOverflowSize);
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
        for (int i = 0; i < empty.length; i++){
            if(!empty[i]) return false;
        }
        return true;
    }

}
