package terminalbuffer;

/**
 * Manages cursor position and movement within a terminal buffer.
 *
 * <p>The cursor tracks the current writing position and handles VT100-style
 * pending wrap semantics. When the cursor reaches the end of a line, it enters
 * a "pending wrap" state where it stays at the last column until the next
 * character is written, at which point it wraps to the beginning of the next line.
 *
 * <p>All cursor movements are automatically clamped to valid screen coordinates.
 * Moving beyond the buffer boundaries will position the cursor at the nearest
 * valid position.
 *
 * @see TerminalBuffer
 */
public class Cursor {
    private int row, col;
    private final TerminalBuffer owner;
    private boolean pendingWrap;

    Cursor(TerminalBuffer owner){
        this.owner = owner;
        row = 0;
        col = 0;
        pendingWrap = false;
    }

    /**
     * Sets the cursor to a specific position.
     *
     * <p>The position is automatically clamped to valid screen coordinates.
     * If the specified row or column is out of bounds, the cursor will be
     * positioned at the nearest valid location.
     *
     * <p>Setting the cursor position clears any pending wrap state.
     *
     * @param row target row (0-based, will be clamped to [0, height-1])
     * @param col target column (0-based, will be clamped to [0, width-1])
     */
    public void set(int row, int col){
        this.row = Math.max(0, Math.min(owner.height() - 1, row));
        this.col = Math.max(0, Math.min(owner.width() - 1, col));
        pendingWrap = col == owner.width();
    }

    /**
     * Returns the current row position.
     *
     * @return row index (0-based, 0 = top line)
     */
    public int row(){
        return row;
    }

    /**
     * Returns the current column position.
     *
     * @return column index (0-based, 0 = leftmost column)
     */
    public int col(){
        return col;
    }

    /**
     * Moves the cursor up by the specified number of lines.
     *
     * <p>The cursor will be clamped to the top of the screen (row 0) if
     * the movement would exceed the buffer boundaries. The column position
     * remains unchanged.
     *
     * <p>Moving the cursor clears any pending wrap state.
     *
     * @param n number of lines to move up (negative values move down)
     */
    public void up(int n){
        set(row - n, col);
    }

    /**
     * Moves the cursor down by the specified number of lines.
     *
     * <p>The cursor will be clamped to the bottom of the screen (row = height-1)
     * if the movement would exceed the buffer boundaries. The column position
     * remains unchanged.
     *
     * <p>If the cursor is already at the bottom and moves down further, the screen
     * does NOT automatically scroll. Use {@link TerminalBuffer#addEmptyLine()} to
     * explicitly scroll the screen.
     *
     * <p>Moving the cursor clears any pending wrap state.
     *
     * @param n number of lines to move down (negative values move up)
     */
    public void down(int n){
        set(row + n, col);
    }

    /**
     * Moves the cursor left by the specified number of columns.
     *
     * <p>The cursor will be clamped to the leftmost column (col 0) if the
     * movement would exceed the buffer boundaries. The row position remains
     * unchanged.
     *
     * <p>Moving the cursor clears any pending wrap state.
     *
     * @param n number of columns to move left (negative values move right)
     */
    public void left(int n){
        set(row, col - n);
    }

    /**
     * Moves the cursor right by the specified number of columns.
     *
     * <p>The cursor will be clamped to the rightmost column (col = width-1) if
     * the movement would exceed the buffer boundaries. The row position remains
     * unchanged.
     *
     * <p>Moving the cursor clears any pending wrap state.
     *
     * @param n number of columns to move right (negative values move left)
     */
    public void right(int n){
        set(row, col + n);
    }

    void advance(){
        if(col != owner.width() - 1)
            right(1);
        else {
            pendingWrap = true;
        }
    }

    void resolveWrap(){
        if(!pendingWrap)
            return;
        if(row == owner.height() - 1){
            owner.scroll();
        }
        down(1);
        set(row, 0);
        owner.setWrapped(row);
    }

    void advanceDown() {
        if(row >= owner.height() - 1)
            owner.scroll();
        down(1);
    }

    void handleChar(char c){
        switch (c){
            case '\n':{
                advanceDown();
                set(row, 0);
                break;
            }
            case '\r':{
                set(row, 0);
                break;
            }
            default: break;
        }
    }

    void advanceForWideChar() {
        if (col + 2 < owner.width()) {
            right(2);
        } else {
            col = owner.width() - 1;
            pendingWrap = true;
        }
    }
}