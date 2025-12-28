package midend.llvm.constant;

import midend.llvm.type.IrType;
import midend.llvm.value.IrValue;

public abstract class IrConstant extends IrValue {
    public IrConstant(IrType type, String name) {
        super(type, name);
    }

    @Override
    public abstract String toString();

    public void mipsDeclare(String label) {
    }
}
