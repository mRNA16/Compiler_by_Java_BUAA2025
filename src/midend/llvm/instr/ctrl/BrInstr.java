package midend.llvm.instr.ctrl;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrBasicBlock;

import backend.mips.assembly.MipsJump;

public class BrInstr extends Instr {
    private IrBasicBlock targetBlock;

    public BrInstr(IrBasicBlock targetBlock) {
        super(IrBaseType.VOID, InstrType.JUMP, "br");
        this.targetBlock = targetBlock;
        this.addUseValue(targetBlock);
    }

    public BrInstr(IrBasicBlock targetBlock, IrBasicBlock sourceBlock) {
        super(IrBaseType.VOID, InstrType.JUMP, "br", false);
        this.setBlock(sourceBlock);
        sourceBlock.addInstruction(this);
        this.targetBlock = targetBlock;
        this.addUseValue(targetBlock);
    }

    public IrBasicBlock getTargetBlock() {
        return targetBlock;
    }

    public void setTargetBlock(IrBasicBlock targetBlock) {
        this.targetBlock = targetBlock;
    }

    @Override
    public String toString() {
        return "br label %" + targetBlock.getIrName();
    }

    @Override
    public void toMips() {
        super.toMips();
        new MipsJump(MipsJump.JumpType.J, targetBlock.getMipsLabel());
    }
}
