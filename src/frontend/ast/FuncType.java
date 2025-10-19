package frontend.ast;

import frontend.lexer.Token;
import frontend.lexer.TokenType;

/**
 * 函数类型节点
 */
public class FuncType extends AbstractASTNode {
    
    public FuncType(Token token) {
        super(SyntaxType.FUNC_TYPE, token);
    }
    
    /**
     * 获取函数类型
     */
    public frontend.lexer.TokenType getType() {
        return token != null ? token.getType() : null;
    }
    
    /**
     * 检查是否为void类型
     */
    public boolean isVoid() {
        return token != null && token.getType() == TokenType.VOIDTK;
    }
    
    /**
     * 检查是否为int类型
     */
    public boolean isInt() {
        return token != null && token.getType() == TokenType.INTTK;
    }
}