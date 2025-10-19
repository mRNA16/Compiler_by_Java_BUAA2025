package frontend.ast;

/**
 * 语句节点 - 抽象基类
 */
public abstract class Stmt extends AbstractASTNode {

    public Stmt(SyntaxType syntaxType) {
        super(syntaxType);
    }
}