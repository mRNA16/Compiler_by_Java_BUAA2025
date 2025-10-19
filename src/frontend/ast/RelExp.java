package frontend.ast;

import frontend.lexer.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 关系表达式节点
 */
public class RelExp extends RecursionNode {
    public RelExp() {
        super(SyntaxType.REL_EXP);
    }

    /**
     * 添加左操作数
     */
    public void addAddExp(AddExp addExp) {
        addOperand(addExp);
    }

    /**
     * 添加操作符
     */
    public void addOp(Token op) {
        addOperator(op);
    }

    /**
     * 获取加法表达式列表
     */
    public List<ASTNode> getAddExpList() {
        return operands;
    }

    /**
     * 获取操作符列表
     */
    public List<Token> getOp() {
        return operators;
    }

    @Override
    protected RecursionNode createNewNode() {
        return new RelExp();
    }
}