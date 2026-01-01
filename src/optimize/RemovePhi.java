package optimize;

import midend.llvm.IrBuilder;
import midend.llvm.constant.IrConstant;
import midend.llvm.instr.Instr;
import midend.llvm.instr.MoveInstr;
import midend.llvm.instr.ctrl.BrCondInstr;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.instr.phi.ParallelCopyInstr;
import midend.llvm.instr.phi.PhiInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * RemovePhi 优化器
 * 将 Phi 指令转换为 ParallelCopy 指令，再转换为 Move 指令
 */
public class RemovePhi extends Optimizer {

    @Override
    public void optimize() {
        this.convertPhiToParallelCopy();
        this.convertParallelCopyToMove();
    }

    /**
     * 将 Phi 指令转换为 ParallelCopy 指令
     */
    private void convertPhiToParallelCopy() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            ArrayList<IrBasicBlock> blockList = new ArrayList<>(irFunction.getBasicBlocks());
            for (IrBasicBlock irBasicBlock : blockList) {
                // 确保有 phi 指令
                Instr firstInstr = irBasicBlock.getFirstInstr();
                if (firstInstr == null || !(firstInstr instanceof PhiInstr)) {
                    continue;
                }

                ArrayList<ParallelCopyInstr> copyList = new ArrayList<>();
                // 遍历前驱基本块，将 copy 指令插入适当的位置
                // 1. 如果 before 只有一个后继，直接插入前驱块
                // 2. 如果 before 有多个后继，需要新建一个中间块
                for (IrBasicBlock beforeBlock : irBasicBlock.getBeforeBlocks()) {
                    ParallelCopyInstr copyInstr = beforeBlock.getNextBlocks().size() == 1
                            ? this.insertCopyDirect(beforeBlock)
                            : this.insertCopyToMiddle(beforeBlock, irBasicBlock);
                    copyList.add(copyInstr);
                }

                // 向 phi 的 copy 中填充相应值，可能有多个 phi
                Iterator<Instr> iterator = irBasicBlock.getInstructions().iterator();
                while (iterator.hasNext()) {
                    Instr instr = iterator.next();
                    if (instr instanceof PhiInstr phiInstr) {
                        // 遍历所有操作数，将其添加到对应前驱块的 copy 指令中
                        for (int i = 0; i < phiInstr.getUseValueList().size(); i++) {
                            IrValue useValue = phiInstr.getUseValueList().get(i);
                            // copyList 的顺序和 irBasicBlock.getBeforeBlocks() 一致
                            // phiInstr 的 beforeBlockList 顺序也和 irBasicBlock.getBeforeBlocks() 一致
                            copyList.get(i).addCopy(useValue, phiInstr);
                        }
                        iterator.remove();
                    }
                }
            }
        }
    }

    /**
     * 直接插入到前驱块
     */
    private ParallelCopyInstr insertCopyDirect(IrBasicBlock beforeBlock) {
        ParallelCopyInstr copyInstr = new ParallelCopyInstr(beforeBlock);
        beforeBlock.addInstrBeforeJump(copyInstr);
        return copyInstr;
    }

    /**
     * 创建新的中间块并插入
     */
    private ParallelCopyInstr insertCopyToMiddle(IrBasicBlock beforeBlock, IrBasicBlock nextBlock) {
        IrBasicBlock middleBlock = addMiddleBlock(beforeBlock, nextBlock);
        ParallelCopyInstr copyInstr = new ParallelCopyInstr(middleBlock);
        middleBlock.addInstrBeforeJump(copyInstr);
        return copyInstr;
    }

    /**
     * 在两个基本块之间添加中间块
     */
    private IrBasicBlock addMiddleBlock(IrBasicBlock beforeBlock, IrBasicBlock nextBlock) {
        // 获取中间基本块
        IrBasicBlock middleBlock = IrBuilder.getNewBasicBlockForRemovePhi(beforeBlock.getFunction());

        // 修改跳转关系
        Instr lastInstr = beforeBlock.getLastInstr();
        if (lastInstr instanceof BrInstr brInstr) {
            brInstr.setTargetBlock(middleBlock);
        } else if (lastInstr instanceof BrCondInstr brCondInstr) {
            if (brCondInstr.getSucBlock() == nextBlock) {
                brCondInstr.setSucBlock(middleBlock);
            } else if (brCondInstr.getFailBlock() == nextBlock) {
                brCondInstr.setFailBlock(middleBlock);
            }
        }

        // 给中间块创建跳转关系
        new midend.llvm.instr.ctrl.BrInstr(nextBlock, middleBlock);

        // 修改流图信息
        int nextIndex = beforeBlock.getNextBlocks().indexOf(nextBlock);
        if (nextIndex >= 0) {
            beforeBlock.getNextBlocks().set(nextIndex, middleBlock);
        }
        int beforeIndex = nextBlock.getBeforeBlocks().indexOf(beforeBlock);
        if (beforeIndex >= 0) {
            nextBlock.getBeforeBlocks().set(beforeIndex, middleBlock);
        }
        middleBlock.getBeforeBlocks().add(beforeBlock);
        middleBlock.getNextBlocks().add(nextBlock);

        return middleBlock;
    }

    /**
     * 将 ParallelCopy 指令转化为一系列 Move
     */
    private void convertParallelCopyToMove() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            for (IrBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
                if (irBasicBlock.hasParallelCopyInstr()) {
                    // 获取并移除 copy
                    ParallelCopyInstr copyInstr = irBasicBlock.getAndRemoveParallelCopyInstr();
                    // 转化为一系列 move
                    this.convertCopyToMove(copyInstr, irBasicBlock);
                }
            }
        }
    }

    /**
     * 将单个 ParallelCopy 转换为 Move 指令序列
     */
    private void convertCopyToMove(ParallelCopyInstr copyInstr, IrBasicBlock irBasicBlock) {
        ArrayList<IrValue> srcList = copyInstr.getSrcList();
        ArrayList<IrValue> dstList = copyInstr.getDstList();

        ArrayList<MoveInstr> moveList = new ArrayList<>();

        // 检测循环依赖并处理
        HashSet<IrValue> processedDst = new HashSet<>();

        for (int i = 0; i < dstList.size(); i++) {
            IrValue src = srcList.get(i);
            IrValue dst = dstList.get(i);

            // 检查是否存在循环依赖
            boolean hasCircle = false;
            for (int j = i + 1; j < srcList.size(); j++) {
                if (srcList.get(j) == dst && !processedDst.contains(dst)) {
                    hasCircle = true;
                    break;
                }
            }

            if (hasCircle) {
                // 创建临时变量来打破循环
                IrValue tmpValue = new IrValue(dst.getIrType(), dst.getIrName() + "_tmp");
                // 先保存 dst 到临时变量
                moveList.add(new MoveInstr(dst, tmpValue, irBasicBlock));
                // 替换后续 src 中对 dst 的引用
                for (int j = i + 1; j < srcList.size(); j++) {
                    if (srcList.get(j) == dst) {
                        srcList.set(j, tmpValue);
                    }
                }
            }

            moveList.add(new MoveInstr(src, dst, irBasicBlock));
            processedDst.add(dst);
        }

        // 在跳转前加入 move
        for (MoveInstr moveInstr : moveList) {
            irBasicBlock.addInstrBeforeJump(moveInstr);
        }
    }
}
