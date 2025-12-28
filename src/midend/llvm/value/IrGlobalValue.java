package midend.llvm.value;

import midend.llvm.constant.IrConstant;
import midend.llvm.type.IrType;
import midend.llvm.use.IrUser;

import backend.mips.MipsBuilder;
import backend.mips.assembly.data.MipsWord;
import backend.mips.assembly.data.MipsSpaceOptimize;

public class IrGlobalValue extends IrUser {
    private final IrConstant globalValue;

    public IrGlobalValue(String name, IrType irType, IrConstant globalValue) {
        super(irType, name);
        this.globalValue = globalValue;
    }

    public IrConstant getGlobalValue() {
        return globalValue;
    }

    public IrType getIrType() {
        return super.getIrType();
    }

    @Override
    public String toString() {
        return this.irName + " = dso_local global " + this.globalValue;
    }

    @Override
    public void toMips() {
        globalValue.mipsDeclare(this.getMipsLabel());
    }
}
