package midend.llvm.type;

public class IrPointerType extends IrType{
    private final IrType targetType;

    public IrPointerType(IrType targetType) {
        this.targetType = targetType;
    }

    public IrType getTargetType() {
        return targetType;
    }

    public int getSize() {
        return 4;
    }

    @Override
    public boolean isPointerType() {
        return true;
    }

    @Override
    public String toString() {
        return targetType.toString() + "*";
    }
}
