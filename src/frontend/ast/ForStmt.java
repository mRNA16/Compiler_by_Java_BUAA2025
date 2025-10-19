package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * for语句节点
 */
public class ForStmt extends Stmt {
    private final List<LVal> LvalList = new ArrayList<>();
    private final List<Exp> ExpList = new ArrayList<>();

    public ForStmt() {
        super(SyntaxType.FOR_STMT);
    }

    /**
     * 添加左值
     */
    public void addLVal(LVal lval) {
        LvalList.add(lval);
        if (lval != null) {
            addChild(lval);
        }
    }

    /**
     * 添加表达式
     */
    public void addExpr(Exp exp) {
        ExpList.add(exp);
        if (exp != null) {
            addChild(exp);
        }
    }

    /**
     * 获取左值列表
     */
    public List<LVal> getLvalList() {
        return LvalList;
    }

    /**
     * 获取表达式列表
     */
    public List<Exp> getExpList() {
        return ExpList;
    }
}
