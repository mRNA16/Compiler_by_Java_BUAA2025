package frontend.ast;

/**
 * break语句节点
 */
public class BreakStmt extends Stmt {
    
    public BreakStmt() {
        super(SyntaxType.BREAK_STMT);
    }
}