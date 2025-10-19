package frontend.lexer;

public class Token {
    private final TokenType type;
    private final String content;
    private final int lineId;

    public Token(TokenType type, String content, int lineId) {
        this.type = type;
        this.content = content;
        this.lineId = lineId;
    }

    public TokenType getType() {
        return this.type;
    }

    public String getContent() {
        return this.content;
    }

    public int getLineId(){
        return this.lineId;
    }

    @Override
    public String toString() {
        return this.content;
    }

    public String info() {
        return this.type + " " + this.content + "\n"; 
    }
}
