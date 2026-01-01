package optimize;

import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.Instr;
import midend.llvm.instr.calc.CalculateInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;

import java.util.Iterator;

/**
 * 强度削弱与代数化简优化器
 * 处理 x*0, x*1, x/1, x%1 等代数恒等式
 */
public class StrengthReduction extends Optimizer {
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
            if (!(instr instanceof CalculateInstr calc))
                continue;

            IrValue simplified = null;
            IrValue l = calc.getL();
            IrValue r = calc.getR();

            switch (calc.getCalculateType()) {
                case MUL -> simplified = simplifyMul(l, r);
                case SDIV -> simplified = simplifyDiv(l, r);
                case SREM -> simplified = simplifyRem(l, r);
                case ADD -> simplified = simplifyAdd(l, r);
                case SUB -> simplified = simplifySub(l, r);
            }

            if (simplified != null) {
                instr.modifyAllUsersToNewValue(simplified);
                instr.removeAllValueUse();
                iterator.remove();
                changed = true;
            }
        }
    }

    private IrValue simplifyMul(IrValue l, IrValue r) {
        if (l instanceof IrConstInt cl && cl.getValue() == 0)
            return cl;
        if (r instanceof IrConstInt cr && cr.getValue() == 0)
            return cr;
        if (l instanceof IrConstInt cl && cl.getValue() == 1)
            return r;
        if (r instanceof IrConstInt cr && cr.getValue() == 1)
            return l;
        return null;
    }

    private IrValue simplifyDiv(IrValue l, IrValue r) {
        if (r instanceof IrConstInt cr && cr.getValue() == 1)
            return l;
        if (l.equals(r))
            return new IrConstInt(1);
        return null;
    }

    private IrValue simplifyRem(IrValue l, IrValue r) {
        if (r instanceof IrConstInt cr && cr.getValue() == 1)
            return new IrConstInt(0);
        if (l.equals(r))
            return new IrConstInt(0);
        return null;
    }

    private IrValue simplifyAdd(IrValue l, IrValue r) {
        if (l instanceof IrConstInt cl && cl.getValue() == 0)
            return r;
        if (r instanceof IrConstInt cr && cr.getValue() == 0)
            return l;
        return null;
    }

    private IrValue simplifySub(IrValue l, IrValue r) {
        if (r instanceof IrConstInt cr && cr.getValue() == 0)
            return l;
        if (l.equals(r))
            return new IrConstInt(0);
        return null;
    }
}
