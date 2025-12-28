package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrArrayType;
import midend.llvm.type.IrPointerType;
import midend.llvm.type.IrType;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrGlobalValue;
import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.memory.AllocateInstr;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsLsu;
import backend.mips.assembly.MipsMdu;

public class GepInstr extends Instr {
    private final IrType sourceType;
    private final IrValue pointer;
    private final IrValue offset;

    public GepInstr(IrValue pointer, IrValue offset) {
        super(new IrPointerType(getSourceType(pointer)), InstrType.GEP);
        this.pointer = pointer;
        this.addUseValue(pointer);
        this.offset = offset;
        this.addUseValue(offset);
        this.sourceType = getSourceType(pointer);
    }

    public IrType getSourceType() {
        return sourceType;
    }

    public IrValue getPointer() {
        return pointer;
    }

    public IrValue getOffset() {
        return offset;
    }

    public static IrType getSourceType(IrValue pointer) {
        IrType sourceType = ((IrPointerType) pointer.getIrType()).getTargetType();
        if (sourceType.isArrayType()) {
            return ((IrArrayType) sourceType).getElementType();
        } else if (sourceType.isPointerType()) {
            return ((IrPointerType) sourceType).getTargetType();
        } else {
            return sourceType;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(irName).append(" =  getelementptr inbounds ");
        IrType pTargetType = ((IrPointerType) pointer.getIrType()).getTargetType();
        sb.append(pTargetType).append(", ").append(pointer.getIrType()).append(" ").append(pointer.getIrName())
                .append(", ");
        if (pTargetType.isArrayType())
            sb.append("i32 0, ");
        sb.append(offset.getIrType()).append(" ").append(offset.getIrName());
        return sb.toString();
    }

    @Override
    public void toMips() {
        super.toMips();
        Register rd = MipsBuilder.allocateStackForValue(this) == null ? MipsBuilder.getValueToRegister(this)
                : Register.K0;

        // Calculate offset in bytes: offset * 4
        Register offReg = Register.K1;
        if (offset instanceof IrConstInt constInt) {
            new MipsAlu(MipsAlu.AluType.ADDIU, offReg, Register.ZERO, constInt.getValue() * 4);
        } else {
            Register temp = MipsBuilder.getValueToRegister(offset);
            if (temp == null) {
                temp = Register.K1;
                Integer stackOff = MipsBuilder.getStackValueOffset(offset);
                if (stackOff != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, temp, Register.SP, stackOff);
                }
            }
            new MipsAlu(MipsAlu.AluType.SLL, offReg, temp, 2); // * 4
        }

        // Calculate base + offset
        if (pointer instanceof IrGlobalValue) {
            new MipsLsu(MipsLsu.LsuType.LA, rd, pointer.getMipsLabel());
            new MipsAlu(MipsAlu.AluType.ADDU, rd, rd, offReg);
        } else if (pointer instanceof AllocateInstr) {
            Integer baseOffset = MipsBuilder.getStackValueOffset(pointer);
            if (baseOffset != null) {
                new MipsAlu(MipsAlu.AluType.ADDIU, rd, Register.SP, baseOffset);
                new MipsAlu(MipsAlu.AluType.ADDU, rd, rd, offReg);
            }
        } else {
            Register base = MipsBuilder.getValueToRegister(pointer);
            if (base == null) {
                base = Register.K0; // Use K0 as temp for base
                Integer stackOff = MipsBuilder.getStackValueOffset(pointer);
                if (stackOff != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, base, Register.SP, stackOff);
                }
            }
            new MipsAlu(MipsAlu.AluType.ADDU, rd, base, offReg);
        }

        if (rd == Register.K0) {
            Integer stackOff = MipsBuilder.getStackValueOffset(this);
            if (stackOff != null) {
                new MipsLsu(MipsLsu.LsuType.SW, Register.K0, Register.SP, stackOff);
            }
        }
    }
}
