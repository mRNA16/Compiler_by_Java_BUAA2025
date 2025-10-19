package frontend.ast;

import frontend.lexer.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 等值表达式节点
 */
public class EqExp extends RecursionNode {
    
    public EqExp() {
        super(SyntaxType.EQ_EXP);
    }
    
    /**
     * 添加左操作数
     */
    public void addRelExp(RelExp relExp) {
        addOperand(relExp);
    }
    
    /**
     * 添加操作符
     */
    public void addOp(Token op) {
        addOperator(op);
    }
    
    /**
     * 获取关系表达式列表
     */
    public List<ASTNode> getRelExpList() {
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
        return new EqExp();
    }
}