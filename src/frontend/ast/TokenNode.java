package frontend.ast;

import frontend.lexer.Token;

/**
 * Token节点 - 表示一个Token
 * 用于在AST中保留原始的Token信息
 */
public class TokenNode extends AbstractASTNode {
    
    public TokenNode(Token token) {
        super(SyntaxType.TOKEN_NODE, token);
    }
    
    /**
     * 获取Token的内容
     */
    public String getContent() {
        return token != null ? token.getContent() : "";
    }
    
    /**
     * 获取Token的类型
     */
    public frontend.lexer.TokenType getTokenType() {
        return token != null ? token.getType() : null;
    }
}