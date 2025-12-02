package midend.visit;

import frontend.ast.Block;
import frontend.ast.BlockItem;
import frontend.ast.Decl;
import frontend.ast.Stmt;
import utils.ParserWrapper;

import java.util.List;

public class IrBlockVisitor {
    /*
     * 需要调用者管理是否进入子作用域
     */
    public static void visitBlock(Block block) {
        List<BlockItem> blockItems = block.getBlockItems();
        for (BlockItem blockItem : blockItems) {
            visitBlockItem(blockItem);
        }
    }

    public static void visitBlockItem(BlockItem blockItem) {
        if(blockItem instanceof ParserWrapper.BlockItemWrapper wrapper){
            if(wrapper.getWrappedNode() instanceof Decl decl){
                // false表局部作用域
                IrDeclVisitor.visitDecl(decl,false);
            } else if(wrapper.getWrappedNode() instanceof Stmt stmt){
                IrStmtVisitor.visitStmt(stmt);
            }
        }
    }
}
