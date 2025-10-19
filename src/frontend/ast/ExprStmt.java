package frontend.ast;

/**
 * 表达式语句节点
 */
public class ExprStmt extends Stmt {
    private Exp exp;
    
    public ExprStmt() {
        super(SyntaxType.EXPR_STMT);
    }
    
    /**
     * 设置表达式
     */
    public void setExp(Exp exp) {
        this.exp = exp;
        if (exp != null) {
            addChild(exp);
        }
    }
    
    /**
     * 获取表达式
     */
    public Exp getExp() {
        return exp;
    }
}