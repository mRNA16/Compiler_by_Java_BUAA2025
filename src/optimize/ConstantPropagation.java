package optimize;

import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.Instr;
import midend.llvm.instr.calc.CalculateInstr;
import midend.llvm.instr.comp.ICompInstr;
import midend.llvm.instr.convert.ZextInstr;
import midend.llvm.instr.phi.PhiInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;

import java.util.Iterator;

/**
 * 常量传播优化器
 * 在 SSA 形式下，识别结果为常量的指令并进行替换
 */
public class ConstantPropagation extends Optimizer {
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
            IrValue constant = null;

            if (instr instanceof CalculateInstr calc) {
                constant = foldCalculate(calc);
            } else if (instr instanceof ICompInstr comp) {
                constant = foldCompare(comp);
            } else if (instr instanceof ZextInstr zext) {
                constant = foldZext(zext);
            } else if (instr instanceof PhiInstr phi) {
                constant = foldPhi(phi);
            }

            if (constant != null) {
                // 1. 维护 Use 关系：将所有使用者指向新常量
                // modifyAllUsersToNewValue 会自动处理 beUsedList 的更新
                instr.modifyAllUsersToNewValue(constant);
                
                // 2. 维护 Use 关系：该指令不再使用其操作数
                instr.removeAllValueUse();
                
                // 3. 从基本块中移除该指令
                iterator.remove();
                
                changed = true;
            }
        }
    }

    private IrValue foldCalculate(CalculateInstr calc) {
        IrValue l = calc.getL();
        IrValue r = calc.getR();
        if (l instanceof IrConstInt cl && r instanceof IrConstInt cr) {
            int v1 = cl.getValue();
            int v2 = cr.getValue();
            return switch (calc.getCalculateType()) {
                case ADD -> new IrConstInt(v1 + v2);
                case SUB -> new IrConstInt(v1 - v2);
                case MUL -> new IrConstInt(v1 * v2);
                case SDIV -> v2 != 0 ? new IrConstInt(v1 / v2) : null;
                case SREM -> v2 != 0 ? new IrConstInt(v1 % v2) : null;
                case AND -> new IrConstInt(v1 & v2);
                case OR -> new IrConstInt(v1 | v2);
            };
        }
        return null;
    }

    private IrValue foldCompare(ICompInstr comp) {
        IrValue l = comp.getL();
        IrValue r = comp.getR();
        if (l instanceof IrConstInt cl && r instanceof IrConstInt cr) {
            int v1 = cl.getValue();
            int v2 = cr.getValue();
            boolean res = switch (comp.getCompType()) {
                case EQ -> v1 == v2;
                case NE -> v1 != v2;
                case SGT -> v1 > v2;
                case SGE -> v1 >= v2;
                case SLT -> v1 < v2;
                case SLE -> v1 <= v2;
            };
            return new IrConstInt(res ? 1 : 0);
        }
        return null;
    }

    private IrValue foldZext(ZextInstr zext) {
        if (zext.getOriginValue() instanceof IrConstInt constInt) {
            return new IrConstInt(constInt.getValue());
        }
        return null;
    }

    /**
     * 如果 Phi 指令的所有输入都是同一个值，则可以折叠
     */
    private IrValue foldPhi(PhiInstr phi) {
        IrValue firstValue = null;
        for (IrValue value : phi.getUseValueList()) {
            if (value == null || value == phi) continue;
            if (firstValue == null) {
                firstValue = value;
            } else if (!firstValue.equals(value)) {
                return null;
            }
        }
        return firstValue;
    }
}
