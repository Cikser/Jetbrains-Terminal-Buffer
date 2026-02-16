package terminalbuffer;

import java.util.ArrayList;

public class Line {
    private final TerminalBuffer owner;
    private final ArrayList<Cell> list;

    Line(TerminalBuffer owner){
        this.owner = owner;
        list = new ArrayList<>();

        for(int i = 0; i < owner.width(); i++){
            list.add(Cell.create(owner));
        }
    }

    Cell get(int i){
        return list.get(i);
    }

    void set(int i, Cell cell){
        list.set(i, cell);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Cell c : list){
            sb.append(c.character());
        }
        return sb.toString();
    }
}
