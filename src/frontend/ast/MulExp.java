package frontend.ast;

import frontend.lexer.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 乘除表达式节点
 */
public class MulExp extends RecursionNode {
    
    public MulExp() {
        super(SyntaxType.MUL_EXP);
    }

    /**
     * 添加左操作数
     */
    public void addUnaryExp(UnaryExp unaryExp) {
        addOperand(unaryExp);
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
    public List<ASTNode> getUnaryExpList() {
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
        return new MulExp();
    }
}