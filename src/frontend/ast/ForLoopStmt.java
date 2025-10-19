package frontend.ast;

/**
 * for循环节点
 */
public class ForLoopStmt extends Stmt {
    private ForStmt initStmt;
    private CondExp condition;
    private ForStmt updateStmt;
    private Stmt body;
    
    public ForLoopStmt() {
        super(SyntaxType.FOR_LOOP_STMT);
    }
    
    /**
     * 设置初始化语句
     */
    public void setInitStmt(ForStmt initStmt) {
        this.initStmt = initStmt;
        if (initStmt != null) {
            addChild(initStmt);
        }
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
     * 设置更新语句
     */
    public void setUpdateStmt(ForStmt updateStmt) {
        this.updateStmt = updateStmt;
        if (updateStmt != null) {
            addChild(updateStmt);
        }
    }
    
    /**
     * 设置循环体
     */
    public void setBody(Stmt body) {
        this.body = body;
        if (body != null) {
            addChild(body);
        }
    }
    
    /**
     * 获取初始化语句
     */
    public ForStmt getInitStmt() {
        return initStmt;
    }
    
    /**
     * 获取条件表达式
     */
    public CondExp getCondition() {
        return condition;
    }
    
    /**
     * 获取更新语句
     */
    public ForStmt getUpdateStmt() {
        return updateStmt;
    }
    
    /**
     * 获取循环体
     */
    public Stmt getBody() {
        return body;
    }
}