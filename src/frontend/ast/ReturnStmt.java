package frontend.ast;

/**
 * return语句节点
 */
public class ReturnStmt extends Stmt {
    private Exp exp;
    
    public ReturnStmt() {
        super(SyntaxType.RETURN_STMT);
    }
    
    /**
     * 设置返回值表达式
     */
    public void setExp(Exp exp) {
        this.exp = exp;
        if (exp != null) {
            addChild(exp);
        }
    }
    
    /**
     * 获取返回值表达式
     */
    public Exp getExp() {
        return exp;
    }
    
    /**
     * 检查是否有返回值
     */
    public boolean hasReturnValue() {
        return exp != null;
    }

}