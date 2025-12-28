package midend.llvm.constant;

import midend.llvm.type.IrBaseType;
import backend.mips.assembly.data.MipsWord;

public class IrConstInt extends IrConstant {
    private final int value;

    public IrConstInt(int value) {
        super(IrBaseType.INT32, String.valueOf(value));
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "i32 " + this.value;
    }

    @Override
    public void mipsDeclare(String label) {
        new MipsWord(label, this.value);
    }
}
