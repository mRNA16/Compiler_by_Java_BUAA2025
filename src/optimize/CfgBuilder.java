package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.ctrl.BrCondInstr;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CfgBuilder extends Optimizer {
    @Override
    public void optimize() {
        // 清除之前生成的支配关系
        this.initFunction();
        // 构建CFG图
        this.buildCfg();
        // 构建支配关系
        this.buildDominateRelationship();
        // 构建直接支配关系
        this.buildDirectDominator();
        // 构建支配边界
        this.buildDominateFrontier();
    }

    private void initFunction() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            for (IrBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
                irBasicBlock.clearCfg();
            }
        }
    }

    private void buildCfg() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            for (IrBasicBlock visitBlock : irFunction.getBasicBlocks()) {
                for (Instr instr : visitBlock.getInstructions()) {
                    if (instr instanceof BrInstr brInstr) {
                        IrBasicBlock targetBlock = brInstr.getTargetBlock();
                        visitBlock.addNextBlock(targetBlock);
                        targetBlock.addBeforeBlock(visitBlock);
                    } else if (instr instanceof BrCondInstr brCondInstr) {
                        IrBasicBlock trueBlock = brCondInstr.getSucBlock();
                        IrBasicBlock falseBlock = brCondInstr.getFailBlock();
                        visitBlock.addNextBlock(trueBlock);
                        visitBlock.addNextBlock(falseBlock);
                        trueBlock.addBeforeBlock(visitBlock);
                        falseBlock.addBeforeBlock(visitBlock);
                    }
                }
            }
        }
    }

    private void buildDominateRelationship() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            List<IrBasicBlock> blockList = irFunction.getBasicBlocks();
            if (blockList.isEmpty())
                continue;

            for (IrBasicBlock deleteBlock : blockList) {
                HashSet<IrBasicBlock> visited = new HashSet<>();
                this.searchDfs(blockList.get(0), deleteBlock, visited);
                for (IrBasicBlock visitBlock : blockList) {
                    if (!visited.contains(visitBlock)) {
                        visitBlock.addDominator(deleteBlock);
                    }
                }
            }
        }
    }

    private void searchDfs(IrBasicBlock visitBlock, IrBasicBlock deleteBlock,
            HashSet<IrBasicBlock> visited) {
        if (visitBlock == deleteBlock) {
            return;
        }

        visited.add(visitBlock);
        for (IrBasicBlock nextBlock : visitBlock.getNextBlocks()) {
            if (!visited.contains(nextBlock) && nextBlock != deleteBlock) {
                this.searchDfs(nextBlock, deleteBlock, visited);
            }
        }
    }

    private void buildDirectDominator() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            for (IrBasicBlock visitBlock : irFunction.getBasicBlocks()) {
                for (IrBasicBlock dominator : visitBlock.getDominatorBlocks()) {
                    if (dominator == visitBlock)
                        continue;

                    HashSet<IrBasicBlock> sharedDominators = new HashSet<>(visitBlock.getDominatorBlocks());
                    sharedDominators.retainAll(dominator.getDominatorBlocks());

                    HashSet<IrBasicBlock> diffDominators = new HashSet<>(visitBlock.getDominatorBlocks());
                    diffDominators.removeAll(sharedDominators);

                    if (diffDominators.size() == 1 && diffDominators.contains(visitBlock)) {
                        visitBlock.setDirectDominator(dominator);
                        break;
                    }
                }
            }
        }
    }

    private void buildDominateFrontier() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            for (IrBasicBlock visitBlock : irFunction.getBasicBlocks()) {
                ArrayList<IrBasicBlock> nextBlocks = visitBlock.getNextBlocks();
                for (IrBasicBlock nextBlock : nextBlocks) {
                    IrBasicBlock currentBlock = visitBlock;
                    while (!nextBlock.getDominatorBlocks().contains(currentBlock) || currentBlock == nextBlock) {
                        currentBlock.addDominateFrontier(nextBlock);
                        currentBlock = currentBlock.getDirectDominator();
                        if (currentBlock == null) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
