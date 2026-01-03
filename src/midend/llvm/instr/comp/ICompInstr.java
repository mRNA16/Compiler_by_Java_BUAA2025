package midend.llvm.instr.comp;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;
import midend.llvm.constant.IrConstInt;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsCompare;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsLsu;

public class ICompInstr extends Instr {
    public enum ICompType {
        EQ,
        NE,
        SGE,
        SGT,
        SLE,
        SLT
    }

    private final ICompType compType;

    public ICompInstr(String op, IrValue L, IrValue R) {
        super(IrBaseType.INT1, InstrType.CMP);
        this.addUseValue(L);
        this.addUseValue(R);
        this.compType = string2ICompType(op);
    }

    public ICompType getCompType() {
        return compType;
    }

    public IrValue getL() {
        return this.getUseValueList().get(0);
    }

    public IrValue getR() {
        return this.getUseValueList().get(1);
    }

    @Override
    public String toString() {
        IrValue actualL = getL();
        IrValue actualR = getR();
        return irName + " = icmp " + compType.toString().toLowerCase() + " i32 " + actualL.getIrName() + ", "
                + actualR.getIrName();
    }

    private ICompType string2ICompType(String s) {
        return switch (s) {
            case "==" -> ICompType.EQ;
            case "!=" -> ICompType.NE;
            case ">" -> ICompType.SGT;
            case "<" -> ICompType.SLT;
            case ">=" -> ICompType.SGE;
            case "<=" -> ICompType.SLE;
            default -> throw new RuntimeException("Unknown ICompType");
        };
    }

    @Override
    public void toMips() {
        super.toMips();
        // 从 useValueList 获取真正的操作数，以支持 MemToReg 优化后的值替换
        IrValue actualL = this.getUseValueList().get(0);
        IrValue actualR = this.getUseValueList().get(1);

        Register rd = MipsBuilder.getValueToRegister(this);
        if (rd == null) {
            rd = Register.K0;
        }

        Register leftReg = getOperandReg(actualL, Register.K0);

        if (actualR instanceof IrConstInt constInt && isAluImm(constInt.getValue())) {
            int imm = constInt.getValue();
            if (compType == ICompType.SLT) {
                new MipsCompare(MipsCompare.CompareType.SLTI, rd, leftReg, imm);
            } else {
                Register rightReg = Register.K1;
                new MipsAlu(MipsAlu.AluType.ADDIU, rightReg, Register.ZERO, imm);
                emitCompare(rd, leftReg, rightReg);
            }
        } else {
            Register rightReg = getOperandReg(actualR, Register.K1);
            emitCompare(rd, leftReg, rightReg);
        }

        if (rd == Register.K0) {
            Integer offset = MipsBuilder.getStackValueOffset(this);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.SW, Register.K0, Register.SP, offset);
            }
        }
    }

    private void emitCompare(Register rd, Register rs, Register rt) {
        switch (compType) {
            case EQ -> new MipsCompare(MipsCompare.CompareType.SEQ, rd, rs, rt);
            case NE -> new MipsCompare(MipsCompare.CompareType.SNE, rd, rs, rt);
            case SGE -> new MipsCompare(MipsCompare.CompareType.SGE, rd, rs, rt);
            case SGT -> new MipsCompare(MipsCompare.CompareType.SGT, rd, rs, rt);
            case SLE -> new MipsCompare(MipsCompare.CompareType.SLE, rd, rs, rt);
            case SLT -> new MipsCompare(MipsCompare.CompareType.SLT, rd, rs, rt);
        }
    }

    private Register getOperandReg(IrValue value, Register tempReg) {
        Register reg = MipsBuilder.getValueToRegister(value);
        if (reg != null) {
            return reg;
        }
        MipsBuilder.loadValueToReg(value, tempReg);
        return tempReg;
    }

    private boolean isAluImm(int val) {
        return val >= -32768 && val <= 32767;
    }
}
