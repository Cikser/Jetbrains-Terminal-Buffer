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

    public void write(String text) {
        for (char c : text.toCharArray()) {
            if (c == '\r' || c == '\n') {
                cursor.handleChar(c);
                continue;
            }
            cursor.resolveWrap();

            boolean wide = WideCharUtil.isWide(c);

            if (wide && cursor.col() == width - 1) {
                screen.get(cursor.row()).set(cursor.col(), ' ', currentAttributes);
                cursor.advance();
                cursor.resolveWrap();
            }

            if (wide) {
                screen.get(cursor.row()).setWide(cursor.col(), c, currentAttributes);
                cursor.advanceForWideChar();
            } else {
                screen.get(cursor.row()).set(cursor.col(), c, currentAttributes);
                cursor.advance();
            }
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

    private void insertAndOverflow(String text, int[] attributes, boolean[] empty, Deque<LineContent> insertQueue){
        cursor.resolveWrap();
        int i = 0;
        int lastControl = -1;
        char[] chars = new char[text.length()];
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

    public void insert(String text) {
        cursor.resolveWrap();
        Deque<LineContent> insertQueue = new ArrayDeque<>();

        Cursor newCursor = calcFinalCursorPos(text);

        StringBuilder expandedText = new StringBuilder();
        List<Integer> expandedAttrs = new ArrayList<>();
        List<Boolean> expandedEmpty = new ArrayList<>();

        for (char c : text.toCharArray()) {
            expandedText.append(c);
            expandedAttrs.add(currentAttributes);
            expandedEmpty.add(false);
            if (WideCharUtil.isWide(c)) {
                expandedText.append(Line.WIDE_PLACEHOLDER);
                expandedAttrs.add(currentAttributes);
                expandedEmpty.add(false);
            }
        }

        String expanded = expandedText.toString();
        int[] attributes = new int[expandedAttrs.size()];
        for (int i = 0; i < expandedAttrs.size(); i++) {
            attributes[i] = expandedAttrs.get(i);
        }
        boolean[] empty = new boolean[expandedEmpty.size()];
        for (int i = 0; i < expandedEmpty.size(); i++) empty[i] = expandedEmpty.get(i);

        insertAndOverflow(expanded, attributes, empty, insertQueue);
        while (!insertQueue.isEmpty()) {
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

        // Optimizacija: Radimo reflow bez međulista StyledChar objekata
        ReflowResult result = performFastReflow(allLines, newWidth, anchor);

        this.width = newWidth;
        this.height = newHeight;
        rebuildBuffers(result.newLines, newHeight);

        restoreCursorPosition(result.newCursorRow, result.newCursorCol, result.newLines.size());
    }

    // Rekordi ostaju za čitljivost (kreiraju se samo jednom po resize-u, to je zanemarljivo)
    private record CursorAnchor(int blockIndex, int offset) {}
    private record ReflowResult(List<Line> newLines, int newCursorRow, int newCursorCol) {}

    /**
     * Glavna optimizacija: Prolazimo kroz linije i identifikujemo "paragrafe" (logičke blokove).
     * Umesto kopiranja u liste, direktno čitamo iz starih Line objekata u nove.
     */
    private ReflowResult performFastReflow(List<Line> allLines, int newWidth, CursorAnchor anchor) {
        List<Line> newLines = new ArrayList<>(allLines.size()); // Pre-allocate
        int resRow = -1, resCol = -1;
        int currentBlockIdx = 0;

        int i = 0;
        while (i < allLines.size()) {
            // 1. Identifikuj opseg paragrafa (od i do end)
            int start = i;
            int end = i;
            while (end + 1 < allLines.size() && allLines.get(end + 1).isWrapped()) {
                end++;
            }

            // 2. Izračunaj efektivnu dužinu paragrafa (bez trailing spaces)
            int effectiveLen = calculateEffectiveLength(allLines, start, end);

            // 3. Odredi koliko nam novih linija treba za ovaj pasus
            // Uključujemo i kursor ako je on unutar ovog bloka
            int logicSize = (currentBlockIdx == anchor.blockIndex)
                    ? Math.max(effectiveLen, anchor.offset + 1)
                    : effectiveLen;

            // 4. "Iseci" pasus u nove linije direktnim kopiranjem
            for (int offset = 0; offset < logicSize || (logicSize == 0 && offset == 0); offset += newWidth) {
                Line newLine = new Line(newWidth, currentAttributes);
                if (offset > 0) newLine.setWrapped(true);

                if (currentBlockIdx == anchor.blockIndex && anchor.offset >= offset && anchor.offset < offset + newWidth) {
                    resRow = newLines.size();
                    resCol = anchor.offset - offset;
                }

                copyFromOriginal(allLines, start, end, newLine, offset, newWidth);
                newLines.add(newLine);
            }

            currentBlockIdx++;
            i = end + 1; // Pređi na sledeći pasus
        }
        return new ReflowResult(newLines, resRow, resCol);
    }

    /**
     * Pronalazi poslednji ne-prazan karakter u celom logičkom bloku.
     * O(N) po pasusu, ali bez alokacije memorije.
     */
    private int calculateEffectiveLength(List<Line> lines, int start, int end) {
        for (int l = end; l >= start; l--) {
            Line line = lines.get(l);
            for (int c = this.width - 1; c >= 0; c--) {
                if (line.getChar(c) != ' ' || line.getAttributes(c) != currentAttributes) {
                    return (l - start) * this.width + (c + 1);
                }
            }
        }
        return 0;
    }

    /**
     * Direktno kopiranje iz više starih linija u jednu novu liniju.
     */
    private void copyFromOriginal(List<Line> allLines, int start, int end, Line target, int startOffset, int targetWidth) {
        for (int j = 0; j < targetWidth; j++) {
            int globalOffset = startOffset + j;
            int lineInBlock = globalOffset / this.width;
            int colInLine = globalOffset % this.width;

            if (start + lineInBlock <= end) {
                Line source = allLines.get(start + lineInBlock);
                target.set(j, source.getChar(colInLine), source.getAttributes(colInLine));
            } else {
                break; // Kraj pasusa
            }
        }
    }
    /*
    public void resize(int newWidth, int newHeight) {
        // 1. Priprema i sakupljanje podataka
        List<Line> allLines = collectLinesForReflow();

        // 2. Mapiranje kursora na logički nivo pre transformacije
        CursorAnchor anchor = calculateCursorAnchor(allLines);

        // 3. Grupisanje u logičke blokove (pasuse)
        List<List<StyledChar>> logicalBlocks = groupIntoLogicalBlocks(allLines);

        // 4. Transformacija: Reflow blokova u nove fizičke linije
        ReflowResult result = reflowAllBlocks(logicalBlocks, newWidth, anchor);

        // 5. Primena novih dimenzija i bafera
        this.width = newWidth;
        this.height = newHeight;
        rebuildBuffers(result.newLines, newHeight);

        // 6. Finalno pozicioniranje kursora
        restoreCursorPosition(result.newCursorRow, result.newCursorCol, result.newLines.size());
    }


    private record StyledChar(char c, int attr, boolean empty) {}
    private record CursorAnchor(int blockIndex, int offset) {}
    private record ReflowResult(List<Line> newLines, int newCursorRow, int newCursorCol) {}
*/
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
/*
    private List<List<StyledChar>> groupIntoLogicalBlocks(List<Line> lines) {
        List<List<StyledChar>> blocks = new ArrayList<>();
        List<StyledChar> currentBlock = new ArrayList<>();

        for (Line line : lines) {
            if (!line.isWrapped() && !currentBlock.isEmpty()) {
                blocks.add(new ArrayList<>(currentBlock));
                currentBlock.clear();
            }
            for (int col = 0; col < this.width; col++) {
                currentBlock.add(new StyledChar(line.getChar(col), line.getAttributes(col), line.isEmpty(col)));
            }
        }
        if (!currentBlock.isEmpty()) blocks.add(currentBlock);
        return blocks;
    }

    private ReflowResult reflowAllBlocks(List<List<StyledChar>> blocks, int newWidth, CursorAnchor anchor) {
        List<Line> newLines = new ArrayList<>();
        int resRow = -1, resCol = -1;

        for (int idx = 0; idx < blocks.size(); idx++) {
            List<StyledChar> block = blocks.get(idx);
            trimTrailingSpaces(block);

            // Određujemo koliko redova ovaj blok zauzima u novoj širini
            int iterations = Math.max(block.size(), (idx == anchor.blockIndex ? anchor.offset + 1 : 0));

            for (int i = 0; i < iterations; i += newWidth) {
                Line newLine = new Line(newWidth, currentAttributes);
                if (i > 0) newLine.setWrapped(true);

                // Provera kursora
                if (idx == anchor.blockIndex && anchor.offset >= i && anchor.offset < i + newWidth) {
                    resRow = newLines.size();
                    resCol = anchor.offset - i;
                }

                fillLineFromBlock(newLine, block, i, newWidth);
                newLines.add(newLine);
            }
        }
        return new ReflowResult(newLines, resRow, resCol);
    }

    private void fillLineFromBlock(Line targetLine, List<StyledChar> block, int startOffset, int width) {
        for (int j = 0; j < width; j++) {
            int blockIndex = startOffset + j;

            // Ako još uvek imamo podataka u bloku, kopiraj ih
            if (blockIndex < block.size()) {
                StyledChar sc = block.get(blockIndex);
                targetLine.set(j, sc.c(), sc.attr());
            } else {
                // Ako smo stigli do kraja bloka (npr. kursor je u "vazduhu"),
                // ostatak linije ostaje popunjen podrazumevanim vrednostima (praznine)
                // što je već definisano u konstruktoru Line klase.
                break;
            }
        }
    }*/

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
/*
    private void trimTrailingSpaces(List<StyledChar> block) {
        int lastNonSpace = -1;
        for (int i = 0; i < block.size(); i++) {
            if (block.get(i).c() != ' ') lastNonSpace = i;
        }
        if (lastNonSpace == -1) return;
        if (lastNonSpace < block.size() - 1) {
            block.subList(lastNonSpace + 1, block.size()).clear();
        }
    }*/

    private void rebuildBuffers(List<Line> newLines, int newHeight) {
        scrollback.clear();
        screen.clear();
        screen.resize(newHeight, false);

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