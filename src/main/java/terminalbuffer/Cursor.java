package terminalbuffer;

public class Cursor {
    private int row, col;
    private final TerminalBuffer owner;

    Cursor(TerminalBuffer owner){
        this.owner = owner;
        row = 0;
        col = 0;
    }

    void set(int row, int col){
        this.row = Math.max(0, Math.min(owner.height() - 1, row));
        this.col = Math.max(0, Math.min(owner.width() - 1, col));
    }

    int row(){
        return row;
    }

    int col(){
        return col;
    }

    void up(int n){
        set(row - n, col);
    }

    void down(int n){
        set(row + n, col);
    }

    void left(int n){
        set(row, col - n);
    }

    void right(int n){
        set(row, col + n);
    }

    void advance(){
        if(col != owner.width() - 1)
            right(1);
        else if(row != owner.height() - 1){
            set(row, 0);
            down(1);
        }
        else{
            set(row, 0);
        }
    }

}
