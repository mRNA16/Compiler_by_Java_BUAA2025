package midend.llvm.type;

public class IrArrayType extends IrType{
    private final int arraySize;
    private final IrType elementType;

    public IrArrayType(int arraySize, IrType elementType) {
        this.arraySize = arraySize;
        this.elementType = elementType;
    }

    public int getArraySize() {
        return arraySize;
    }

    public IrType getElementType() {
        return elementType;
    }

    @Override
    public boolean isArrayType() {
        return true;
    }

    @Override
    public String toString() {
        return "[" + arraySize + " x " + elementType + "]";
    }
}
