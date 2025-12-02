package midend.llvm.instr.io;

import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;

public class PrintIntInstr extends IOInstr{
    private final IrValue printValue;
    public PrintIntInstr(IrValue printValue) {
        super(IrBaseType.VOID);
        this.printValue = printValue;
        this.addUseValue(printValue);
    }

    public IrValue getPrintValue() {
        return printValue;
    }

    @Override
    public String toString() {
        return "call void @putint(i32 " + printValue.getIrName() + ")";
    }
}
