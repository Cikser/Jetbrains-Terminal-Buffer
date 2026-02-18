package terminalbuffer;

import terminalbuffer.queue.BoundedQueue;

import java.util.*;

public class TerminalBuffer {
    private int width, height;
    private final int maxScrollback;
    private int currentAttributes;
    private final BoundedQueue<Line> scrollback;
    private final BoundedQueue<Line> screen;
    private Cursor cursor;

    void setWrapped(int i){
        screen.get(i).setWrapped();
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

    public void write(String text) {
        char[] chars = text.toCharArray();
        int i = 0;

        while (i < chars.length) {
            if(chars[i] == Line.WIDE_PLACEHOLDER){
                i++;
                continue;
            }

            int next = findControlOrWide(i, chars);

            writeChunk(chars, i, next);

            if (next < chars.length) {
                char c = chars[next];
                if (c == '\r' || c == '\n') {
                    cursor.handleChar(c);
                } else if (WideCharUtil.isWide(c)) {
                    writeWideChar(c);
                }
            }
            i = next + 1;
        }
    }

    private void writeChunk(char[] chars, int start, int end) {
        if(end <= start) return;
        int current = start;
        while (current < end) {
            cursor.resolveWrap();
            Line line = screen.get(cursor.row());
            int spaceInLine = width - cursor.col();
            int toWrite = Math.min(spaceInLine, end - current);
            if (toWrite <= 0) {
                cursor.advance();
                continue;
            }
            line.writeBlock(cursor.col(), chars, current, toWrite, currentAttributes);
            cursor.right(toWrite - 1);
            cursor.advance();
            current += toWrite;
        }
    }

    private void writeWideChar(char c) {
        cursor.resolveWrap();
        if (cursor.col() == width - 1) {
            cursor.advance();
            cursor.resolveWrap();
        }
        Line line = screen.get(cursor.row());
        line.setWide(cursor.col(), c, currentAttributes);
        cursor.advanceForWideChar();
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
        screen.get(i).fill(character, currentAttributes);
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

    private void insertAndOverflow(String text, int[] attributes, Deque<LineContent> insertQueue){
        cursor.resolveWrap();
        int i = 0;
        char[] chars = text.toCharArray();
        while (i < chars.length){
            if(chars[i] == Line.WIDE_PLACEHOLDER) {
                i++;
                continue;
            }
            int next = findControlOrWide(i, chars);

            processTextChunk(chars, attributes, i, next, insertQueue);

            if(next < chars.length){
                char c = text.charAt(next);
                if(c == '\r' || c == '\n')
                    cursor.handleChar(c);
                else{
                    handleWideCharInsert(chars, attributes, next, insertQueue);
                }
            }
            i = next + 1;
        }
    }

    private void handleWideCharInsert(char[] text, int[] attributes, int start, Deque<LineContent> insertQueue){
        cursor.resolveWrap();
        if (cursor.col() == width - 1) {
            cursor.advance();
            cursor.resolveWrap();
        }
        Line line = screen.get(cursor.row());
        char c = text[start];
        int attr = attributes[start];
        LineContent lc = line.insertWideAndOverflow(cursor.col(), c, attr);
        cursor.advanceForWideChar();
        moveCursorAfterInsert(lc, start, start + 2);
        if (lc != null) insertQueue.push(lc);
    }

    private void processTextChunk(char[] text, int[] attributes, int start, int end, Deque<LineContent> insertQueue){
        if(start == end) return;
        Line line = screen.get(cursor.row());
        LineContent lc = line.insertAndOverflow(cursor.col(), text, attributes, start, end);
        moveCursorAfterInsert(lc, start, end);
        if(lc != null) insertQueue.push(lc);
    }

    private void moveCursorAfterInsert(LineContent lc, int textStartIndex, int textEndIndex){
        int shift = calcCursorShift(lc, textStartIndex, textEndIndex);
        cursor.right(shift);
        cursor.resolveWrap();
        cursor.advance();
    }

    private int calcCursorShift(LineContent lc, int textStartIndex, int textEndIndex){
        int substringLen = textEndIndex - textStartIndex;
        if(lc != null)
            substringLen = width;
        return Math.min(width, substringLen - 1);
    }

    private int findControlOrWide(int start, char[] text){
        for(int i = start; i < text.length; i++){
            char c = text[i];
            if(c == '\r' || c == '\n' || WideCharUtil.isWide(c))
                return i;
        }
        return text.length;
    }

    public void insert(String text) {
        cursor.resolveWrap();
        Deque<LineContent> insertQueue = new ArrayDeque<>();

        Cursor newCursor = calcFinalCursorPos(text);

        StringBuilder expandedText = new StringBuilder();
        List<Integer> expandedAttrs = new ArrayList<>();

        for (char c : text.toCharArray()) {
            expandedText.append(c);
            expandedAttrs.add(currentAttributes);
            if (WideCharUtil.isWide(c)) {
                expandedText.append(Line.WIDE_PLACEHOLDER);
                expandedAttrs.add(currentAttributes);
            }
        }

        String expanded = expandedText.toString();
        int[] attributes = new int[expandedAttrs.size()];
        for (int i = 0; i < expandedAttrs.size(); i++) {
            attributes[i] = expandedAttrs.get(i);
        }

        insertAndOverflow(expanded, attributes, insertQueue);
        while (!insertQueue.isEmpty()) {
            LineContent lc = insertQueue.pop();
            insertAndOverflow(new String(lc.characters), lc.attributes, insertQueue);
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
            if (WideCharUtil.isWide(c)) {
                if (newCursor.col() == width - 1) {
                    newCursor.advance();
                    newCursor.resolveWrap();
                }
                newCursor.advanceForWideChar();
            } else {
                newCursor.advance();
            }
        }

        return newCursor;
    }

    public void insert(String text, int row, int col){
        cursor.set(row, col);
        insert(text);
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

    public void resize(int newWidth, int newHeight) {
        List<Line> allLines = collectLinesForReflow();
        CursorAnchor anchor = calculateCursorAnchor(allLines);

        ReflowResult result = performFastReflow(allLines, newWidth, anchor);

        this.width = newWidth;
        this.height = newHeight;
        rebuildBuffers(result.newLines, newHeight);

        restoreCursorPosition(result.newCursorRow, result.newCursorCol, result.newLines.size());
    }


    private record CursorAnchor(int blockIndex, int offset) {}
    private record ReflowResult(List<Line> newLines, int newCursorRow, int newCursorCol) {}


    private ReflowResult performFastReflow(List<Line> allLines, int newWidth, CursorAnchor anchor) {
        List<Line> newLines = new ArrayList<>(allLines.size());
        int resRow = -1, resCol = -1;
        int currentBlockIdx = 0;

        int i = 0;
        while (i < allLines.size()) {
            int start = i;
            int end = i;
            while (end + 1 < allLines.size() && allLines.get(end + 1).isWrapped()) {
                end++;
            }

            int effectiveLen = calculateEffectiveLength(allLines, start, end);

            int logicSize = (currentBlockIdx == anchor.blockIndex)
                    ? Math.max(effectiveLen, anchor.offset + 1)
                    : effectiveLen;

            for (int offset = 0; offset < logicSize || (logicSize == 0 && offset == 0); offset += newWidth) {
                Line newLine = new Line(newWidth, currentAttributes);
                if (offset > 0) newLine.setWrapped();

                if (currentBlockIdx == anchor.blockIndex && anchor.offset >= offset && anchor.offset < offset + newWidth) {
                    resRow = newLines.size();
                    resCol = anchor.offset - offset;
                }

                copyFromOriginal(allLines, start, end, newLine, offset, newWidth);
                newLines.add(newLine);
            }

            currentBlockIdx++;
            i = end + 1;
        }
        return new ReflowResult(newLines, resRow, resCol);
    }

    private int calculateEffectiveLength(List<Line> lines, int start, int end) {
        for (int l = end; l >= start; l--) {
            Line line = lines.get(l);
            for (int c = this.width - 1; c >= 0; c--) {
                if (!line.isEmpty(c) && (line.getChar(c) != ' ' || line.getAttributes(c) != currentAttributes)) {
                    return (l - start) * this.width + (c + 1);
                }
            }
        }
        return 0;
    }


    private void copyFromOriginal(List<Line> allLines, int start, int end, Line target, int startOffset, int targetWidth) {
        for (int j = 0; j < targetWidth; j++) {
            int globalOffset = startOffset + j;
            int lineInBlock = globalOffset / this.width;
            int colInLine = globalOffset % this.width;

            if (start + lineInBlock <= end) {
                Line source = allLines.get(start + lineInBlock);
                target.set(j, source.getChar(colInLine), source.getAttributes(colInLine));
            } else {
                break;
            }
        }
    }

    private List<Line> collectLinesForReflow() {
        List<Line> all = new ArrayList<>(scrollback.size() + findLastNonEmptyScreenLine());
        for (int i = 0; i < scrollback.size(); i++) all.add(scrollback.get(i));
        for (int i = 0; i < findLastNonEmptyScreenLine(); i++) {
            Line line = screen.get(i);
            all.add(line);
        }
        return all;
    }

    private int findLastNonEmptyScreenLine(){
        for (int i = screen.size() - 1; i >= 0; i--){
            if(screen.get(i).empty() && i != cursor.row())
                continue;
            return i + 1;
        }
        return 0;
    }

    private CursorAnchor calculateCursorAnchor(List<Line> lines) {
        int blockIdx = 0;
        int currentOffset = 0;
        Line cursorLine = screen.get(cursor.row());

        for (Line line : lines) {
            if (!line.isWrapped() && currentOffset > 0) {
                blockIdx++;
                currentOffset = 0;
            }
            if (line == cursorLine) {
                return new CursorAnchor(blockIdx, currentOffset + cursor.col());
            }
            currentOffset += this.width;
        }
        return new CursorAnchor(0, 0);
    }

    private void restoreCursorPosition(int newRow, int newCol, int totalLines) {
        int screenStart = Math.max(0, totalLines - height);
        if (newRow != -1) {
            int relativeRow = newRow - screenStart;
            if (relativeRow < 0) cursor.set(0, 0);
            else cursor.set(relativeRow, Math.min(newCol, width - 1));
        } else {
            cursor.set(Math.max(0, screen.size() - 1), 0);
        }
    }

    private void rebuildBuffers(List<Line> newLines, int newHeight) {
        scrollback.clear();
        screen.resizeAndClear(newHeight);

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

}