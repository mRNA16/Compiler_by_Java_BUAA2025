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
        if (this.targetType instanceof IrArrayType irArrayType) {
            MipsBuilder.allocateStackSpace(4 * irArrayType.getArraySize());
        } else {
            MipsBuilder.allocateStackSpace(4);
        }

        // 紧随其后创建指针
        int pointerOffset = MipsBuilder.getCurrentStackOffset();
        Register register = MipsBuilder.getValueToRegister(this);
        if (register != null) {
            new MipsAlu(MipsAlu.AluType.ADDI, register, Register.SP, pointerOffset);
        } else {
            new MipsAlu(MipsAlu.AluType.ADDI, Register.K0, Register.SP, pointerOffset);
            pointerOffset = MipsBuilder.allocateStackForValue(this);
            new MipsLsu(MipsLsu.LsuType.SW, Register.K0, Register.SP, pointerOffset);
        }
    }
}
