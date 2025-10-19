package frontend.ast;

import frontend.lexer.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 逻辑与表达式节点
 */
public class LAndExp extends RecursionNode {
    public LAndExp() {
        super(SyntaxType.LAND_EXP);
    }
    
    /**
     * 添加EqExp
     */
    public void addEqExp(EqExp eqExp) {
        addOperand(eqExp);
    }

    /**
     * 添加操作符
     */
    public void addOp(Token op) {
        addOperator(op);
    }
    
    /**
     * 获取eqExp列表
     */
    public List<ASTNode> getEqExpList() {
        return operands;
    }

    @Override
    protected RecursionNode createNewNode() {
        return new LAndExp();
    }
}