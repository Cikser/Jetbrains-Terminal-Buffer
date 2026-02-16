package terminalbuffer;

import terminalbuffer.queue.BoundedQueue;

import java.util.ArrayList;

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
            screen.get(cursor.row()).set(cursor.col(), c, currentAttributes);
            cursor.advance();
        }
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
        addEmptyLine();
    }

    public void addEmptyLine(){
        screen.push(new Line(this));
    }

    public void fillLine(int i, char character){
        Line line = screen.get(i);
        for(int k = 0; k < width; k++){
            int attributes = line.getAttributes(k);
            line.set(k, character, attributes);
        }
    }

    public void clearScreen(){
        screen.clear();
        for(int i = 0; i < height; i++){
            addEmptyLine();
        }
        cursor.set(0, 0);
    }

    public void clearScreenAndScrollback(){
        clearScreen();
        scrollback.clear();
    }

    String lineToString(int i){
        return screen.get(i).toString();
    }

    String screenToString(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < screen.size(); i++){
            sb.append(screen.get(i).toString());
        }
        return sb.toString();
    }

    String screenAndScrollbackToString(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < scrollback.size(); i++){
            sb.append(scrollback.get(i).toString());
        }
        sb.append(screenToString());
        return sb.toString();
    }

    Character charAtScreen(int row, int col){
        return screen.get(row).getChar(col);
    }

    int attributesAtScreen(int row, int col){
        return screen.get(row).getAttributes(col);
    }

    Character charAtScrollBack(int row, int col){
        return scrollback.get(row).getChar(col);
    }

    int attributesAtScrollback(int row, int col){
        return scrollback.get(row).getAttributes(col);
    }

}