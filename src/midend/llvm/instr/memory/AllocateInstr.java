package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrArrayType;
import midend.llvm.type.IrPointerType;
import midend.llvm.type.IrType;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsLsu;

public class AllocateInstr extends Instr {
    private final IrType targetType;

    public AllocateInstr(IrType targetType) {
        super(new IrPointerType(targetType), InstrType.ALLOCATE);
        this.targetType = targetType;
    }

    public IrType getTargetType() {
        return targetType;
    }

    @Override
    public String toString() {
        return irName + " = alloca " + targetType;
    }

    @Override
    public void toMips() {
        super.toMips();
        // 获取预分配的数据块偏移
        Integer dataOffset = MipsBuilder.getAllocaDataOffset(this);

        Register register = MipsBuilder.getValueToRegister(this);
        if (register != null) {
            new MipsAlu(MipsAlu.AluType.ADDIU, register, Register.SP, dataOffset);
        } else {
            new MipsAlu(MipsAlu.AluType.ADDIU, Register.K0, Register.SP, dataOffset);
            Integer pointerOffset = MipsBuilder.getStackValueOffset(this);
            if (pointerOffset != null) {
                new MipsLsu(MipsLsu.LsuType.SW, Register.K0, Register.SP, pointerOffset);
            }
        }
    }
}
