package frontend.ast;

/**
 * 表达式节点 - 抽象基类
 */
public abstract class Exp extends AbstractASTNode {
    public Exp(SyntaxType syntaxType) {
        super(syntaxType);
    }
}