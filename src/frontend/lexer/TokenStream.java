package frontend.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TokenStream {
    private final ArrayList<Token> tokens;
    private int index;
    private final Stack<Integer> prePeekStack;

    public TokenStream(ArrayList<Token> tokens) {
        this.tokens = tokens;
        this.index = 0;
        this.prePeekStack = new Stack<>();
    }

    public void next() {
        if(this.index>=tokens.size()) {
            return;
        }
        this.index++;
    }

    public void pushFlagIndex() {
        this.prePeekStack.push(index);
    }

    public int rollback() {
        this.index = this.prePeekStack.pop();
        return this.index;
    }

    public void pop() {
        this.prePeekStack.pop();
    }

    public int getLastTokenLineId() {
        if(this.index > 1)  return this.tokens.get(this.index-1).getLineId();
        else return -1;
    }

    // 调试方法
    public List<Token> getLast10Tokens() {
        ArrayList<Token> last10Tokens = new ArrayList<>();
        for(int i=6;i>=-10;i--) {
            last10Tokens.add(this.tokens.get(index-i));
        }
        return last10Tokens;
    }

    public Token peek(int peekStep) {
        if(this.index + peekStep >= this.tokens.size()) {
            // 这里读取到token流的末尾返回错误的Token
            return new Token(TokenType.EOF,"eof",-1);
        }
        // 窥视不入栈
        return this.tokens.get(this.index + peekStep);
    }
}
