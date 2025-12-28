package midend.llvm.type;

public class IrFunctionType extends IrType {
    private final IrType returnType;

    public IrFunctionType(IrType returnType) {
        this.returnType = returnType;
    }

    public IrType getReturnType() {
        return this.returnType;
    }

    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return this.returnType.toString();
    }
}