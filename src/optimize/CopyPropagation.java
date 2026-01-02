package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.MoveInstr;
import midend.llvm.instr.phi.PhiInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 复制传播 (Copy Propagation) 优化器
 * 在 SSA 形式下，如果 v2 = v1，则将所有对 v2 的引用替换为对 v1 的引用。
 * 主要处理：
 * 1. Phi 指令：如果所有操作数都相同，或者只有一个操作数。
 * 2. Move 指令：显式的值复制。
 */
public class CopyPropagation extends Optimizer {
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
            IrValue simplified = null;

            if (instr instanceof PhiInstr phi) {
                simplified = simplifyPhi(phi);
            } else if (instr instanceof MoveInstr move) {
                // 虽然 Move 通常在后期添加，但如果优化过程中产生了 Move，也可以传播
                simplified = move.getSrcValue();
            }

            if (simplified != null && simplified != instr) {
                // 发现可传播的副本！
                // 1. 替换所有使用者
                instr.modifyAllUsersToNewValue(simplified);
                // 2. 维护 Use 关系
                instr.removeAllValueUse();
                // 3. 移除该指令
                iterator.remove();
                changed = true;
            }
        }
    }

    /**
     * 简化 Phi 指令
     * 如果 Phi 指令的所有操作数都是同一个值 V，或者除了 V 之外只有该 Phi 指令本身（循环），
     * 则该 Phi 指令可以被替换为 V。
     */
    private IrValue simplifyPhi(PhiInstr phi) {
        IrValue commonValue = null;
        for (IrValue operand : phi.getUseValueList()) {
            if (operand == null || operand == phi) {
                continue;
            }
            if (commonValue == null) {
                commonValue = operand;
            } else if (!commonValue.equals(operand)) {
                return null; // 存在不同的操作数，不能简化
            }
        }
        return commonValue;
    }
}
