package midend.llvm.instr.ctrl;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrBasicBlock;

public class BrInstr extends Instr {
    private final IrBasicBlock targetBlock;

    public BrInstr(IrBasicBlock targetBlock) {
        super(IrBaseType.VOID, InstrType.JUMP, "br");
        this.targetBlock = targetBlock;
        this.addUseValue(targetBlock);
    }

    public BrInstr(IrBasicBlock targetBlock, IrBasicBlock sourceBlock) {
        super(IrBaseType.VOID, InstrType.JUMP, "br", false);
        this.setBlock(sourceBlock);
        this.targetBlock = targetBlock;
        this.addUseValue(targetBlock);
    }

    public IrBasicBlock getTargetBlock() {
        return targetBlock;
    }

    @Override
    public String toString() {
        return "br label %" + targetBlock.getIrName();
    }
}
