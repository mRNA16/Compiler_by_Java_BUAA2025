package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrPointerType;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrGlobalValue;
import midend.llvm.instr.memory.AllocateInstr;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsLsu;

public class LoadInstr extends Instr {
    public LoadInstr(IrValue pointer) {
        super(((IrPointerType) pointer.getIrType()).getTargetType(), InstrType.LOAD);
        this.addUseValue(pointer);
    }

    public IrValue getPointer() {
        return this.getUseValueList().get(0);
    }

    @Override
    public String toString() {
        IrValue actualPointer = getPointer();
        return irName + " = load " + irType + ", " + actualPointer.getIrType() + " " + actualPointer.getIrName();
    }

    @Override
    public void toMips() {
        super.toMips();
        // 从 useValueList 获取真正的指针操作数，以支持 MemToReg 优化后的值替换
        IrValue actualPointer = this.getUseValueList().get(0);

        Register rd = MipsBuilder.getValueToRegister(this);
        if (rd == null) {
            rd = Register.K0;
        }

        if (actualPointer instanceof IrGlobalValue) {
            new MipsLsu(MipsLsu.LsuType.LW, rd, actualPointer.getMipsLabel());
        } else if (actualPointer instanceof AllocateInstr) {
            Integer offset = MipsBuilder.getAllocaDataOffset(actualPointer);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.LW, rd, Register.SP, offset);
            }
        } else {
            Register base = MipsBuilder.getValueToRegister(actualPointer);
            if (base != null) {
                new MipsLsu(MipsLsu.LsuType.LW, rd, base, 0);
            } else {
                Register temp = Register.K1;
                Integer offset = MipsBuilder.getStackValueOffset(actualPointer);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.LW, temp, Register.SP, offset);
                    new MipsLsu(MipsLsu.LsuType.LW, rd, temp, 0);
                }
            }
        }

        if (rd == Register.K0) {
            Integer offset = MipsBuilder.getStackValueOffset(this);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.SW, Register.K0, Register.SP, offset);
            }
        }
    }
}
