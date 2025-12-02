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

    public void reverseOperand(){
        List<ASTNode> newOperands = new ArrayList<>();
        for(int i = operands.size()-1;i>=0;i--){
            newOperands.add(operands.get(i));
        }
        operands.clear();
        operands.addAll(newOperands);
    }

    public void reverseOperator(){
        List<Token> newOperators = new ArrayList<>();
        for(int i = operators.size()-1;i>=0;i--){
            newOperators.add(operators.get(i));
        }
        operators.clear();
        operators.addAll(newOperators);
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
            newNode.addOperand(left);
            newNode.addOperator(op);
            newNode.addOperand(right);
            left = newNode;
        }
        return left;
    }

    public RecursionNode flattenTree(RecursionNode newNode) {
        if (newNode == null) {
           newNode = createNewNode();
        }

        int childCount = this.getChildCount();

        if (childCount == 3) {
            ((RecursionNode)this.getChild(0)).flattenTree(newNode);
            ASTNode opNode = this.getChild(1);
            newNode.addOperator(opNode.getToken());
            newNode.addOperand(this.getChild(2));
        } else if (childCount == 1) {
            newNode.addOperand(this.getChild(0));
        } else {
            throw new RuntimeException("Wrong number of children of the RecursionNode");
        }
        // newNode.reverseOperand();
        // newNode.reverseOperator();
        return newNode;
    }

    /**
     * 创建当前节点类型的新实例
     */
    protected abstract RecursionNode createNewNode();
}
