package frontend.ast;

import frontend.lexer.Token;
import java.util.ArrayList;
import java.util.List;

/**
 * AST节点的抽象基类
 * 提供了所有AST节点的通用实现
 */
public abstract class AbstractASTNode implements ASTNode {
    protected final SyntaxType syntaxType;
    protected final List<ASTNode> children;
    protected Token token;
    
    public AbstractASTNode(SyntaxType syntaxType) {
        this.syntaxType = syntaxType;
        this.children = new ArrayList<>();
        this.token = null;
    }
    
    public AbstractASTNode(SyntaxType syntaxType, Token token) {
        this.syntaxType = syntaxType;
        this.children = new ArrayList<>();
        this.token = token;
    }
    
    @Override
    public SyntaxType getSyntaxType() {
        return syntaxType;
    }
    
    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(children);
    }
    
    @Override
    public void addChild(ASTNode child) {
        if (child != null) {
            children.add(child);
        }
    }
    
    @Override
    public Token getToken() {
        return token;
    }
    
    @Override
    public void setToken(Token token) {
        this.token = token;
    }
    
    @Override
    public int getLineNumber() {
        return token != null ? token.getLineId() : -8899;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<ASTNode> children = getChildren();
        if(!children.isEmpty()) {
            for (ASTNode child : children) {
                sb.append(child.toString());
            }
        } else {
            if(token!=null) sb.append(token.info());
        }
        if(getToPrint()) sb.append("<").append(getSyntaxType()).append(">").append("\n");
        return sb.toString();
    }

    /**
     * 获取是否呈现语法成分
     */
    public boolean getToPrint(){
        return getSyntaxType().getToPrint();
    }

    /**
     * 获取子节点数量
     */
    public int getChildCount() {
        return children.size();
    }
    
    /**
     * 获取指定索引的子节点
     */
    public ASTNode getChild(int index) {
        if (index >= 0 && index < children.size()) {
            return children.get(index);
        }
        return null;
    }
    
    /**
     * 检查是否有子节点
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }
}