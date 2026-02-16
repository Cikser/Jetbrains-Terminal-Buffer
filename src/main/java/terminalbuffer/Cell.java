package terminalbuffer;

public record Cell(
        Character character,
        int attributes
) {
    static Cell create(TerminalBuffer owner){
        return new Cell(' ', owner.currentAttributes());
    }
}
