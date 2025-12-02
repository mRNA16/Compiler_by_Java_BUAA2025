package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;

public class StoreInstr extends Instr {
    private final IrValue bury;
    private final IrValue address;

    public StoreInstr(IrValue bury, IrValue address) {
        super(IrBaseType.VOID, InstrType.STORE,"store");
        this.bury = bury;
        this.address = address;
        this.addUseValue(bury);
        this.addUseValue(address);
    }

    public IrValue getBury() {
        return bury;
    }

    public IrValue getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "store " + bury.getIrType() + " " + bury.getIrName()
                + ", " + address.getIrType() + " " + address.getIrName();
    }
}
