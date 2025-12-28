package midend.llvm.instr;

import backend.mips.assembly.MipsAnnotation;
import midend.llvm.IrBuilder;
import midend.llvm.type.IrType;
import midend.llvm.use.IrUser;
import midend.llvm.value.IrBasicBlock;

public abstract class Instr extends IrUser {
    private IrBasicBlock block;
    private final InstrType instrType;

    public Instr(IrType irType, InstrType instrType) {
        super(irType, IrBuilder.getLocalVarNameIr());
        this.instrType = instrType;
        IrBuilder.addInstr(this);
    }

    public Instr(IrType irType, InstrType instrType, String irName) {
        super(irType, irName);
        this.instrType = instrType;
        IrBuilder.addInstr(this);
    }

    public Instr(IrType irType, InstrType instrType, String irName, boolean auto) {
        super(irType, irName);
        this.instrType = instrType;
        if (auto)
            IrBuilder.addInstr(this);
    }

    public void setBlock(IrBasicBlock block) {
        this.block = block;
    }

    public IrBasicBlock getBlock() {
        return block;
    }

    public InstrType getInstrType() {
        return instrType;
    }

    @Override
    public abstract String toString();

    public void toMips() {
        new MipsAnnotation(this.toString());
    }
}
