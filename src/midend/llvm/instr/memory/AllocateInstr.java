package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrPointerType;
import midend.llvm.type.IrType;

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
}
