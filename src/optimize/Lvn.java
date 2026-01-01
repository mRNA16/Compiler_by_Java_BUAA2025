package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.calc.CalculateInstr;
import midend.llvm.instr.comp.ICompInstr;
import midend.llvm.instr.convert.ZextInstr;
import midend.llvm.instr.memory.GepInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * LVN (Local Value Numbering) 优化器
 * 用于实现基本块内的公共子表达式消除 (CSE)
 */
public class Lvn extends Optimizer {
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
        // 表达式 -> 产生该表达式结果的指令(Value)
        HashMap<ExpressionKey, IrValue> expressionMap = new HashMap<>();

        Iterator<Instr> iterator = block.getInstructions().iterator();
        while (iterator.hasNext()) {
            Instr instr = iterator.next();

            // 只有无副作用的指令才能进行 LVN
            if (isLvnCandidate(instr)) {
                ExpressionKey key = new ExpressionKey(instr);

                if (expressionMap.containsKey(key)) {
                    // 发现公共子表达式！
                    IrValue existingValue = expressionMap.get(key);

                    // 1. 维护 Use 关系：替换所有使用者
                    instr.modifyAllUsersToNewValue(existingValue);

                    // 2. 维护 Use 关系：该指令不再使用其操作数
                    instr.removeAllValueUse();

                    // 3. 移除重复指令
                    iterator.remove();
                    changed = true;
                } else {
                    // 记录新的表达式
                    expressionMap.put(key, instr);
                }
            }
        }
    }

    private boolean isLvnCandidate(Instr instr) {
        return instr instanceof CalculateInstr ||
                instr instanceof ICompInstr ||
                instr instanceof ZextInstr ||
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

            if (instr instanceof CalculateInstr calc) {
                this.op = calc.getCalculateType().name();
                // 可交换运算归一化
                if (isCommutative(calc.getCalculateType())) {
                    normalize();
                }
            } else if (instr instanceof ICompInstr comp) {
                this.op = comp.getCompType().name();
                // 可交换比较归一化 (EQ, NE)
                if (comp.getCompType() == ICompInstr.ICompType.EQ ||
                        comp.getCompType() == ICompInstr.ICompType.NE) {
                    normalize();
                }
            } else if (instr instanceof ZextInstr) {
                this.op = "ZEXT";
            } else if (instr instanceof GepInstr) {
                this.op = "GEP";
            } else {
                this.op = "UNKNOWN";
            }
        }

        private boolean isCommutative(CalculateInstr.CalculateType type) {
            return type == CalculateInstr.CalculateType.ADD ||
                    type == CalculateInstr.CalculateType.MUL ||
                    type == CalculateInstr.CalculateType.AND ||
                    type == CalculateInstr.CalculateType.OR;
        }

        private void normalize() {
            if (operands.size() == 2) {
                IrValue v1 = operands.get(0);
                IrValue v2 = operands.get(1);
                // 简单的基于名称或哈希的排序
                if (v1.getIrName().compareTo(v2.getIrName()) > 0) {
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
