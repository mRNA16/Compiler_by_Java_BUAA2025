package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.MoveInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrGlobalValue;
import midend.llvm.constant.IrConstant;

import java.util.HashSet;
import java.util.List;

public class LivenessAnalysis extends Optimizer {
    @Override
    public void optimize() {
        for (IrFunction function : irModule.getFunctions()) {
            analyzeFunction(function);
        }
    }

    private void analyzeFunction(IrFunction function) {
        List<IrBasicBlock> blocks = function.getBasicBlocks();

        // 1. 初始化每个基本块的 defSet 和 useSet
        for (IrBasicBlock block : blocks) {
            computeDefAndUse(block);
        }

        // 2. 迭代计算 liveIn 和 liveOut
        boolean changed = true;
        while (changed) {
            changed = false;
            // 逆序遍历基本块通常收敛更快
            for (int i = blocks.size() - 1; i >= 0; i--) {
                IrBasicBlock block = blocks.get(i);

                // LiveOut[B] = Union(LiveIn[S]) for all successors S of B
                HashSet<IrValue> newLiveOut = new HashSet<>();
                for (IrBasicBlock next : block.getNextBlocks()) {
                    newLiveOut.addAll(next.getLiveIn());
                }
                block.getLiveOut().clear();
                block.getLiveOut().addAll(newLiveOut);

                // LiveIn[B] = Use[B] Union (LiveOut[B] - Def[B])
                HashSet<IrValue> newLiveIn = new HashSet<>(block.getLiveOut());
                newLiveIn.removeAll(block.getDefSet());
                newLiveIn.addAll(block.getUseSet());

                if (!newLiveIn.equals(block.getLiveIn())) {
                    block.getLiveIn().clear();
                    block.getLiveIn().addAll(newLiveIn);
                    changed = true;
                }
            }
        }
    }

    private void computeDefAndUse(IrBasicBlock block) {
        HashSet<IrValue> def = block.getDefSet();
        HashSet<IrValue> use = block.getUseSet();
        def.clear();
        use.clear();

        for (Instr instr : block.getInstructions()) {
            if (instr instanceof MoveInstr moveInstr) {
                // MoveInstr 特殊处理：src 是 use，dst 是 def
                IrValue src = moveInstr.getSrcValue();
                IrValue dst = moveInstr.getDstValue();

                if (src != null && isAllocatable(src)) {
                    if (!def.contains(src)) {
                        use.add(src);
                    }
                }
                if (dst != null && isAllocatable(dst)) {
                    def.add(dst);
                }
            } else {
                // 普通指令：操作数是 use，指令本身是 def
                for (IrValue operand : instr.getUseValueList()) {
                    if (operand != null && isAllocatable(operand)) {
                        if (!def.contains(operand)) {
                            use.add(operand);
                        }
                    }
                }
                if (isAllocatable(instr)) {
                    def.add(instr);
                }
            }
        }
    }

    public boolean isAllocatable(IrValue value) {
        if (value == null) {
            return false;
        }
        // 只有指令（有返回值）和参数需要分配寄存器
        // 常数、全局变量、基本块、函数等不需要
        if (value instanceof IrConstant || value instanceof IrGlobalValue ||
                value instanceof IrBasicBlock || value instanceof IrFunction) {
            return false;
        }
        // Void 类型的指令不需要寄存器
        if (value.getIrType().isVoidType()) {
            return false;
        }
        return true;
    }
}