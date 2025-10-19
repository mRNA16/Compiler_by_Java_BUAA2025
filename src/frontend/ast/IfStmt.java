package frontend.ast;

/**
 * if语句节点
 */
public class IfStmt extends Stmt {
    private CondExp condition;
    private Stmt thenStmt;
    private Stmt elseStmt;
    
    public IfStmt() {
        super(SyntaxType.IF_STMT);
    }
    
    /**
     * 设置条件表达式
     */
    public void setCondition(CondExp condition) {
        this.condition = condition;
        if (condition != null) {
            addChild(condition);
        }
    }
    
    /**
     * 设置then语句
     */
    public void setThenStmt(Stmt thenStmt) {
        this.thenStmt = thenStmt;
        if (thenStmt != null) {
            addChild(thenStmt);
        }
    }
    
    /**
     * 设置else语句
     */
    public void setElseStmt(Stmt elseStmt) {
        this.elseStmt = elseStmt;
        if (elseStmt != null) {
            addChild(elseStmt);
        }
    }
    
    /**
     * 获取条件表达式
     */
    public CondExp getCondition() {
        return condition;
    }
    
    /**
     * 获取then语句
     */
    public Stmt getThenStmt() {
        return thenStmt;
    }
    
    /**
     * 获取else语句
     */
    public Stmt getElseStmt() {
        return elseStmt;
    }
    
    /**
     * 检查是否有else分支
     */
    public boolean hasElse() {
        return elseStmt != null;
    }
}