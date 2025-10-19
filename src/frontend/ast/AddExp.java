package frontend.ast;

import frontend.lexer.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 加减表达式节点
 */
public class AddExp extends RecursionNode {
    
    public AddExp() {
        super(SyntaxType.ADD_EXP);
    }

    /**
     * 添加左操作数
     */
    public void addMulExp(MulExp mulExp) {
        addOperand(mulExp);
    }

    /**
     * 添加操作符
     */
    public void addOp(Token op) {
        addOperator(op);
    }

    /**
     * 获取乘法表达式列表
     */
    public List<ASTNode> getMulExpList() {
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
        return new AddExp();
    }
}