package midend.llvm.instr.phi;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrValue;

import java.util.ArrayList;

/**
 * 并行复制指令类，用于消除 Phi 指令
 */
public class ParallelCopyInstr extends Instr {
    private final ArrayList<IrValue> srcList;
    private final ArrayList<IrValue> dstList;

    public ParallelCopyInstr(IrBasicBlock irBasicBlock) {
        super(IrBaseType.VOID, InstrType.PCOPY, "parallel-copy", false);
        this.srcList = new ArrayList<>();
        this.dstList = new ArrayList<>();
        this.setBlock(irBasicBlock);
    }

    public void addCopy(IrValue src, IrValue dst) {
        this.srcList.add(src);
        this.dstList.add(dst);
    }

    public ArrayList<IrValue> getSrcList() {
        return this.srcList;
    }

    public ArrayList<IrValue> getDstList() {
        return this.dstList;
    }

    @Override
    public String toString() {
        return "parallel-copy-instr";
    }

    @Override
    public void toMips() {
        throw new RuntimeException("ParallelCopy instruction should be removed before MIPS generation!");
    }
}
