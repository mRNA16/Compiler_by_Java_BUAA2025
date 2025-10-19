package frontend.ast;

public class AssignmentStmt extends Stmt {
    private LVal lVal;
    private Exp exp;

    public AssignmentStmt() {
        super(SyntaxType.ASSIGN_STMT);
    }

    /**
     * 设置左值
     */
    public void setLVal(LVal lVal) {
        this.lVal = lVal;
        if (lVal != null) {
            addChild(lVal);
        }
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
     * 获取左值
     */
    public LVal getLVal() {
        return lVal;
    }

    /**
     * 获取表达式
     */
    public Exp getExp() {
        return exp;
    }
}
