package terminalbuffer;

import java.util.ArrayList;
import java.util.Arrays;

public class Line {
    private final char[] characters;
    private final int[] attributes;

    Line(TerminalBuffer owner){
        characters = new char[owner.width()];
        attributes = new int[owner.width()];

        for(int i = 0; i < owner.width(); i++){
            characters[i] = ' ';
            attributes[i] = owner.currentAttributes();
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
    }

    @Override
    public String toString() {
        return new String(characters);
    }

}
