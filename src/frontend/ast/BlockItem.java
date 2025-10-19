package frontend.ast;

/**
 * 块项节点 - 抽象基类
 * 可以是声明或语句
 */
public abstract class BlockItem extends AbstractASTNode {
    
    public BlockItem(SyntaxType syntaxType) {
        super(syntaxType);
    }
}