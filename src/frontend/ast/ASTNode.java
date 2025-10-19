package frontend.ast;

import frontend.lexer.Token;
import java.util.List;

/**
 * AST节点的基础接口
 * 定义了所有AST节点必须实现的基本功能
 */
public interface ASTNode {

    /**
     * 获取是否呈现语法成分
     */
    boolean getToPrint();

    /**
     * 获取节点的语法类型
     */
    SyntaxType getSyntaxType();
    
    /**
     * 获取子节点列表
     */
    List<ASTNode> getChildren();
    
    /**
     * 添加子节点
     */
    void addChild(ASTNode child);
    
    /**
     * 获取节点对应的Token（如果有的话）
     */
    Token getToken();
    
    /**
     * 设置节点对应的Token
     */
    void setToken(Token token);
    
    /**
     * 获取节点在源码中的行号
     */
    int getLineNumber();
    
    /**
     * 获取节点的字符串表示（用于调试和输出）
     */
    String toString();
}