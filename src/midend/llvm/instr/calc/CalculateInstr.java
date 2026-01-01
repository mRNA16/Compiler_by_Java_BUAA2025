package midend.llvm.instr.calc;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;
import midend.llvm.constant.IrConstInt;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsMdu;
import backend.mips.assembly.MipsLsu;

public class CalculateInstr extends Instr {
    public enum CalculateType {
        ADD,
        SUB,
        MUL,
        SDIV,
        SREM,
        AND,
        OR
    }

    private CalculateType calculateType;
    private IrValue L;
    private IrValue R;

    public CalculateInstr(String op, IrValue L, IrValue R) {
        super(IrBaseType.INT32, InstrType.ALU);
        this.L = L;
        this.R = R;
        this.addUseValue(L);
        this.addUseValue(R);
        this.calculateType = string2CalculateType(op);
    }

    public CalculateType getCalculateType() {
        return calculateType;
    }

    public IrValue getL() {
        return this.getUseValueList().get(0);
    }

    public IrValue getR() {
        return this.getUseValueList().get(1);
    }

    @Override
    public String toString() {
        return irName + " = " + calculateType.toString().toLowerCase() + " i32 " +
                L.getIrName() + ", " + R.getIrName();

    }

    private CalculateType string2CalculateType(String s) {
        return switch (s) {
            case "+" -> CalculateType.ADD;
            case "-" -> CalculateType.SUB;
            case "*" -> CalculateType.MUL;
            case "/" -> CalculateType.SDIV;
            case "%" -> CalculateType.SREM;
            case "&", "&&" -> CalculateType.AND;
            case "|", "||" -> CalculateType.OR;
            default -> throw new RuntimeException("Unknown calculate type");
        };
    }

    @Override
    public void toMips() {
        super.toMips();
        // 从 useValueList 获取真正的操作数，以支持 MemToReg 优化后的值替换
        IrValue actualL = this.getUseValueList().get(0);
        IrValue actualR = this.getUseValueList().get(1);

        // 目的寄存器
        Register rd = MipsBuilder.allocateStackForValue(this) == null ? MipsBuilder.getValueToRegister(this)
                : Register.K0;

        // 左操作数
        Register leftReg = getOperandReg(actualL, Register.K0);

        // 右操作数（可能是立即数）
        if (actualR instanceof IrConstInt constInt && isAluImm(constInt.getValue())) {
            int imm = constInt.getValue();
            switch (calculateType) {
                case ADD -> new MipsAlu(MipsAlu.AluType.ADDIU, rd, leftReg, imm);
                case SUB -> new MipsAlu(MipsAlu.AluType.ADDIU, rd, leftReg, -imm);
                case MUL -> { // MUL不支持立即数，需要加载
                    Register rightReg = Register.K1;
                    new MipsAlu(MipsAlu.AluType.ADDIU, rightReg, Register.ZERO, imm);
                    new MipsMdu(MipsMdu.MduType.MULT, leftReg, rightReg);
                    new MipsMdu(MipsMdu.MduType.MFLO, rd);
                }
                case SDIV -> {
                    Register rightReg = Register.K1;
                    new MipsAlu(MipsAlu.AluType.ADDIU, rightReg, Register.ZERO, imm);
                    new MipsMdu(MipsMdu.MduType.DIV, leftReg, rightReg);
                    new MipsMdu(MipsMdu.MduType.MFLO, rd);
                }
                case SREM -> {
                    Register rightReg = Register.K1;
                    new MipsAlu(MipsAlu.AluType.ADDIU, rightReg, Register.ZERO, imm);
                    new MipsMdu(MipsMdu.MduType.DIV, leftReg, rightReg);
                    new MipsMdu(MipsMdu.MduType.MFHI, rd);
                }
                case AND -> new MipsAlu(MipsAlu.AluType.ANDI, rd, leftReg, imm);
                case OR -> new MipsAlu(MipsAlu.AluType.ORI, rd, leftReg, imm);
            }
        } else {
            Register rightReg = getOperandReg(actualR, Register.K1);
            switch (calculateType) {
                case ADD -> new MipsAlu(MipsAlu.AluType.ADDU, rd, leftReg, rightReg);
                case SUB -> new MipsAlu(MipsAlu.AluType.SUBU, rd, leftReg, rightReg);
                case MUL -> {
                    new MipsMdu(MipsMdu.MduType.MULT, leftReg, rightReg);
                    new MipsMdu(MipsMdu.MduType.MFLO, rd);
                }
                case SDIV -> {
                    new MipsMdu(MipsMdu.MduType.DIV, leftReg, rightReg);
                    new MipsMdu(MipsMdu.MduType.MFLO, rd);
                }
                case SREM -> {
                    new MipsMdu(MipsMdu.MduType.DIV, leftReg, rightReg);
                    new MipsMdu(MipsMdu.MduType.MFHI, rd);
                }
                case AND -> new MipsAlu(MipsAlu.AluType.AND, rd, leftReg, rightReg);
                case OR -> new MipsAlu(MipsAlu.AluType.OR, rd, leftReg, rightReg);
            }
        }

        // 如果rd是K0，说明结果需要存回栈
        if (rd == Register.K0) {
            Integer offset = MipsBuilder.getStackValueOffset(this);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.SW, Register.K0, Register.SP, offset);
            }
        }
    }

    private Register getOperandReg(IrValue value, Register tempReg) {
        Register reg = MipsBuilder.getValueToRegister(value);
        if (reg != null) {
            return reg;
        }
        if (value instanceof IrConstInt constInt) {
            new MipsAlu(MipsAlu.AluType.ADDIU, tempReg, Register.ZERO, constInt.getValue());
            return tempReg;
        }
        // 从栈中加载
        Integer offset = MipsBuilder.getStackValueOffset(value);
        if (offset != null) {
            new MipsLsu(MipsLsu.LsuType.LW, tempReg, Register.SP, offset);
            return tempReg;
        }
        // 应该是全局变量或其他
        // TODO: handle global variables
        return tempReg;
    }

    private boolean isAluImm(int val) {
        return val >= -32768 && val <= 32767;
    }
}
