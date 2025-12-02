package midend.llvm.instr.io;

import midend.llvm.constant.IrConstString;
import midend.llvm.type.IrBaseType;
import midend.llvm.type.IrPointerType;

public class PrintStrInstr extends IOInstr{
    private final IrConstString irConstString;

    public PrintStrInstr(IrConstString irConstString) {
        super(IrBaseType.VOID);
        this.irConstString = irConstString;
        this.addUseValue(irConstString);
    }

    @Override
    public String toString() {
        IrPointerType irPointerType = (IrPointerType) this.irConstString.getIrType();
        return "call void @putstr(i8* getelementptr inbounds (" +
                irPointerType.getTargetType() + ", " +
                irPointerType + " " +
                this.irConstString.getIrName() + ", i64 0, i64 0))";
    }
}
