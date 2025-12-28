package midend.llvm.instr.convert;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrType;
import midend.llvm.value.IrValue;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsLsu;

public class ZextInstr extends Instr {
    private final IrType targetType;
    private final IrValue originValue;

    public ZextInstr(IrType targetType, IrValue originValue) {
        super(targetType, InstrType.EXTEND);
        this.targetType = targetType;
        this.originValue = originValue;
        this.addUseValue(originValue);
    }

    public IrType getTargetType() {
        return targetType;
    }

    public IrValue getOriginValue() {
        return originValue;
    }

    @Override
    public String toString() {
        return irName + " = zext " + originValue.getIrType() + " " + originValue.getIrName() + " to " + targetType;
    }

    @Override
    public void toMips() {
        super.toMips();
        Register rd = MipsBuilder.allocateStackForValue(this) == null ? MipsBuilder.getValueToRegister(this)
                : Register.K0;

        Register rs = MipsBuilder.getValueToRegister(originValue);
        if (rs == null) {
            rs = Register.K0;
            Integer offset = MipsBuilder.getStackValueOffset(originValue);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.LW, rs, Register.SP, offset);
            }
        }

        if (originValue.getIrType().isInt8Type()) {
            // zext i8 to i32: andi rd, rs, 0xFF
            new MipsAlu(MipsAlu.AluType.ANDI, rd, rs, 0xFF);
        } else {
            // zext i1 to i32: just move
            if (rd != rs) {
                new MipsAlu(MipsAlu.AluType.ADDU, rd, Register.ZERO, rs);
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
