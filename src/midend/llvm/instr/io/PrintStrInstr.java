package midend.llvm.instr.io;

import midend.llvm.constant.IrConstString;
import midend.llvm.type.IrBaseType;
import midend.llvm.type.IrPointerType;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsSyscall;
import backend.mips.assembly.MipsLsu;

public class PrintStrInstr extends IOInstr {
    private final IrConstString irConstString;

    public PrintStrInstr(IrConstString irConstString) {
        super(IrBaseType.VOID);
        this.irConstString = irConstString;
        this.addUseValue(irConstString);
    }

    @Override
    public String toString() {
        IrPointerType irPointerType = (IrPointerType) this.irConstString.getIrType();
        return "call void @putstr(i8* getelementptr inbounds (" +
                irPointerType.getTargetType() + ", " +
                irPointerType + " " +
                this.irConstString.getIrName() + ", i64 0, i64 0))";
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

        new MipsLsu(MipsLsu.LsuType.LA, Register.A0, irConstString.getMipsLabel());
        new MipsAlu(MipsAlu.AluType.ADDI, Register.V0, Register.ZERO, 4);
        new MipsSyscall();

        // 3. 恢复现场
        MipsBuilder.recoverCurrent(allocatedRegisterList, registersToSave);
    }
}
