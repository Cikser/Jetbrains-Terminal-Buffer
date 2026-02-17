package terminalbuffer;

import terminalbuffer.queue.BoundedQueue;

import java.util.*;

public class TerminalBuffer {
    private int width, height;
    private int maxScrollback;
    private int currentAttributes;
    private final BoundedQueue<Line> scrollback;
    private final BoundedQueue<Line> screen;
    private Cursor cursor;

    void setWrapped(int i){
        screen.get(i).setWrapped(true);
    }

    public TerminalBuffer(int width, int height, int maxScrollback){
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;

        screen = new BoundedQueue<>(height);
        scrollback = new BoundedQueue<>(maxScrollback);

        currentAttributes = Style.setAttributes(Style.Color.WHITE, Style.Color.BLACK, Style.StyleFlag.NONE);
        cursor = new Cursor(this);

        for (int i = 0; i < height; i++){
            screen.push(new Line(width, currentAttributes));
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

    public void setAttributes(Style.Color fg, Style.Color bg, EnumSet<Style.StyleFlag> attributes){
        currentAttributes = Style.setAttributes(fg, bg, attributes);
    }

    public void setAttributes(Style.Color fg, Style.Color bg, Style.StyleFlag attributes){
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
            cursor.resolveWrap();
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
        screen.push(new Line(width, currentAttributes));
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
            screen.push(new Line(width, currentAttributes));
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

    private void insertAndOverflow(String text, int[] attributes, boolean[] empty, Deque<LineContent> insertQueue){
        cursor.resolveWrap();
        int i = 0;
        int lastControl = -1;
        while (i < text.length()){
            int controlChar = findControl(lastControl, text);

            Line line = screen.get(cursor.row());
            LineContent lc = line.insertAndOverflow(cursor.col(), text, attributes, empty, lastControl + 1, controlChar);
            int shift = calcCursorShift(lc, lastControl + 1, controlChar);
            cursor.right(shift);
            cursor.resolveWrap();
            cursor.advance();
            if(lc != null) insertQueue.push(lc);
            if(controlChar < text.length()){
                cursor.handleChar(text.charAt(controlChar));
                lastControl = controlChar;
            }
            i = controlChar + 1;
        }
    }

    private int calcCursorShift(LineContent lc, int textStartIndex, int textEndIndex){
        int substringLen = textEndIndex - textStartIndex;
        if(lc != null)
            substringLen = width;
        return Math.min(width, substringLen - 1);
    }

    private int findControl(int lastControl, String text){
        for(int i = lastControl + 1; i < text.length(); i++){
            if(text.charAt(i) == '\r' || text.charAt(i) == '\n')
                return i;
        }
        return text.length();
    }

    public void insert(String text){
        cursor.resolveWrap();
        int[] attributes = buildAttr(text.length());
        boolean[] empty = buildEmpty(text.length());
        Deque<LineContent> insertQueue = new ArrayDeque<>();

        Cursor newCursor = calcFinalCursorPos(text);

        insertAndOverflow(text, attributes, empty, insertQueue);
        while (!insertQueue.isEmpty()){
            LineContent lc = insertQueue.pop();
            insertAndOverflow(new String(lc.characters), lc.attributes, lc.empty, insertQueue);
        }

        cursor = newCursor;
    }

    private Cursor calcFinalCursorPos(String text) {
        Cursor newCursor = new Cursor(this);
        newCursor.set(cursor.row(), cursor.col());

        for (char c : text.toCharArray()) {
            if (c == '\n' || c == '\r') {
                newCursor.handleChar(c);
                continue;
            }
            newCursor.resolveWrap();
            newCursor.advance();
        }

        return newCursor;
    }

    public void insert(String text, int row, int col){
        cursor.set(row, col);
        insert(text);
    }

    private int[] buildAttr(int length){
        int[] attr = new int[length];
        Arrays.fill(attr, currentAttributes);
        return attr;
    }

    private boolean[] buildEmpty(int length){
        boolean[] empty = new boolean[length];
        Arrays.fill(empty, false);
        return empty;
    }

    public char getChar(int i, int j){
        if(i >= 0){
            return screen.get(i).getChar(j);
        }
        return scrollback.get(scrollback.size() + i).getChar(j);
    }

    public int getAttributes(int i, int j){
        if(i >= 0){
            return screen.get(i).getAttributes(j);
        }
        return scrollback.get(scrollback.size() + i).getAttributes(j);
    }

    public String getLine(int i){
        if(i >= 0){
            return screen.get(i).toString();
        }
        return scrollback.get(scrollback.size() + i).toString();
    }

    public int scrollbackSize() {
        return scrollback.size();
    }


    record StyledChar(char c, int attr, boolean empty) {}

    public void resize(int newWidth, int newHeight) {
        List<Line> allPhysicalLines = new ArrayList<>();
        for (int i = 0; i < scrollback.size(); i++) allPhysicalLines.add(scrollback.get(i));
        for (int i = 0; i < screen.size(); i++) {
            if(!screen.get(i).empty())
                allPhysicalLines.add(screen.get(i));
        }

        List<List<StyledChar>> logicalBlocks = new ArrayList<>();
        List<StyledChar> currentBlock = new ArrayList<>();

        for (Line line : allPhysicalLines) {
            if (!line.isWrapped() && !currentBlock.isEmpty()) {
                logicalBlocks.add(new ArrayList<>(currentBlock));
                currentBlock.clear();
            }

            for (int col = 0; col < this.width; col++) {
                currentBlock.add(new StyledChar(line.getChar(col), line.getAttributes(col), line.isEmpty(col)));
            }
        }
        if (!currentBlock.isEmpty()) logicalBlocks.add(currentBlock);

        this.width = newWidth;
        this.height = newHeight;

        List<Line> newPhysicalLines = new ArrayList<>();
        for (List<StyledChar> block : logicalBlocks) {
            trimTrailingSpaces(block);

            for (int i = 0; i < block.size(); i += newWidth) {
                Line newLine = new Line(width, currentAttributes);
                if (i > 0) newLine.setWrapped(true);

                for (int j = 0; j < newWidth && (i + j) < block.size(); j++) {
                    StyledChar sc = block.get(i + j);
                    newLine.set(j, sc.c(), sc.attr());
                }
                newPhysicalLines.add(newLine);
            }
            if (block.isEmpty()) {
                newPhysicalLines.add(new Line(width, currentAttributes));
            }
        }

        rebuildBuffers(newPhysicalLines, newHeight);

        cursor.set(0, 0);
    }

    private void trimTrailingSpaces(List<StyledChar> block) {
        int lastNonSpace = -1;
        for (int i = 0; i < block.size(); i++) {
            if (block.get(i).c() != ' ') lastNonSpace = i;
        }
        if (lastNonSpace < block.size() - 1) {
            block.subList(lastNonSpace + 1, block.size()).clear();
        }
    }

    private void rebuildBuffers(List<Line> newLines, int newHeight) {
        scrollback.clear();
        screen.clear();

        int totalNewLines = newLines.size();

        int screenStart = Math.max(0, totalNewLines - newHeight);

        for (int i = 0; i < screenStart; i++) {
            moveToScrollBack(newLines.get(i));
        }

        for (int i = screenStart; i < totalNewLines; i++) {
            screen.push(newLines.get(i));
        }

        while (screen.size() < newHeight) {
            screen.push(new Line(width, currentAttributes));
        }
    }

    public static void main(String[] args){
        TerminalBuffer buffer = new TerminalBuffer(5, 6, 100);
        buffer.write("AAAAAAAA\nAAA\nAAAAAAAA");
        System.out.println(buffer.screenToString());
        buffer.resize(4, buffer.height());
        System.out.println(buffer.screenToString());
        buffer.resize(5, buffer.height());
        System.out.println(buffer.screenToString());

    }
}