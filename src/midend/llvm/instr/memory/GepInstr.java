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
        return this.getUseValueList().get(0);
    }

    public IrValue getOffset() {
        return this.getUseValueList().get(1);
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
        IrValue actualPointer = getPointer();
        IrValue actualOffset = getOffset();
        IrType pTargetType = ((IrPointerType) actualPointer.getIrType()).getTargetType();
        sb.append(pTargetType).append(", ").append(actualPointer.getIrType()).append(" ")
                .append(actualPointer.getIrName())
                .append(", ");
        if (pTargetType.isArrayType())
            sb.append("i32 0, ");
        sb.append(actualOffset.getIrType()).append(" ").append(actualOffset.getIrName());
        return sb.toString();
    }

    @Override
    public void toMips() {
        super.toMips();
        // 从 useValueList 获取真正的操作数，以支持 MemToReg 优化后的值替换
        IrValue actualPointer = this.getUseValueList().get(0);
        IrValue actualOffset = this.getUseValueList().get(1);

        Register rd = MipsBuilder.allocateStackForValue(this) == null ? MipsBuilder.getValueToRegister(this)
                : Register.K0;

        // Calculate offset in bytes: offset * 4
        Register offReg = Register.K1;
        if (actualOffset instanceof IrConstInt constInt) {
            new MipsAlu(MipsAlu.AluType.ADDIU, offReg, Register.ZERO, constInt.getValue() * 4);
        } else {
            Register temp = MipsBuilder.getValueToRegister(actualOffset);
            if (temp == null) {
                temp = Register.K1;
                Integer stackOff = MipsBuilder.getStackValueOffset(actualOffset);
                if (stackOff != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, temp, Register.SP, stackOff);
                }
            }
            new MipsAlu(MipsAlu.AluType.SLL, offReg, temp, 2); // * 4
        }

        // Calculate base + offset
        if (actualPointer instanceof IrGlobalValue) {
            new MipsLsu(MipsLsu.LsuType.LA, rd, actualPointer.getMipsLabel());
            new MipsAlu(MipsAlu.AluType.ADDU, rd, rd, offReg);
        } else if (actualPointer instanceof AllocateInstr) {
            Integer baseOffset = MipsBuilder.getAllocaDataOffset(actualPointer);
            if (baseOffset != null) {
                new MipsAlu(MipsAlu.AluType.ADDIU, rd, Register.SP, baseOffset);
                new MipsAlu(MipsAlu.AluType.ADDU, rd, rd, offReg);
            }
        } else {
            Register base = MipsBuilder.getValueToRegister(actualPointer);
            if (base == null) {
                base = Register.K0; // Use K0 as temp for base
                Integer stackOff = MipsBuilder.getStackValueOffset(actualPointer);
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
