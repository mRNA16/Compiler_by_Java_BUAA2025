package midend.llvm.instr.ctrl;

import midend.llvm.IrBuilder;
import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;

import java.util.ArrayList;
import java.util.List;

public class CallInstr extends Instr {
    private IrFunction function;
    private List<IrValue> args;

    public CallInstr(IrFunction function, List<IrValue> args) {
        super(function.getReturnType(),InstrType.CALL,
                (function.getReturnType().isVoidType()?"call": IrBuilder.getLocalVarNameIr()));
        this.function = function;
        this.addUseValue(function);
        this.args = new ArrayList<>(args);
        args.forEach(this::addUseValue);
    }

    public IrFunction getFunction() {
        return function;
    }

    public List<IrValue> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!function.getReturnType().isVoidType()) {
            sb.append(irName);
            sb.append(" = ");
        }
        sb.append("call ")
                .append((function.getReturnType().isVoidType())?"void":function.getReturnType())
                .append(" ")
                .append(function.getIrName())
                .append("(");

        boolean haveArgs = false;
        for(IrValue arg : args) {
            sb.append(arg.getIrType().toString())
                    .append(" ");
            sb.append(arg.getIrName()).append(", ");
            haveArgs = true;
        }
        if(haveArgs) sb.delete(sb.length()-2, sb.length());
        sb.append(")");
        return sb.toString();
    }
}
