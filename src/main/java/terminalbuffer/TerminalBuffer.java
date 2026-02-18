package terminalbuffer;

import terminalbuffer.queue.BoundedQueue;

import java.util.*;

/**
 * Terminal text buffer implementation with scrollback support, attribute handling,
 * cursor management, wide character support, and dynamic resizing with content reflow.
 *
 * <p>This class provides the core data structure for terminal emulators, managing
 * a fixed-height visible screen and a bounded scrollback buffer of historical lines.
 *
 * <p>Features:
 * <ul>
 *   <li>Write and insert operations with automatic line wrapping</li>
 *   <li>16 foreground/background colors with bold, italic, underline styles</li>
 *   <li>Cursor movement with VT100-style pending wrap semantics</li>
 *   <li>Wide character support (CJK, emoji) occupying 2 cells</li>
 *   <li>Dynamic resize with content reflow</li>
 *   <li>Bounded scrollback with automatic eviction</li>
 * </ul>
 *
 * @see Cursor
 * @see Style
 * @see Line
 */
public class TerminalBuffer {
    private int width, height;
    private final int maxScrollback;
    private int currentAttributes;
    private final BoundedQueue<Line> scrollback;
    private final BoundedQueue<Line> screen;
    private Cursor cursor;

    /**
     * Creates a new terminal buffer with specified dimensions.
     *
     * <p>The buffer is initialized with empty lines using default attributes
     * The cursor starts at position (0, 0).
     *
     * @param width buffer width in characters (columns)
     * @param height visible screen height in lines (rows)
     * @param maxScrollback maximum number of scrollback lines to retain (0 to disable)
     */
    public TerminalBuffer(int width, int height, int maxScrollback){
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;

        screen = new BoundedQueue<>(height);
        scrollback = new BoundedQueue<>(maxScrollback);

        currentAttributes = Style.setAttributes(Style.Color.FG_DEFAULT, Style.Color.BG_DEFAULT, Style.StyleFlag.NONE);
        cursor = new Cursor(this);

        for (int i = 0; i < height; i++){
            screen.push(new Line(width, currentAttributes));
        }
    }

    /**
     * Returns the current buffer height (visible screen lines).
     *
     * @return height in lines
     */
    public int height() {
        return height;
    }

    /**
     * Returns the current buffer width.
     *
     * @return width in characters
     */
    public int width() {
        return width;
    }

    /**
     * Returns the current attribute settings as a bit-packed integer.
     *
     * <p>The returned value encodes foreground color, background color,
     * and style flags (bold, italic, underline) used for subsequent write operations.
     *
     * @return bit-packed attribute value
     * @see Style#setAttributes
     */
    public int currentAttributes() {
        return currentAttributes;
    }

    /**
     * Sets the current text attributes with multiple style flags.
     *
     * <p>All subsequent write operations will use these attributes until changed.
     *
     * @param fg foreground color
     * @param bg background color
     * @param attributes set of style flags (bold, italic, underline)
     * @see Style.Color
     * @see Style.StyleFlag
     */
    public void setAttributes(Style.Color fg, Style.Color bg, EnumSet<Style.StyleFlag> attributes){
        currentAttributes = Style.setAttributes(fg, bg, attributes);
    }

    /**
     * Sets the current text attributes with a single style flag.
     *
     * <p>All subsequent write operations will use these attributes until changed.
     *
     * @param fg foreground color
     * @param bg background color
     * @param attributes style flag (NONE, BOLD, ITALIC, or UNDERLINE)
     * @see Style.Color
     * @see Style.StyleFlag
     */
    public void setAttributes(Style.Color fg, Style.Color bg, Style.StyleFlag attributes){
        currentAttributes = Style.setAttributes(fg, bg, attributes);
    }

    /**
     * Returns the cursor object for position management.
     *
     * <p>The cursor can be moved using methods like {@link Cursor#set},
     * {@link Cursor#up}, {@link Cursor#down}, {@link Cursor#left}, {@link Cursor#right}.
     *
     * @return the cursor instance
     */
    public Cursor cursor(){
        return cursor;
    }

    /**
     * Writes text at the current cursor position.
     *
     * <p>Text is written character-by-character, advancing the cursor after each character.
     * Lines wrap automatically when reaching the right edge. Control characters
     * (\n, \r) are handled appropriately. Wide characters (CJK, emoji) occupy 2 cells.
     *
     * <p>Behavior:
     * <ul>
     *   <li>Regular characters: written at cursor position, cursor advances by 1</li>
     *   <li>Wide characters: occupy 2 cells, cursor advances by 2</li>
     *   <li>\n: moves cursor to start of next line, scrolls if at bottom</li>
     *   <li>\r: moves cursor to start of current line</li>
     *   <li>Line wrap: when cursor reaches end, next character goes to next line</li>
     * </ul>
     *
     * @param text text to write (may contain newlines, carriage returns, wide characters)
     */
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

    /**
     * Writes text at a specific screen position.
     *
     * <p>This is equivalent to calling {@link Cursor#set} followed by {@link #write}.
     * The cursor is moved to the specified position before writing begins.
     *
     * @param text text to write
     * @param row target row (0-based, clamped to valid range)
     * @param col target column (0-based, clamped to valid range)
     */
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

    /**
     * Scrolls the screen up by one line.
     *
     * <p>The top line is moved to scrollback (if enabled), all lines shift up,
     * and a new empty line is added at the bottom.
     */
    public void addEmptyLine(){
        scroll();
        cursor.set(cursor.row() - 1, cursor.col());
    }

    /**
     * Fills an entire line with a specific character using current attributes.
     *
     * <p>This overwrites all existing content on the line. The cursor position
     * is not changed.
     *
     * @param i line index (0-based)
     * @param character character to fill the line with
     */
    public void fillLine(int i, char character){
        screen.get(i).fill(character, currentAttributes);
    }

    /**
     * Clears all content on the visible screen.
     *
     * <p>All screen lines are reset to empty (filled with spaces using current attributes).
     * Scrollback is preserved. The cursor is moved to position (0, 0).
     */
    public void clearScreen(){
        screen.clear();
        for(int i = 0; i < height; i++){
            screen.push(new Line(width, currentAttributes));
        }
        cursor.set(0, 0);
    }

    /**
     * Clears both the screen and scrollback buffer.
     *
     * <p>All content is permanently erased. The cursor is moved to position (0, 0).
     */
    public void clearScreenAndScrollback(){
        clearScreen();
        scrollback.clear();
    }

    /**
     * Returns the visible screen content as a string.
     *
     * <p>Each line is separated by a newline character. Trailing spaces are preserved.
     *
     * @return multi-line string representing screen content
     */
    public String screenToString(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < screen.size(); i++){
            sb.append(screen.get(i).toString());
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Returns the combined scrollback and screen content as a string.
     *
     * <p>Scrollback lines appear first (oldest to newest), followed by screen lines.
     * Each line is separated by a newline character.
     *
     * @return multi-line string representing all buffer content
     */
    public String screenAndScrollbackToString(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < scrollback.size(); i++){
            sb.append(scrollback.get(i).toString());
            sb.append('\n');
        }
        sb.append(screenToString());
        return sb.toString();
    }

    void setWrapped(int i){
        screen.get(i).setWrapped();
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

    /**
     * Inserts text at the current cursor position, shifting existing content right.
     *
     * <p>Unlike {@link #write}, insert does not overwrite existing content. Instead,
     * characters to the right of the cursor are shifted to make room for the new text.
     * Content that doesn't fit on the current line overflows to the next line, potentially
     * cascading through multiple lines.
     *
     * <p>Wide characters and control characters are handled appropriately. The cursor
     * is positioned at the end of the inserted text.
     *
     * @param text text to insert (may contain newlines, wide characters)
     */
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

    /**
     * Inserts text at a specific screen position.
     *
     * <p>This is equivalent to calling {@link Cursor#set} followed by {@link #insert}.
     * The cursor is moved to the specified position before insertion begins.
     *
     * @param text text to insert
     * @param row target row (0-based, clamped to valid range)
     * @param col target column (0-based, clamped to valid range)
     */
    public void insert(String text, int row, int col){
        cursor.set(row, col);
        insert(text);
    }

    /**
     * Retrieves a character at the specified position.
     *
     * <p>Supports unified access to both screen and scrollback using Python-style
     * negative indexing:
     * <ul>
     *   <li>row &gt;= 0: access screen (row 0 = top visible line)</li>
     *   <li>row &lt; 0: access scrollback (row -1 = most recent scrollback line)</li>
     * </ul>
     *
     * @param i row index (0-based for screen, negative for scrollback)
     * @param j column index (0-based)
     * @return character at the specified position
     * @throws IndexOutOfBoundsException if position is out of bounds
     */
    public char getChar(int i, int j){
        if(i >= 0){
            return screen.get(i).getChar(j);
        }
        return scrollback.get(scrollback.size() + i).getChar(j);
    }

    /**
     * Retrieves attributes at the specified position.
     *
     * <p>Returns the bit-packed attribute value encoding color and style information.
     * Supports unified access to both screen and scrollback using negative indexing
     * (see {@link #getChar} for details).
     *
     * @param i row index (0-based for screen, negative for scrollback)
     * @param j column index (0-based)
     * @return bit-packed attribute value
     * @throws IndexOutOfBoundsException if position is out of bounds
     * @see Style#setAttributes
     */
    public int getAttributes(int i, int j){
        if(i >= 0){
            return screen.get(i).getAttributes(j);
        }
        return scrollback.get(scrollback.size() + i).getAttributes(j);
    }

    /**
     * Retrieves an entire line as a string.
     *
     * <p>Supports unified access to both screen and scrollback using negative indexing
     * (see {@link #getChar} for details). Trailing spaces are preserved.
     *
     * @param i line index (0-based for screen, negative for scrollback)
     * @return line content as a string
     * @throws IndexOutOfBoundsException if line index is out of bounds
     */
    public String getLine(int i){
        if(i >= 0){
            return screen.get(i).toString();
        }
        return scrollback.get(scrollback.size() + i).toString();
    }

    /**
     * Returns the current number of lines in scrollback.
     *
     * @return number of scrollback lines (0 to maxScrollback)
     */
    public int scrollbackSize() {
        return scrollback.size();
    }

    /**
     * Resizes the terminal buffer, reflowing content to new dimensions.
     *
     * <p>Content is intelligently reflowed to fit the new width:
     * <ul>
     *   <li>Wider: wrapped lines may merge back into single lines</li>
     *   <li>Narrower: long lines are split across multiple lines</li>
     *   <li>Empty lines are preserved as separate paragraphs</li>
     *   <li>Wide characters maintain integrity across reflow</li>
     * </ul>
     *
     * <p>The cursor position is preserved by mapping it to the same logical position
     * in the reflowed content. If the cursor was in scrollback after resize, it maps to (0,0).
     *
     * <p>Both screen and scrollback participate in reflow. If the new screen is smaller,
     * excess lines move to scrollback.
     *
     * @param newWidth new buffer width (must be &gt; 0)
     * @param newHeight new visible screen height (must be &gt; 0)
     */
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
        List<Line> newLines = new ArrayList<>();
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

            int offset = 0;
            while (offset < logicSize || (logicSize == 0 && offset == 0)) {
                Line newLine = new Line(newWidth, currentAttributes);
                if (offset > 0) newLine.setWrapped();

                int consumed = copyFromOriginal(allLines, start, end, newLine, offset, newWidth);

                if (currentBlockIdx == anchor.blockIndex && anchor.offset >= offset && anchor.offset < offset + consumed) {
                    resRow = newLines.size();
                    resCol = anchor.offset - offset;
                }

                newLines.add(newLine);
                offset += consumed;

                if (offset >= logicSize && currentBlockIdx != anchor.blockIndex) break;
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


    private int copyFromOriginal(List<Line> allLines, int start, int end, Line target, int startOffset, int targetWidth) {
        int copiedInTarget = 0;
        int consumedFromSource = 0;

        while (copiedInTarget < targetWidth) {
            int globalOffset = startOffset + consumedFromSource;
            int lineInBlock = globalOffset / this.width;
            int colInLine = globalOffset % this.width;

            if (start + lineInBlock <= end) {
                Line source = allLines.get(start + lineInBlock);
                char c = source.getChar(colInLine);
                int attr = source.getAttributes(colInLine);

                if (WideCharUtil.isWide(c)) {
                    if (copiedInTarget >= targetWidth - 1) {
                        break;
                    }
                    target.setWide(copiedInTarget, c, attr);
                    copiedInTarget += 2;
                    consumedFromSource += 2;
                } else {
                    target.set(copiedInTarget, c, attr);
                    copiedInTarget++;
                    consumedFromSource++;
                }
            } else {
                break;
            }
        }
        return consumedFromSource == 0 && targetWidth > 0 ? 1 : consumedFromSource;
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