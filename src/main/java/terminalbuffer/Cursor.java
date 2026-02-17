package terminalbuffer;

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

    public void set(int row, int col){
        this.row = Math.max(0, Math.min(owner.height() - 1, row));
        this.col = Math.max(0, Math.min(owner.width() - 1, col));
        pendingWrap = col == owner.width();
    }

    public int row(){
        return row;
    }

    public int col(){
        return col;
    }

    public void up(int n){
        set(row - n, col);
    }

    public void down(int n){
        set(row + n, col);
    }

    public void left(int n){
        set(row, col - n);
    }

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

    void advanceWide() {
        if (col + 2 < owner.width()) {
            right(2);
        } else {
            col = owner.width() - 1;
            pendingWrap = true;
        }
    }
}
