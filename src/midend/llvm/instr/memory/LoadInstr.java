package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrPointerType;
import midend.llvm.value.IrValue;

public class LoadInstr extends Instr {
    private final IrValue pointer;
    public LoadInstr(IrValue pointer) {
        super(((IrPointerType)pointer.getIrType()).getTargetType(), InstrType.LOAD);
        this.pointer = pointer;
        this.addUseValue(pointer);
    }

    @Override
    public String toString() {
        return irName + " = load " + irType + ", " + pointer.getIrType() + " " + pointer.getIrName();
    }
}
