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

        // 1. 计算需要保护的寄存器
        java.util.HashSet<Register> registersToSave = new java.util.HashSet<>();
        java.util.HashSet<midend.llvm.value.IrValue> liveValues = this.getBlock().getLiveValuesAt(this);
        for (midend.llvm.value.IrValue val : liveValues) {
            Register reg = MipsBuilder.getValueToRegister(val);
            if (reg != null && MipsBuilder.isCallerSaved(reg)) {
                registersToSave.add(reg);
            }
        }

        // 2. 保护现场
        MipsBuilder.saveCurrent(allocatedRegisterList, registersToSave);

        // 3. 加载打印值到 A0
        IrValue actualPrintValue = this.getUseValueList().get(0);
        Register rs = MipsBuilder.getValueToRegister(actualPrintValue);
        if (rs != null) {
            Integer offset = MipsBuilder.getRegisterOffset(rs);
            if (offset != null && registersToSave.contains(rs)) {
                // 如果该值在寄存器中且被保护了，从保护区加载
                new MipsLsu(MipsLsu.LsuType.LW, Register.A0, Register.SP, offset);
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

        // 4. 恢复现场
        MipsBuilder.recoverCurrent(allocatedRegisterList, registersToSave);
    }
}
