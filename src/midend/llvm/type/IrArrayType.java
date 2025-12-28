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

    public int getSize() {
        // 数组大小 = 元素个数 × 每个元素的大小
        return arraySize * elementType.getSize();
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
