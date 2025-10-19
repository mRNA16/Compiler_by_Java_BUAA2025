package frontend.ast;

/**
 * 声明节点 - 抽象基类
 * 可以是常量声明或变量声明
 */
public abstract class Decl extends AbstractASTNode {
    
    public Decl(SyntaxType syntaxType) {
        super(syntaxType);
    }
}