package midend.llvm.instr.io;

import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;
import midend.llvm.constant.IrConstInt;
import midend.llvm.value.IrGlobalValue;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsSyscall;
import backend.mips.assembly.fake.MarsMove;
import backend.mips.assembly.MipsLsu;

public class PrintIntInstr extends IOInstr {
    public PrintIntInstr(IrValue printValue) {
        super(IrBaseType.VOID);
        this.addUseValue(printValue);
    }

    public IrValue getPrintValue() {
        return this.getUseValueList().get(0);
    }

    @Override
    public String toString() {
        IrValue val = getPrintValue();
        return "call void @putint(i32 " + val.getIrName() + ")";
    }

    @Override
    public void toMips() {
        super.toMips();
        java.util.List<Register> allocatedRegisterList = MipsBuilder.getAllocatedRegList();
        MipsBuilder.saveCurrent(allocatedRegisterList);

        // 使用 getUseValueList() 获取真正的打印值，以支持 MemToReg 优化后的值替换
        IrValue actualPrintValue = this.getUseValueList().get(0);
        Register rs = MipsBuilder.getValueToRegister(actualPrintValue);
        if (rs != null) {
            int index = allocatedRegisterList.indexOf(rs);
            if (index != -1) {
                // 如果该值在寄存器中，从保护区加载，避免被之前的参数覆盖
                new MipsLsu(MipsLsu.LsuType.LW, Register.A0, Register.SP,
                        MipsBuilder.getRegSaveOffset() - (index + 1) * 4);
            } else {
                new MarsMove(Register.A0, rs);
            }
        } else if (actualPrintValue instanceof IrConstInt constInt) {
            new MipsAlu(MipsAlu.AluType.ADDIU, Register.A0, Register.ZERO, constInt.getValue());
        } else if (actualPrintValue instanceof IrGlobalValue globalValue) {
            new MipsLsu(MipsLsu.LsuType.LW, Register.A0, globalValue.getMipsLabel());
        } else {
            Integer offset = MipsBuilder.getStackValueOffset(actualPrintValue);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.LW, Register.A0, Register.SP, offset);
            }
        }

        new MipsAlu(MipsAlu.AluType.ADDI, Register.V0, Register.ZERO, 1);
        new MipsSyscall();

        MipsBuilder.recoverCurrent(allocatedRegisterList);
    }
}
