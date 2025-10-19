package frontend.ast;

import frontend.lexer.Token;

/**
 * 标识符节点
 */
public class Ident extends AbstractASTNode {
    
    public Ident(Token token) {
        super(SyntaxType.IDENT, token);
    }
    
    /**
     * 获取标识符名称
     */
    public String getName() {
        return token != null ? token.getContent() : "";
    }
}