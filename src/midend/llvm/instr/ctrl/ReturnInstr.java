package midend.llvm.instr.ctrl;

import midend.llvm.IrBuilder;
import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;

public class ReturnInstr extends Instr {
    private IrValue returnValue;
    public ReturnInstr(IrValue returnValue) {
        super(IrBaseType.VOID, InstrType.RETURN,"return");
        this.returnValue = returnValue;
        this.addUseValue(returnValue);
    }

    public IrValue getReturnValue() {
        return returnValue;
    }

    @Override
    public String toString() {
        return "ret " + ((returnValue!=null)?(returnValue.getIrType().toString()+ " " + returnValue.getIrName()):"void");
    }
}
