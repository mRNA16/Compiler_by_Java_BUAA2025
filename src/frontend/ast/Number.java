package frontend.ast;

import frontend.lexer.Token;

/**
 * 数字常量节点
 */
public class Number extends AbstractASTNode {
    
    public Number(Token token) {
        super(SyntaxType.NUMBER, token);
    }
    
    /**
     * 获取数字值
     */
    public int getValue() {
        if (token != null) {
            try {
                return Integer.parseInt(token.getContent());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * 获取数字的字符串表示
     */
    public String getValueString() {
        return token != null ? token.getContent() : "0";
    }
}