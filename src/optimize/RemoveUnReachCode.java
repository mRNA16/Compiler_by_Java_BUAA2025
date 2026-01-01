package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.ctrl.BrCondInstr;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.instr.ctrl.ReturnInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;

import java.util.HashSet;
import java.util.Iterator;

public class RemoveUnReachCode extends Optimizer {
    @Override
    public void optimize() {
        // 删除多余的jump (基本块结束后的指令)
        this.removeUselessJump();
        // 删除不可达块
        this.removeUselessBlock();
    }

    private void removeUselessJump() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            for (IrBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
                boolean hasJump = false;
                Iterator<Instr> iterator = irBasicBlock.getInstructions().iterator();
                while (iterator.hasNext()) {
                    Instr instr = iterator.next();
                    if (hasJump) {
                        instr.removeAllValueUse();
                        iterator.remove();
                        continue;
                    }

                    if (instr instanceof BrInstr || instr instanceof BrCondInstr ||
                            instr instanceof ReturnInstr) {
                        hasJump = true;
                    }
                }
            }
        }
    }

    private void removeUselessBlock() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            if (irFunction.getBasicBlocks().isEmpty())
                continue;

            IrBasicBlock entryBlock = irFunction.getBasicBlocks().get(0);
            HashSet<IrBasicBlock> visited = new HashSet<>();
            // 使用dfs记录可达的block
            this.dfsBlock(entryBlock, visited);

            // 删除不可达块中的指令引用
            for (IrBasicBlock block : irFunction.getBasicBlocks()) {
                if (!visited.contains(block)) {
                    for (Instr instr : block.getInstructions()) {
                        instr.removeAllValueUse();
                    }
                }
            }

            // 移除不可达块
            irFunction.getBasicBlocks().removeIf(block -> !visited.contains(block));
        }
    }

    private void dfsBlock(IrBasicBlock block, HashSet<IrBasicBlock> visited) {
        if (visited.contains(block)) {
            return;
        }

        visited.add(block);
        Instr instr = block.getLastInstr();
        if (instr == null || instr instanceof ReturnInstr) {
            return;
        } else if (instr instanceof BrInstr brInstr) {
            IrBasicBlock targetBlock = brInstr.getTargetBlock();
            this.dfsBlock(targetBlock, visited);
        } else if (instr instanceof BrCondInstr brCondInstr) {
            IrBasicBlock trueBlock = brCondInstr.getSucBlock();
            IrBasicBlock falseBlock = brCondInstr.getFailBlock();
            this.dfsBlock(trueBlock, visited);
            this.dfsBlock(falseBlock, visited);
        }
    }
}
