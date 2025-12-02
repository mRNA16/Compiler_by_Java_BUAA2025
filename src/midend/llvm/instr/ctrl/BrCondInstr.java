package midend.llvm.instr.ctrl;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrValue;

public class BrCondInstr extends Instr {
    private final IrValue cond;
    private final IrBasicBlock sucBlock;
    private final IrBasicBlock failBlock;

    public BrCondInstr(IrValue cond, IrBasicBlock sucBlock, IrBasicBlock failBlock) {
        super(IrBaseType.VOID, InstrType.BRANCH,"branch");
        this.cond = cond;
        this.sucBlock = sucBlock;
        this.failBlock = failBlock;
        this.addUseValue(cond);
        this.addUseValue(sucBlock);
        this.addUseValue(failBlock);
    }

    public IrValue getCond() {
        return cond;
    }

    public IrBasicBlock getSucBlock() {
        return sucBlock;
    }

    public IrBasicBlock getFailBlock() {
        return failBlock;
    }

    @Override
    public String toString() {
        return "br i1 " + cond.getIrName() + ", label %" + sucBlock.getIrName() + ", label %" + failBlock.getIrName();
    }
}
