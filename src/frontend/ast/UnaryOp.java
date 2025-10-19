package frontend.ast;

import frontend.lexer.Token;

public class UnaryOp extends AbstractASTNode{
    private Token op;

    public UnaryOp() {
        super(SyntaxType.UNARY_OP);
    }

    /**
     * 设置操作符
     */
    public void setOp(Token op) {
        this.op = op;
        if(op!=null){
            addChild(new TokenNode(op));
        }
    }

    /**
     * 获得操作符
     */
    public Token getOp() {
        return op;
    }
}
