package midend.llvm.constant;

import midend.llvm.type.IrBaseType;

public class IrConstChar extends IrConstant{
    private final char value;

    public IrConstChar(int value) {
        super(IrBaseType.INT8,String.valueOf(value));
        this.value = (char) value;
    }

    public char getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "i8 " + (int) this.value;
    }
}
