package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.calc.CalculateInstr;
import midend.llvm.instr.comp.ICompInstr;
import midend.llvm.instr.convert.ZextInstr;
import midend.llvm.instr.io.IOInstr;
import midend.llvm.instr.memory.AllocateInstr;
import midend.llvm.instr.memory.GepInstr;
import midend.llvm.instr.memory.LoadInstr;
import midend.llvm.instr.memory.StoreInstr;
import midend.llvm.instr.ctrl.CallInstr;
import midend.llvm.instr.ctrl.ReturnInstr;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.instr.ctrl.BrCondInstr;
import midend.llvm.instr.phi.PhiInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;

import java.util.Iterator;

/**
 * 死代码删除优化器
 * 移除没有副作用且结果未被使用的指令
 */
public class DeadCodeElimination extends Optimizer {
    private boolean changed;

    @Override
    public void optimize() {
        changed = true;
        while (changed) {
            changed = false;
            for (IrFunction function : irModule.getFunctions()) {
                for (IrBasicBlock block : function.getBasicBlocks()) {
                    optimizeBlock(block);
                }
            }
        }
    }

    private void optimizeBlock(IrBasicBlock block) {
        Iterator<Instr> iterator = block.getInstructions().iterator();
        while (iterator.hasNext()) {
            Instr instr = iterator.next();

            if (isDead(instr)) {
                // 维护 Use 关系：该指令不再使用其操作数
                // 这可能会导致操作数的 beUsedList 变空，从而在下一轮被删除
                instr.removeAllValueUse();

                // 从基本块中移除
                iterator.remove();
                changed = true;
            }
        }
    }

    /**
     * 判断指令是否为死代码
     */
    private boolean isDead(Instr instr) {
        // 1. 如果指令的结果被使用了，则不是死代码
        if (!instr.getBeUsedList().isEmpty()) {
            return false;
        }

        // 2. 检查副作用：以下指令即使结果没被使用，也不能删除
        if (instr instanceof StoreInstr ||
                instr instanceof CallInstr ||
                instr instanceof ReturnInstr ||
                instr instanceof BrInstr ||
                instr instanceof BrCondInstr ||
                instr instanceof IOInstr) {
            return false;
        }

        // 3. 特殊处理：AllocateInstr 如果没有被任何 load/store 使用，也可以删除
        // 但通常 MemToReg 已经处理了标量 alloca，剩下的多是数组

        // 其余指令（Calculate, IComp, Zext, Load, Gep, Phi）
        // 如果结果没被使用且没有上述副作用，均可视为死代码
        return true;
    }
}
