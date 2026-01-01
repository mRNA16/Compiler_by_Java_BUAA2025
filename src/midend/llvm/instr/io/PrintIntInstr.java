package midend.llvm.instr.io;

import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;
import midend.llvm.constant.IrConstInt;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsSyscall;
import backend.mips.assembly.fake.MarsMove;
import backend.mips.assembly.MipsLsu;

public class PrintIntInstr extends IOInstr {
    private final IrValue printValue;

    public PrintIntInstr(IrValue printValue) {
        super(IrBaseType.VOID);
        this.printValue = printValue;
        this.addUseValue(printValue);
    }

    public IrValue getPrintValue() {
        return this.getUseValueList().get(0);
    }

    @Override
    public String toString() {
        return "call void @putint(i32 " + printValue.getIrName() + ")";
    }

    @Override
    public void toMips() {
        super.toMips();
        // 使用 getUseValueList() 获取真正的打印值，以支持 MemToReg 优化后的值替换
        IrValue actualPrintValue = this.getUseValueList().get(0);
        Register rs = MipsBuilder.getValueToRegister(actualPrintValue);
        if (rs != null) {
            new MarsMove(Register.A0, rs);
        } else if (actualPrintValue instanceof IrConstInt constInt) {
            new MipsAlu(MipsAlu.AluType.ADDIU, Register.A0, Register.ZERO, constInt.getValue());
        } else {
            Integer offset = MipsBuilder.getStackValueOffset(actualPrintValue);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.LW, Register.A0, Register.SP, offset);
            }
        }

        new MipsAlu(MipsAlu.AluType.ADDI, Register.V0, Register.ZERO, 1);
        new MipsSyscall();
    }
}
