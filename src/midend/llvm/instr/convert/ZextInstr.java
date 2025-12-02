package midend.llvm.instr.convert;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.type.IrType;
import midend.llvm.value.IrValue;

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
}
