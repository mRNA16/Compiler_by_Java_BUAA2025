package midend.llvm.instr.ctrl;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;
import midend.llvm.constant.IrConstInt;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsLsu;
import backend.mips.assembly.MipsJump;
import backend.mips.assembly.fake.MarsMove;

public class ReturnInstr extends Instr {
    private IrValue returnValue;

    public ReturnInstr(IrValue returnValue) {
        super(IrBaseType.VOID, InstrType.RETURN, "return");
        this.returnValue = returnValue;
        this.addUseValue(returnValue);
    }

    public IrValue getReturnValue() {
        return returnValue;
    }

    @Override
    public String toString() {
        IrValue val = (this.getUseValueList().isEmpty()) ? null : this.getUseValueList().get(0);
        return "ret " + ((val != null) ? (val.getIrType().toString() + " " + val.getIrName())
                : "void");
    }

    @Override
    public void toMips() {
        super.toMips();
        // 使用 getUseValueList() 获取真正的返回值，以支持 MemToReg 优化后的值替换
        IrValue actualReturnValue = (this.getUseValueList().isEmpty()) ? null : this.getUseValueList().get(0);
        if (actualReturnValue != null) {
            Register returnRegister = MipsBuilder.getValueToRegister(actualReturnValue);
            if (returnRegister != null) {
                new MarsMove(Register.V0, returnRegister);
            } else if (actualReturnValue instanceof IrConstInt constInt) {
                new MipsAlu(MipsAlu.AluType.ADDIU, Register.V0, Register.ZERO, constInt.getValue());
            } else {
                Integer offset = MipsBuilder.getStackValueOffset(actualReturnValue);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, Register.V0, Register.SP, offset);
                }
            }
        }

        int frameSize = MipsBuilder.getFrameSize();
        if (frameSize > 0) {
            // 恢复 Callee-Saved 寄存器 (S0-S7)
            for (Register reg : MipsBuilder.getAllocatedRegList()) {
                if (MipsBuilder.isCalleeSaved(reg)) {
                    Integer offset = MipsBuilder.getRegisterOffset(reg);
                    if (offset != null) {
                        new MipsLsu(MipsLsu.LsuType.LW, reg, Register.SP, offset);
                    }
                }
            }
            // 恢复 RA 寄存器
            if (!MipsBuilder.getCurrentFunction().isLeafFunction()) {
                new MipsLsu(MipsLsu.LsuType.LW, Register.RA, Register.SP, MipsBuilder.getRaOffset());
            }
            // 恢复 SP 寄存器
            new MipsAlu(MipsAlu.AluType.ADDIU, Register.SP, Register.SP, frameSize);
        }

        new MipsJump(MipsJump.JumpType.JR, Register.RA);
    }
}
