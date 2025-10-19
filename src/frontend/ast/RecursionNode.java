package frontend.ast;

import frontend.lexer.Token;

import java.util.ArrayList;
import java.util.List;

public abstract class RecursionNode extends  AbstractASTNode{
    protected final List<ASTNode> operands = new ArrayList<>();
    protected final List<Token> operators = new ArrayList<>();

    public RecursionNode(SyntaxType syntaxType) {
        super(syntaxType);
    }

    /**
     * 添加操作数
     */
    public void addOperand(ASTNode operand) {
        operands.add(operand);
        if (operand != null) {
            addChild(operand);
        }
    }

    /**
     * 添加操作符
     */
    public void addOperator(Token op) {
        operators.add(op);
        if (op != null) {
            addChild(new TokenNode(op));
        }
    }

    public List<ASTNode> getOperands() {
        return operands;
    }

    public List<Token> getOperators() {
        return operators;
    }

    /** 返回左结合的AST结构 */
    public ASTNode buildLeftAssociativeTree() {
        // 只有一个操作数，无需变换
        if (operands.size() == 1) {
            return this;
        }

        // 从左到右逐步构造
        ASTNode left = operands.get(0);
        RecursionNode leftNode = createNewNode();
        leftNode.addChild(left);
        left = leftNode;
        for (int i = 0; i < operators.size(); i++) {
            Token op = operators.get(i);
            ASTNode right = operands.get(i + 1);

            // 新建一个当前类型的节点作为父节点
            RecursionNode newNode = createNewNode();
            newNode.addChild(left);
            newNode.addChild(new TokenNode(op));
            newNode.addChild(right);
            left = newNode;
        }
        return left;
    }

    /**
     * 创建当前节点类型的新实例
     */
    protected abstract RecursionNode createNewNode();
}
