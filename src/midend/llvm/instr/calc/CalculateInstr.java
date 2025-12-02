package midend.llvm.instr.calc;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;

public class CalculateInstr extends Instr {
    public enum CalculateType{
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

    public CalculateInstr(String op,IrValue L,IrValue R) {
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
        return L;
    }

    public IrValue getR() {
        return R;
    }

    @Override
    public String toString() {
        return irName + " = " + calculateType.toString().toLowerCase() + " i32 " +
                L.getIrName() + ", " + R.getIrName();

    }

    private CalculateType string2CalculateType(String s) {
        return switch (s){
            case "+" -> CalculateType.ADD;
            case "-" -> CalculateType.SUB;
            case "*" -> CalculateType.MUL;
            case "/" -> CalculateType.SDIV;
            case "%" -> CalculateType.SREM;
            case "&","&&" -> CalculateType.AND;
            case "|","||" -> CalculateType.OR;
            default -> throw new RuntimeException("Unknown calculate type");
        };
    }
}
