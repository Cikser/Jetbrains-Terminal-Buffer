package terminalbuffer;

import terminalbuffer.queue.BoundedQueue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;

public class TerminalBuffer {
    private int width, height;
    private int maxScrollback;
    private int currentAttributes;
    private final BoundedQueue<Line> scrollback;
    private final BoundedQueue<Line> screen;
    private final Cursor cursor;

    public TerminalBuffer(int width, int height, int maxScrollback){
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;

        screen = new BoundedQueue<>(height);
        scrollback = new BoundedQueue<>(maxScrollback);

        currentAttributes = Style.setAttributes(Style.Color.WHITE, Style.Color.BLACK, Style.NONE);
        cursor = new Cursor(this);

        for (int i = 0; i < height; i++){
            screen.push(new Line(this));
        }
    }

    public int height() {
        return height;
    }

    public int width() {
        return width;
    }

    public int currentAttributes() {
        return currentAttributes;
    }

    public void setAttributes(int fg, int bg, int attributes){
        currentAttributes = Style.setAttributes(fg, bg, attributes);
    }

    public Cursor cursor(){
        return cursor;
    }

    public void write(String text){
        for(char c : text.toCharArray()){
            if(c == '\r' || c == '\n') {
                cursor.handleChar(c);
                continue;
            }
            screen.get(cursor.row()).set(cursor.col(), c, currentAttributes);
            cursor.advance();
        }
    }

    public void write(String text, int row, int col){
        cursor.set(row, col);
        write(text);
    }

    private void moveToScrollBack(Line line){
        if(maxScrollback == 0) return;

        if(scrollback.size() == maxScrollback){
            scrollback.pop();
        }
        scrollback.push(line);
    }

    void scroll(){
        Line removed = screen.pop();
        moveToScrollBack(removed);
        screen.push(new Line(this));
    }

    public void addEmptyLine(){
        scroll();
    }

    public void fillLine(int i, char character){
        screen.get(i).fill(character);
    }

    public void clearScreen(){
        screen.clear();
        for(int i = 0; i < height; i++){
            screen.push(new Line(this));
        }
        cursor.set(0, 0);
    }

    public void clearScreenAndScrollback(){
        clearScreen();
        scrollback.clear();
    }

    public String lineToString(int i){
        return screen.get(i).toString();
    }

    public String screenToString(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < screen.size(); i++){
            sb.append(screen.get(i).toString());
            sb.append('\n');
        }
        return sb.toString();
    }

    public String screenAndScrollbackToString(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < scrollback.size(); i++){
            sb.append(scrollback.get(i).toString());
            sb.append('\n');
        }
        sb.append(screenToString());
        return sb.toString();
    }

    public char charAtScreen(int row, int col){
        return screen.get(row).getChar(col);
    }

    public int attributesAtScreen(int row, int col){
        return screen.get(row).getAttributes(col);
    }

    public char charAtScrollBack(int row, int col){
        return scrollback.get(row).getChar(col);
    }

    public int attributesAtScrollback(int row, int col){
        return scrollback.get(row).getAttributes(col);
    }

    private final Deque<LineContent> insertQueue = new ArrayDeque<>();

    private void insertAndOverflow(String text, int[] attributes){
        int i = 0;
        int lastControl = -1;
        while (i < text.length()){
            int controlChar = findControl(lastControl, text);

            if(controlChar < text.length()){
                cursor.handleChar(text.charAt(controlChar));
                lastControl = controlChar;
                i = controlChar + 1;
                continue;
            }

            Line line = screen.get(cursor.row());
            LineContent lc = line.insertAndOverflow(cursor.col(), text, attributes, lastControl + 1, controlChar);
            int shift = calcCursorShift(lc, lastControl + 1, controlChar);
            cursor.right(shift);
            cursor.advance();
            if(lc != null) insertQueue.push(lc);
            i = controlChar + 1;
        }
    }

    private int calcCursorShift(LineContent lc, int textStartIndex, int textEndIndex){
        int substringLen = textEndIndex - textStartIndex;
        if(lc != null)
            substringLen = width;
        int shift = Math.min(width, substringLen - 1);
        return shift;
    }

    private int findControl(int lastControl, String text){
        for(int i = lastControl + 1; i < text.length(); i++){
            if(text.charAt(i) == '\r' || text.charAt(i) == '\n')
                return i;
        }
        return text.length();
    }

    public void insert(String text){
        int[] attributes = new int[text.length()];
        Arrays.fill(attributes, currentAttributes);
        insertAndOverflow(text, attributes);
        while (!insertQueue.isEmpty()){
            LineContent lc = insertQueue.pop();
            if(lc == null)
                continue;
            text = new String(lc.characters);
            attributes = lc.attributes;
            insertAndOverflow(text, attributes);
        }
    }

    public void insert(String text, int row, int col){
        cursor.set(row, col);
        insert(text);
    }

    void print(){
        for(int i = 0; i < screen.size(); i++) {
            System.out.println(screen.get(i).toString());
        }
    }


}