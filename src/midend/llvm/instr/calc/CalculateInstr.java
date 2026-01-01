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
                case MUL -> {
                    if (isPowerOfTwo(Math.abs(imm))) {
                        int n = log2(Math.abs(imm));
                        new MipsAlu(MipsAlu.AluType.SLL, rd, leftReg, n);
                        if (imm < 0) {
                            new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.ZERO, rd);
                        }
                    } else if (imm == 0) {
                        new MipsAlu(MipsAlu.AluType.ADDU, rd, Register.ZERO, Register.ZERO);
                    } else {
                        Register rightReg = Register.K1;
                        new MipsAlu(MipsAlu.AluType.ADDIU, rightReg, Register.ZERO, imm);
                        new MipsMdu(MipsMdu.MduType.MULT, leftReg, rightReg);
                        new MipsMdu(MipsMdu.MduType.MFLO, rd);
                    }
                }
                case SDIV -> divOptimize(leftReg, imm, rd);
                case SREM -> remOptimize(leftReg, imm, rd);
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

    private void divOptimize(Register dividend, int imm, Register rd) {
        if (imm == 1) {
            if (rd != dividend)
                new MipsAlu(MipsAlu.AluType.ADDU, rd, dividend, Register.ZERO);
        } else if (imm == -1) {
            new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.ZERO, dividend);
        } else if (isPowerOfTwo(Math.abs(imm))) {
            int n = log2(Math.abs(imm));
            // 使用 K0 作为中间计算，避免覆盖 dividend
            new MipsAlu(MipsAlu.AluType.SRA, Register.K0, dividend, 31);
            new MipsAlu(MipsAlu.AluType.SRL, Register.K0, Register.K0, 32 - n);
            new MipsAlu(MipsAlu.AluType.ADDU, Register.K0, dividend, Register.K0);
            new MipsAlu(MipsAlu.AluType.SRA, Register.K0, Register.K0, n);
            if (imm < 0) {
                new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.ZERO, Register.K0);
            } else {
                if (rd != Register.K0)
                    new MipsAlu(MipsAlu.AluType.ADDU, rd, Register.K0, Register.ZERO);
            }
        } else {
            // Magic Number Division
            long d = Math.abs(imm);
            Multiplier multiplier = getMultiplier(d, 31);
            BigInteger m = multiplier.m;
            int post = multiplier.post;

            // 1. 将被除数加载到 K1，魔法数加载到 K0
            new MipsAlu(MipsAlu.AluType.ADDU, Register.K1, dividend, Register.ZERO);
            new MarsLi(Register.K0, m.intValue());

            // 2. 执行乘法 (signed mult)
            new MipsMdu(MipsMdu.MduType.MULT, Register.K0, Register.K1);

            // 3. 取高位结果并进行修正
            if (m.compareTo(BigInteger.ONE.shiftLeft(31)) < 0) {
                new MipsMdu(MipsMdu.MduType.MFHI, Register.K0);
            } else {
                new MipsMdu(MipsMdu.MduType.MFHI, Register.K0);
                new MipsAlu(MipsAlu.AluType.ADDU, Register.K0, Register.K0, Register.K1);
            }

            // 4. 右移 post 位
            if (post > 0) {
                new MipsAlu(MipsAlu.AluType.SRA, Register.K0, Register.K0, post);
            }

            // 5. 修正负数情况: q = q + (n < 0 ? 1 : 0)
            new MipsCompare(MipsCompare.CompareType.SLT, Register.K1, Register.K1, Register.ZERO);
            new MipsAlu(MipsAlu.AluType.ADDU, Register.K0, Register.K0, Register.K1);

            // 6. 处理除数正负并写入结果
            if (imm < 0) {
                new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.ZERO, Register.K0);
            } else {
                if (rd != Register.K0)
                    new MipsAlu(MipsAlu.AluType.ADDU, rd, Register.K0, Register.ZERO);
            }
        }
    }

    private void remOptimize(Register dividend, int imm, Register rd) {
        if (Math.abs(imm) == 1) {
            new MipsAlu(MipsAlu.AluType.ADDU, rd, Register.ZERO, Register.ZERO);
        } else {
            // m % n = m - (m / n * n)
            // 1. 保护被除数到 FP，因为 divOptimize 会修改 K0/K1
            new MipsAlu(MipsAlu.AluType.ADDU, Register.FP, dividend, Register.ZERO);

            // 2. 计算 q = m / n，结果存入 GP
            divOptimize(Register.FP, imm, Register.GP);

            // 3. 计算 p = q * n，结果存入 GP
            if (isPowerOfTwo(Math.abs(imm))) {
                int n = log2(Math.abs(imm));
                new MipsAlu(MipsAlu.AluType.SLL, Register.GP, Register.GP, n);
                if (imm < 0) {
                    new MipsAlu(MipsAlu.AluType.SUBU, Register.GP, Register.ZERO, Register.GP);
                }
            } else {
                new MarsLi(Register.K0, imm);
                new MipsMdu(MipsMdu.MduType.MULT, Register.GP, Register.K0);
                new MipsMdu(MipsMdu.MduType.MFLO, Register.GP);
            }

            // 4. 计算 m - p，结果存入 rd
            new MipsAlu(MipsAlu.AluType.SUBU, rd, Register.FP, Register.GP);
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
