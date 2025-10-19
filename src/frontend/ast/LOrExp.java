package frontend.ast;

import frontend.lexer.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 逻辑或表达式节点
 */
public class LOrExp extends RecursionNode {
    public LOrExp() {
        super(SyntaxType.LOR_EXP);
    }
    
    /**
     * 添加LAndExp
     */
    public void addLAndExp(LAndExp lAndExp) {
        addOperand(lAndExp);
    }


    /**
     * 添加操作符
     */
    public void addOp(Token op) {
        addOperator(op);
    }

    /**
     * 获取lAndExpList
     */
    public List<ASTNode> getLAndExpList() {
        return operands;
    }

    @Override
    protected RecursionNode createNewNode() {
        return new LOrExp();
    }
}