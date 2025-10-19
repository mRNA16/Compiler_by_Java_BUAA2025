package frontend.ast;

/**
 * 基本表达式节点
 */
public class PrimaryExp extends Exp {
    private ASTNode primary;
    
    public PrimaryExp() {
        super(SyntaxType.PRIMARY_EXP);
    }
    
    /**
     * 设置基本表达式内容
     */
    public void setPrimary(ASTNode primary) {
        this.primary = primary;
        if (primary != null) {
            addChild(primary);
        }
    }
    
    /**
     * 获取基本表达式内容
     */
    public ASTNode getPrimary() {
        return primary;
    }
}