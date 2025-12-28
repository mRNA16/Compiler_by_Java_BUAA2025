package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrPointerType;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrGlobalValue;
import midend.llvm.instr.memory.AllocateInstr;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsLsu;

public class LoadInstr extends Instr {
    private final IrValue pointer;

    public LoadInstr(IrValue pointer) {
        super(((IrPointerType) pointer.getIrType()).getTargetType(), InstrType.LOAD);
        this.pointer = pointer;
        this.addUseValue(pointer);
    }

    @Override
    public String toString() {
        return irName + " = load " + irType + ", " + pointer.getIrType() + " " + pointer.getIrName();
    }

    @Override
    public void toMips() {
        super.toMips();
        Register rd = MipsBuilder.allocateStackForValue(this) == null ? MipsBuilder.getValueToRegister(this)
                : Register.K0;

        if (pointer instanceof IrGlobalValue) {
            new MipsLsu(MipsLsu.LsuType.LW, rd, pointer.getMipsLabel());
        } else if (pointer instanceof AllocateInstr) {
            Integer offset = MipsBuilder.getStackValueOffset(pointer);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.LW, rd, Register.SP, offset);
            }
        } else {
            Register base = MipsBuilder.getValueToRegister(pointer);
            if (base != null) {
                new MipsLsu(MipsLsu.LsuType.LW, rd, base, 0);
            } else {
                Register temp = Register.K1;
                Integer offset = MipsBuilder.getStackValueOffset(pointer);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, temp, Register.SP, offset);
                    new MipsLsu(MipsLsu.LsuType.LW, rd, temp, 0);
                }
            }
        }

        if (rd == Register.K0) {
            Integer offset = MipsBuilder.getStackValueOffset(this);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.SW, Register.K0, Register.SP, offset);
            }
        }
    }
}
