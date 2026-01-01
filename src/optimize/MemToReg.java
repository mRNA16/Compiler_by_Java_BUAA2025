package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.memory.AllocateInstr;
import midend.llvm.type.IrArrayType;
import midend.llvm.type.IrPointerType;
import midend.llvm.type.IrType;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;

import java.util.ArrayList;

/**
 * MemToReg 优化器
 * 将栈上分配的标量变量提升到 SSA 寄存器形式
 * 通过插入 Phi 指令和重命名来消除 load/store 指令
 */
public class MemToReg extends Optimizer {

    @Override
    public void optimize() {
        for (IrFunction irFunction : irModule.getFunctions()) {
            IrBasicBlock entryBlock = irFunction.getEntryBlock();

            // 遍历函数中的所有基本块
            for (IrBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
                // 复制指令列表以避免并发修改
                ArrayList<Instr> instrList = new ArrayList<>(irBasicBlock.getInstructions());

                for (Instr instr : instrList) {
                    // 只对非数组类型的 alloca 进行 mem2reg 优化
                    if (this.isValueAllocate(instr)) {
                        InsertPhi insertPhi = new InsertPhi((AllocateInstr) instr, entryBlock);
                        insertPhi.addPhi();
                    }
                }
            }
        }
    }

    /**
     * 判断是否为可提升的 alloca 指令
     * 只有非数组类型才能进行 mem2reg 优化
     */
    private boolean isValueAllocate(Instr instr) {
        if (instr instanceof AllocateInstr allocateInstr) {
            IrType targetType = ((IrPointerType) allocateInstr.getIrType()).getTargetType();
            // 只对非数组类型添加 phi
            return !(targetType instanceof IrArrayType);
        }
        return false;
    }
}
