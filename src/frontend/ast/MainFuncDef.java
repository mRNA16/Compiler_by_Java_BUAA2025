package frontend.ast;

/**
 * 主函数定义节点
 */
public class MainFuncDef extends AbstractASTNode {
    private Block block;
    
    public MainFuncDef() {
        super(SyntaxType.MAIN_FUNC_DEF);
    }
    
    /**
     * 设置函数体
     */
    public void setBlock(Block block) {
        this.block = block;
        if (block != null) {
            addChild(block);
        }
    }
    
    /**
     * 获取函数体
     */
    public Block getBlock() {
        return block;
    }
}