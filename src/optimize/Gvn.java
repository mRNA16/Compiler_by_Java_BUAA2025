package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.calc.CalculateInstr;
import midend.llvm.instr.comp.ICompInstr;
import midend.llvm.instr.convert.ZextInstr;
import midend.llvm.instr.convert.TruncInstr;
import midend.llvm.instr.memory.GepInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;

import java.util.*;

/**
 * GVN (Global Value Numbering) 优化器
 * 扩展 LVN 到全局范围，利用支配树在整个函数范围内消除公共子表达式。
 * 在 SSA 形式下，支配节点中定义的表达式在被支配节点中依然有效。
 */
public class Gvn extends Optimizer {
    private boolean changed;
    private final Map<ExpressionKey, IrValue> globalExpressionMap = new HashMap<>();

    @Override
    public void optimize() {
        changed = true;
        while (changed) {
            changed = false;
            for (IrFunction function : irModule.getFunctions()) {
                if (function.getBasicBlocks().isEmpty())
                    continue;
                globalExpressionMap.clear();
                optimizeFunction(function);
            }
        }
    }

    private void optimizeFunction(IrFunction function) {
        IrBasicBlock entry = function.getEntryBlock();
        // 按照支配树进行深度优先搜索
        domTreeDFS(entry);
    }

    private void domTreeDFS(IrBasicBlock block) {
        // 记录进入当前块前，Map 中已有的状态，以便回溯
        List<ExpressionKey> addedKeys = new ArrayList<>();

        Iterator<Instr> iterator = block.getInstructions().iterator();
        while (iterator.hasNext()) {
            Instr instr = iterator.next();

            if (isGvnCandidate(instr)) {
                ExpressionKey key = new ExpressionKey(instr);
                if (globalExpressionMap.containsKey(key)) {
                    IrValue existingValue = globalExpressionMap.get(key);
                    instr.modifyAllUsersToNewValue(existingValue);
                    instr.removeAllValueUse();
                    iterator.remove();
                    changed = true;
                } else {
                    globalExpressionMap.put(key, instr);
                    addedKeys.add(key);
                }
            }
        }

        // 递归访问支配树中的子节点
        for (IrBasicBlock child : block.getDirectDominateBlocks()) {
            domTreeDFS(child);
        }

        // 回溯：移除当前块添加的表达式，恢复到父节点的状态
        for (ExpressionKey key : addedKeys) {
            globalExpressionMap.remove(key);
        }
    }

    private boolean isGvnCandidate(Instr instr) {
        return instr instanceof CalculateInstr ||
                instr instanceof ICompInstr ||
                instr instanceof ZextInstr ||
                instr instanceof TruncInstr ||
                instr instanceof GepInstr;
    }

    /**
     * 表达式键类，用于唯一标识一个计算过程
     */
    private static class ExpressionKey {
        private final String op;
        private final List<IrValue> operands;

        public ExpressionKey(Instr instr) {
            this.operands = new ArrayList<>(instr.getUseValueList());

            String opName = instr.getInstrType().name();
            if (instr instanceof CalculateInstr calc) {
                // 修复：必须包含具体的计算类型（ADD, SDIV 等）
                opName = calc.getCalculateType().name();
                if (isCommutative(calc.getCalculateType())) {
                    normalize();
                }
            } else if (instr instanceof ICompInstr comp) {
                // 修复：必须包含具体的比较类型（EQ, SLT 等）
                opName = comp.getCompType().name();
                if (comp.getCompType() == ICompInstr.ICompType.EQ ||
                        comp.getCompType() == ICompInstr.ICompType.NE) {
                    normalize();
                }
            } else if (instr instanceof GepInstr gep) {
                opName = "GEP_" + gep.getSourceType().toString();
            }

            // 包含操作名和结果类型，确保唯一性
            this.op = opName + "_" + instr.getIrType().toString();
        }

        private static boolean isCommutative(CalculateInstr.CalculateType type) {
            return type == CalculateInstr.CalculateType.ADD ||
                    type == CalculateInstr.CalculateType.MUL ||
                    type == CalculateInstr.CalculateType.AND ||
                    type == CalculateInstr.CalculateType.OR;
        }

        private void normalize() {
            if (operands.size() == 2) {
                IrValue v1 = operands.get(0);
                IrValue v2 = operands.get(1);
                String n1 = v1.getIrName() != null ? v1.getIrName() : "";
                String n2 = v2.getIrName() != null ? v2.getIrName() : "";
                if (n1.compareTo(n2) > 0) {
                    operands.set(0, v2);
                    operands.set(1, v1);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ExpressionKey that = (ExpressionKey) o;
            return Objects.equals(op, that.op) && Objects.equals(operands, that.operands);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, operands);
        }
    }
}
