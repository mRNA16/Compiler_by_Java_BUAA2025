package midend.llvm.instr.comp;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrValue;

public class ICompInstr extends Instr {
    public enum ICompType{
        EQ,
        NE,
        SGE,
        SGT,
        SLE,
        SLT
    }
    private final ICompType compType;
    private final IrValue L;
    private final IrValue R;

    public ICompInstr(String op, IrValue L, IrValue R) {
        super(IrBaseType.INT1, InstrType.CMP);
        this.L = L;
        this.R = R;
        this.addUseValue(L);
        this.addUseValue(R);
        this.compType = string2ICompType(op);
    }

    public ICompType getCompType() {
        return compType;
    }

    public IrValue getL() {
        return L;
    }

    public IrValue getR() {
        return R;
    }

    @Override
    public String toString() {
        return irName + " = icmp " + compType.toString().toLowerCase() + " i32 " + L.getIrName() + ", " + R.getIrName();
    }

    private ICompType string2ICompType(String s){
        return switch (s){
            case "==" -> ICompType.EQ;
            case "!=" -> ICompType.NE;
            case ">" -> ICompType.SGT;
            case "<" -> ICompType.SLT;
            case ">=" -> ICompType.SGE;
            case "<=" -> ICompType.SLE;
            default -> throw new RuntimeException("Unknown ICompType");
        };
    }
}
