package terminalbuffer;

import terminalbuffer.queue.BoundedQueue;

import java.util.ArrayList;

public class TerminalBuffer {
    private int width, height;
    private int maxScrollback;
    private int currentAttributes;
    private BoundedQueue<Line> scrollback;
    private ArrayList<Line> screen;
    private Cursor cursor;

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
        Style.setAttributes(fg, bg, attributes);
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

    private void print(){
        for(Line line : screen){
            System.out.println(line.toString());
        }
    }

    public void newLine(){
        if(cursor.row() < height - 1)
            return;
        Line removed = screen.removeFirst();
        if(maxScrollback != 0){
            if(scrollback.size() != 0){
                scrollback.pop();
            }
            scrollback.push(removed);
        }
        screen.addLast(new Line(this));
    }

}