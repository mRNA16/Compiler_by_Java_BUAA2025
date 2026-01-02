package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrGlobalValue;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsLsu;

public class StoreInstr extends Instr {

    public StoreInstr(IrValue bury, IrValue address) {
        super(IrBaseType.VOID, InstrType.STORE, "store");
        this.addUseValue(bury);
        this.addUseValue(address);
    }

    public IrValue getBury() {
        return this.getUseValueList().get(0);
    }

    public IrValue getAddress() {
        return this.getUseValueList().get(1);
    }

    @Override
    public String toString() {
        IrValue actualBury = getBury();
        IrValue actualAddress = getAddress();
        return "store " + actualBury.getIrType() + " " + actualBury.getIrName()
                + ", " + actualAddress.getIrType() + " " + actualAddress.getIrName();
    }

    @Override
    public void toMips() {
        super.toMips();
        // 从 useValueList 获取真正的操作数，以支持 MemToReg 优化后的值替换
        IrValue actualBury = this.getUseValueList().get(0);
        IrValue actualAddress = this.getUseValueList().get(1);

        Register rs = Register.K0;
        MipsBuilder.loadValueToReg(actualBury, rs);
        Register allocatedRs = MipsBuilder.getValueToRegister(actualBury);
        if (allocatedRs != null) {
            rs = allocatedRs;
        }

        if (actualAddress instanceof IrGlobalValue) {
            new MipsLsu(MipsLsu.LsuType.SW, rs, actualAddress.getMipsLabel());
        } else if (actualAddress instanceof AllocateInstr) {
            Integer offset = MipsBuilder.getAllocaDataOffset(actualAddress);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.SW, rs, Register.SP, offset);
            }
        } else {
            Register base = MipsBuilder.getValueToRegister(actualAddress);
            if (base != null) {
                new MipsLsu(MipsLsu.LsuType.SW, rs, base, 0);
            } else {
                Register temp = Register.K1;
                Integer offset = MipsBuilder.getStackValueOffset(actualAddress);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, temp, Register.SP, offset);
                    new MipsLsu(MipsLsu.LsuType.SW, rs, temp, 0);
                }
            }
        }
    }
}
