package terminalbuffer;

import terminalbuffer.queue.BoundedQueue;

import java.util.ArrayList;

public class TerminalBuffer {
    private int width, height;
    private int maxScrollback;
    private int currentAttributes;
    private final BoundedQueue<Line> scrollback;
    private final ArrayList<Line> screen;
    private final Cursor cursor;

    public TerminalBuffer(int width, int height, int maxScrollback){
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;

        screen = new ArrayList<>();
        scrollback = new BoundedQueue<>(maxScrollback);

        currentAttributes = Style.setAttributes(Style.Color.WHITE, Style.Color.BLACK, Style.NONE);
        cursor = new Cursor(this);

        for (int i = 0; i < height; i++){
            screen.add(new Line(this));
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
        for(Character c : text.toCharArray()){
            Cell cell = new Cell(c, currentAttributes);
            screen.get(cursor.row()).set(cursor.col(), cell);
            if(cursor.col() == width - 1)
                newLine();
            cursor.advance();
        }
    }

    private void moveToScrollBack(Line line){
        if(maxScrollback != 0){
            if(!scrollback.empty()){
                scrollback.pop();
            }
            scrollback.push(line);
        }
    }

    public void newLine(){
        if(cursor.row() < height - 1)
            return;
        Line removed = screen.removeFirst();
        moveToScrollBack(removed);
        screen.addLast(new Line(this));
    }

    public void fillLine(int i, Character character){
        Line line = screen.get(i);
        for(int k = 0; k < width; k++){
            Cell cell = line.get(k);
            line.set(k, new Cell(character, cell.attributes()));
        }
    }

    public void clearScreen(){
        for(Line line : screen){
            moveToScrollBack(line);
        }
        screen.clear();
    }

    public void clearScreenAndScrollback(){
        screen.clear();
        scrollback.clear();
    }

    String lineToString(int i){
        return screen.get(i).toString();
    }

    String screenToString(){
        StringBuilder sb = new StringBuilder();
        for(Line line : screen){
            sb.append(line.toString());
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
        return screen.get(row).get(col).character();
    }

    int attributesAtScreen(int row, int col){
        return screen.get(row).get(col).attributes();
    }

    Character charAtScrollBack(int row, int col){
        return scrollback.get(row).get(col).character();
    }

    int attributesAtScrollback(int row, int col){
        return scrollback.get(row).get(col).attributes();
    }

}