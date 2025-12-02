package midend.llvm.instr.io;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrType;

public abstract class IOInstr extends Instr {
    public IOInstr(IrType irType) {
        super(irType, InstrType.IO);
    }

    @Override
    public abstract String toString();
}
