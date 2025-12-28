package midend.llvm.instr.ctrl;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;
import midend.llvm.constant.IrConstInt;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsJump;
import backend.mips.assembly.fake.MarsMove;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsLsu;

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
        return "ret " + ((returnValue != null) ? (returnValue.getIrType().toString() + " " + returnValue.getIrName())
                : "void");
    }

    @Override
    public void toMips() {
        super.toMips();
        if (returnValue != null) {
            Register returnRegister = MipsBuilder.getValueToRegister(returnValue);
            if (returnRegister != null) {
                new MarsMove(Register.V0, returnRegister);
            } else if (returnValue instanceof IrConstInt constInt) {
                new MipsAlu(MipsAlu.AluType.ADDIU, Register.V0, Register.ZERO, constInt.getValue());
            } else {
                Integer offset = MipsBuilder.getStackValueOffset(returnValue);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, Register.V0, Register.SP, offset);
                }
            }
        }
        new MipsJump(MipsJump.JumpType.JR, Register.RA);
    }
}
