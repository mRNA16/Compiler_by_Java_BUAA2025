package midend.llvm.instr.ctrl;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrValue;
import midend.llvm.constant.IrConstInt;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsBranch;
import backend.mips.assembly.MipsJump;
import backend.mips.assembly.MipsLsu;
import backend.mips.assembly.MipsAlu;

public class BrCondInstr extends Instr {
    private final IrValue cond;
    private IrBasicBlock sucBlock;
    private IrBasicBlock failBlock;

    public BrCondInstr(IrValue cond, IrBasicBlock sucBlock, IrBasicBlock failBlock) {
        super(IrBaseType.VOID, InstrType.BRANCH, "branch");
        this.cond = cond;
        this.sucBlock = sucBlock;
        this.failBlock = failBlock;
        this.addUseValue(cond);
        this.addUseValue(sucBlock);
        this.addUseValue(failBlock);
    }

    public IrValue getCond() {
        return this.getUseValueList().get(0);
    }

    public IrBasicBlock getSucBlock() {
        return sucBlock;
    }

    public IrBasicBlock getFailBlock() {
        return failBlock;
    }

    public void setSucBlock(IrBasicBlock sucBlock) {
        this.sucBlock = sucBlock;
    }

    public void setFailBlock(IrBasicBlock failBlock) {
        this.failBlock = failBlock;
    }

    @Override
    public String toString() {
        return "br i1 " + cond.getIrName() + ", label %" + sucBlock.getIrName() + ", label %" + failBlock.getIrName();
    }

    @Override
    public void toMips() {
        super.toMips();
        // 从 useValueList 获取真正的条件值，以支持 MemToReg 优化后的值替换
        IrValue actualCond = this.getUseValueList().get(0);

        Register condReg = MipsBuilder.getValueToRegister(actualCond);
        if (condReg == null) {
            condReg = Register.K0;
            if (actualCond instanceof IrConstInt constInt) {
                new MipsAlu(MipsAlu.AluType.ADDIU, condReg, Register.ZERO, constInt.getValue());
            } else {
                Integer offset = MipsBuilder.getStackValueOffset(actualCond);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, condReg, Register.SP, offset);
                }
            }
        }

        new MipsBranch(MipsBranch.BranchType.BNE, condReg, Register.ZERO, sucBlock.getMipsLabel());
        new MipsJump(MipsJump.JumpType.J, failBlock.getMipsLabel());
    }
}
