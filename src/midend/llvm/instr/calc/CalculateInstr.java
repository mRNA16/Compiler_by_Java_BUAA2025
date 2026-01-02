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
import backend.mips.assembly.MipsCompare;
import backend.mips.assembly.MipsLsu;
import backend.mips.assembly.fake.MarsLi;
import java.math.BigInteger;

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

    public CalculateInstr(String op, IrValue L, IrValue R) {
        super(IrBaseType.INT32, InstrType.ALU);
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
        IrValue actualL = getL();
        IrValue actualR = getR();
        return irName + " = " + calculateType.toString().toLowerCase() + " i32 " +
                actualL.getIrName() + ", " + actualR.getIrName();
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
        Register rd = MipsBuilder.getValueToRegister(this);
        if (rd == null) {
            rd = Register.K0;
        }

        // 左操作数
        Register leftReg = getOperandReg(actualL, Register.K0);

        // 右操作数（可能是立即数）
        if (actualR instanceof IrConstInt constInt && isAluImm(constInt.getValue())) {
            int imm = constInt.getValue();
            switch (calculateType) {
                case ADD -> new MipsAlu(MipsAlu.AluType.ADDIU, rd, leftReg, imm);
                case SUB -> new MipsAlu(MipsAlu.AluType.ADDIU, rd, leftReg, -imm);
                case MUL -> {
                    if (imm == 0) {
                        new MipsAlu(MipsAlu.AluType.ADDU, rd, Register.ZERO, Register.ZERO);
                    } else if (imm == 1) {
                        if (rd != leftReg)
                            new MipsAlu(MipsAlu.AluType.ADDU, rd, leftReg, Register.ZERO);
                    } else if (imm == -1) {
                        new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.ZERO, leftReg);
                    } else if (isPowerOfTwo(Math.abs(imm))) {
                        int n = log2(Math.abs(imm));
                        new MipsAlu(MipsAlu.AluType.SLL, rd, leftReg, n);
                        if (imm < 0) {
                            new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.ZERO, rd);
                        }
                    } else {
                        Register rightReg = Register.K1;
                        new MarsLi(rightReg, imm);
                        new MipsMdu(MipsMdu.MduType.MULT, leftReg, rightReg);
                        new MipsMdu(MipsMdu.MduType.MFLO, rd);
                    }
                }
                case SDIV -> divOptimize(actualL, imm, rd);
                case SREM -> remOptimize(actualL, imm, rd);
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

    private void divOptimize(IrValue dividendValue, int imm, Register rd) {
        if (imm == 1) {
            MipsBuilder.loadValueToReg(dividendValue, rd);
        } else if (imm == -1) {
            MipsBuilder.loadValueToReg(dividendValue, rd);
            new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.ZERO, rd);
        } else if (isPowerOfTwo(Math.abs(imm))) {
            int n = log2(Math.abs(imm));
            MipsBuilder.loadValueToReg(dividendValue, Register.K1);
            new MipsAlu(MipsAlu.AluType.SRA, Register.K0, Register.K1, 31);
            new MipsAlu(MipsAlu.AluType.SRL, Register.K0, Register.K0, 32 - n);
            new MipsAlu(MipsAlu.AluType.ADDU, Register.K0, Register.K1, Register.K0);
            new MipsAlu(MipsAlu.AluType.SRA, rd, Register.K0, n);
            if (imm < 0) {
                new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.ZERO, rd);
            }
        } else {
            // Magic Number Division
            long d = Math.abs(imm);
            Multiplier multiplier = getMultiplier(d, 31);
            BigInteger m = multiplier.m;
            int post = multiplier.post;

            MipsBuilder.loadValueToReg(dividendValue, Register.K1);
            if (m.compareTo(BigInteger.ONE.shiftLeft(31)) < 0) {
                new MarsLi(rd, m.intValue());
                new MipsMdu(MipsMdu.MduType.MULT, rd, Register.K1);
                new MipsMdu(MipsMdu.MduType.MFHI, rd);
            } else {
                new MarsLi(rd, m.intValue());
                new MipsMdu(MipsMdu.MduType.MULT, rd, Register.K1);
                new MipsMdu(MipsMdu.MduType.MFHI, rd);
                new MipsAlu(MipsAlu.AluType.ADDU, rd, rd, Register.K1);
            }

            if (post > 0) {
                new MipsAlu(MipsAlu.AluType.SRA, rd, rd, post);
            }

            new MipsCompare(MipsCompare.CompareType.SLT, Register.K1, Register.K1, Register.ZERO);
            new MipsAlu(MipsAlu.AluType.ADDU, rd, rd, Register.K1);

            if (imm < 0) {
                new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.ZERO, rd);
            }
        }
    }

    private void remOptimize(IrValue dividendValue, int imm, Register rd) {
        int absImm = Math.abs(imm);
        if (absImm == 1) {
            new MipsAlu(MipsAlu.AluType.ADDU, rd, Register.ZERO, Register.ZERO);
        } else {
            // m % n = m % |n| = m - (m / |n| * |n|)
            // 使用 FP 和 GP 寄存器，避免与 K0/K1 冲突
            MipsBuilder.loadValueToReg(dividendValue, Register.FP);
            divOptimize(dividendValue, absImm, Register.GP);

            if (isPowerOfTwo(absImm)) {
                int n = log2(absImm);
                new MipsAlu(MipsAlu.AluType.SLL, Register.GP, Register.GP, n);
            } else {
                new MarsLi(Register.K0, absImm);
                new MipsMdu(MipsMdu.MduType.MULT, Register.GP, Register.K0);
                new MipsMdu(MipsMdu.MduType.MFLO, Register.GP);
            }

            new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.FP, Register.GP);
        }
    }

    private Register getOperandReg(IrValue value, Register tempReg) {
        MipsBuilder.loadValueToReg(value, tempReg);
        Register reg = MipsBuilder.getValueToRegister(value);
        return reg != null ? reg : tempReg;
    }

    private boolean isAluImm(int val) {
        return val >= -32768 && val <= 32767;
    }

    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    private int log2(int n) {
        return (int) (Math.log(n) / Math.log(2));
    }

    private static class Multiplier {
        private final BigInteger m;
        private final int post;

        public Multiplier(BigInteger m, int post) {
            this.m = m;
            this.post = post;
        }
    }

    private Multiplier getMultiplier(long d, int prec) {
        int l = 32 - countLeadingZeros(d - 1);
        int post = l;
        BigInteger low = BigInteger.ONE.shiftLeft(32 + l).divide(BigInteger.valueOf(d));
        BigInteger high = BigInteger.ONE.shiftLeft(32 + l).add(BigInteger.ONE.shiftLeft(32 + l - prec))
                .divide(BigInteger.valueOf(d));
        while (low.shiftRight(1).compareTo(high.shiftRight(1)) < 0 && post > 0) {
            low = low.shiftRight(1);
            high = high.shiftRight(1);
            post--;
        }
        return new Multiplier(high, post);
    }

    private int countLeadingZeros(long x) {
        int count = 0;
        for (int i = 31; i >= 0; i--) {
            if ((x & (1L << i)) != 0)
                break;
            count++;
        }
        return count;
    }
}
