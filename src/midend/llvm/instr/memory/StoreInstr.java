package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrGlobalValue;
import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.memory.AllocateInstr;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsLsu;
import backend.mips.assembly.MipsAlu;

public class StoreInstr extends Instr {
    private final IrValue bury;
    private final IrValue address;

    public StoreInstr(IrValue bury, IrValue address) {
        super(IrBaseType.VOID, InstrType.STORE, "store");
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

    @Override
    public void toMips() {
        super.toMips();
        Register rs = MipsBuilder.getValueToRegister(bury);
        if (rs == null) {
            rs = Register.K0;
            if (bury instanceof IrConstInt constInt) {
                new MipsAlu(MipsAlu.AluType.ADDIU, rs, Register.ZERO, constInt.getValue());
            } else {
                Integer offset = MipsBuilder.getStackValueOffset(bury);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, rs, Register.SP, offset);
                }
            }
        }

        if (address instanceof IrGlobalValue) {
            new MipsLsu(MipsLsu.LsuType.SW, rs, address.getMipsLabel());
        } else if (address instanceof AllocateInstr) {
            Integer offset = MipsBuilder.getAllocaDataOffset(address);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.SW, rs, Register.SP, offset);
            }
        } else {
            Register base = MipsBuilder.getValueToRegister(address);
            if (base != null) {
                new MipsLsu(MipsLsu.LsuType.SW, rs, base, 0);
            } else {
                Register temp = Register.K1;
                Integer offset = MipsBuilder.getStackValueOffset(address);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, temp, Register.SP, offset);
                    new MipsLsu(MipsLsu.LsuType.SW, rs, temp, 0);
                }
            }
        }
    }
}
