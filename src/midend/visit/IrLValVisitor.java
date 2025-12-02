package midend.visit;

import frontend.ast.Exp;
import frontend.ast.LVal;
import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.memory.GepInstr;
import midend.llvm.instr.memory.LoadInstr;
import midend.llvm.type.IrPointerType;
import midend.llvm.value.IrValue;
import midend.semantic.Symbol;
import midend.semantic.SymbolManager;
import midend.semantic.VariableSymbol;

public class IrLValVisitor {
    public static IrValue visitLValValue(LVal lVal){
        String lValName = lVal.getName();
        Symbol symbol = SymbolManager.getInstance().getCurrentTable().lookupWithIrValue(lValName);
        Exp indexExp = lVal.getIndexExp();
        if(!(symbol instanceof VariableSymbol variableSymbol)) throw new RuntimeException("LVal is not a variable" + lValName);
        if(!variableSymbol.isArray()){
            return new LoadInstr(variableSymbol.getIrValue());
        } else {
            IrValue pointer = variableSymbol.getIrValue();
            IrPointerType irPointerType = (IrPointerType) pointer.getIrType();

            // 如果会产生二维数组那么找到首元素的指针
            if(irPointerType.getTargetType() instanceof IrPointerType) {
                pointer = new LoadInstr(pointer);
            }

            if(indexExp != null) {
                GepInstr gepInstr = new GepInstr(pointer,IrExpVisitor.visitExp(indexExp));
                return new LoadInstr(gepInstr);
            } else {
                return new GepInstr(pointer,new IrConstInt(0));
            }
        }
    }

    public static IrValue visitLValAddress(LVal lVal){
        String lValName = lVal.getName();
        Symbol symbol = SymbolManager.getInstance().getCurrentTable().lookupWithIrValue(lValName);
        Exp indexExp = lVal.getIndexExp();
        if(!(symbol instanceof VariableSymbol variableSymbol)) throw new RuntimeException("LVal is not a variable" + lValName);
        if(!variableSymbol.isArray()){
            return variableSymbol.getIrValue();
        } else {
            IrValue pointer = variableSymbol.getIrValue();
            IrPointerType irPointerType = (IrPointerType) pointer.getIrType();
            if(irPointerType.getTargetType() instanceof IrPointerType) {
                pointer = new LoadInstr(pointer);
            }
            return new GepInstr(pointer,IrExpVisitor.visitExp(indexExp));
        }
    }
}
