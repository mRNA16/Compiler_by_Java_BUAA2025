package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码块节点
 */
public class Block extends AbstractASTNode {
    private final List<BlockItem> blockItems;
    
    public Block() {
        super(SyntaxType.BLOCK);
        this.blockItems = new ArrayList<>();
    }
    
    /**
     * 添加块项
     */
    public void addBlockItem(BlockItem blockItem) {
        if (blockItem != null) {
            blockItems.add(blockItem);
            addChild(blockItem);
        }
    }
    
    /**
     * 获取所有块项
     */
    public List<BlockItem> getBlockItems() {
        return new ArrayList<>(blockItems);
    }
    
    /**
     * 获取块项数量
     */
    public int getBlockItemCount() {
        return blockItems.size();
    }
}