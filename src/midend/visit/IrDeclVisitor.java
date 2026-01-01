package midend.visit;

import frontend.ast.*;
import midend.llvm.IrBuilder;
import midend.llvm.constant.IrConstArray;
import midend.llvm.constant.IrConstInt;
import midend.llvm.constant.IrConstant;
import midend.llvm.instr.memory.AllocateInstr;
import midend.llvm.instr.memory.GepInstr;
import midend.llvm.instr.memory.StoreInstr;
import midend.llvm.type.IrArrayType;
import midend.llvm.type.IrPointerType;
import midend.llvm.type.IrType;
import midend.llvm.value.IrGlobalValue;
import midend.llvm.value.IrValue;
import midend.semantic.Symbol;
import midend.semantic.SymbolManager;
import midend.semantic.SymbolType;
import midend.semantic.VariableSymbol;
import utils.ConstExpEvaluator;
import utils.IrTypeConverter;
import utils.ParserWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IrDeclVisitor {
    public static void visitDecl(Decl decl,boolean isGlobal) {
        if(decl instanceof ConstDecl constDecl){
            List<ConstDef> constDefs= constDecl.getConstDefs();
            for(ConstDef constDef : constDefs){
                visitConstDef(constDef,isGlobal);
            }
        }
        else if (decl instanceof VarDecl varDecl){
            List<VarDef> varDefs= varDecl.getVarDefs();
            for(VarDef varDef : varDefs){
                visitVarDef(varDef,isGlobal);
            }
        }
    }

    public static void visitConstDef(ConstDef constDef,boolean isGlobal){
        String constName = constDef.getName();
        Symbol symbol = SymbolManager.getInstance().getCurrentTable().lookup(constName);

        if(!(symbol instanceof VariableSymbol varSymbol)) throw new RuntimeException("Symbol is not a variable" + constName);
        IrType irType = IrTypeConverter.symbolType2IrType(varSymbol.getType(),varSymbol.getElementCount());

        if(isGlobal) {
            IrGlobalValue irGlobalValue = IrBuilder.getNewGlobalValueIr(new IrPointerType(irType),getConstInitValue(varSymbol));
            varSymbol.setIrValue(irGlobalValue);
        } else {
            // 按照全局变量进行处理即可，由作用域——符号表——符号——Ir这一串对应关系保证作用域
            if(varSymbol.isStatic()){
                IrGlobalValue irGlobalValue = IrBuilder.getNewGlobalValueIr(new IrPointerType(irType),getConstInitValue(varSymbol));
                varSymbol.setIrValue(irGlobalValue);
            } else {
                AllocateInstr allocateInstr = new AllocateInstr(irType);
                varSymbol.setIrValue(allocateInstr);

                IrConstant init = getConstInitValue(varSymbol);
                if(!varSymbol.isArray()) {
                    StoreInstr storeInstr = new StoreInstr(init,allocateInstr);
                } else {
                    int size = varSymbol.getElementCount();
                    List<Integer> initIntegers = getInitIntegerList(varSymbol);
                    for(int i = 0; i < size; i++){
                        GepInstr gepInstr = new GepInstr(allocateInstr,new IrConstInt(i));
                        IrValue initValue = new IrConstInt(initIntegers.get(i));
                        StoreInstr storeInstr = new StoreInstr(initValue,gepInstr);
                    }
                }
            }
        }
    }

    public static void visitVarDef(VarDef varDef,boolean isGlobal){
        String varName = varDef.getName();
        Symbol symbol = SymbolManager.getInstance().getCurrentTable().lookup(varName);

        if(!(symbol instanceof VariableSymbol varSymbol)) throw new RuntimeException("Symbol is not a variable " + varName);
        IrType irType = IrTypeConverter.symbolType2IrType(varSymbol.getType(),varSymbol.getElementCount());

        if(isGlobal) {
            IrGlobalValue irGlobalValue = IrBuilder.getNewGlobalValueIr(new IrPointerType(irType),getConstInitValue(varSymbol));
            varSymbol.setIrValue(irGlobalValue);
        } else {
            // 按照全局变量进行处理即可，由作用域——符号表——符号——Ir这一串对应关系保证作用域
            if(varSymbol.isStatic()){
                IrGlobalValue irGlobalValue = IrBuilder.getNewGlobalValueIr(new IrPointerType(irType),getConstInitValue(varSymbol));
                varSymbol.setIrValue(irGlobalValue);
            } else {
                AllocateInstr allocateInstr = new AllocateInstr(irType);
                varSymbol.setIrValue(allocateInstr);

                InitVal initVal = varDef.getInitVal();
                List<Exp> exps = (initVal!=null&&initVal.hasExp())?initVal.getExpList():null;
                if(!varSymbol.isArray()) {
                    if(varDef.hasInitVal()){
                        Exp exp = exps.get(0);
                        IrValue irValue = IrExpVisitor.visitExp(exp);
                        StoreInstr storeInstr = new StoreInstr(irValue,allocateInstr);
                    }
                } else {
                    if(varDef.hasInitVal()&&exps!=null){
                        for(int i = 0; i < exps.size(); i++){
                            Exp exp = exps.get(i);
                            IrValue irValue = IrExpVisitor.visitExp(exp);
                            GepInstr gepInstr = new GepInstr(allocateInstr,new IrConstInt(i));
                            StoreInstr storeInstr = new StoreInstr(irValue,gepInstr);
                        }
                    }
                }
            }
        }
    }

    private static IrConstant getConstInitValue(VariableSymbol varSymbol){
        List<Integer> dimensions = varSymbol.getDimensions();
        Object initObj = varSymbol.getInitialValue();
        ConstInitVal constInitVal;
        if(initObj instanceof InitVal initVal) {
            constInitVal = new ConstInitVal();
            List<Exp> exps = initVal.getExpList();
            for(Exp exp : exps){
                if(exp instanceof ParserWrapper.ExpWrapper wrapper){
                    ConstExp constExp = new ConstExp();
                    constExp.setAddExp((AddExp) wrapper.getWrappedNode());
                    constInitVal.addConstExp(constExp);
                }
            }
        } else {
            constInitVal = (ConstInitVal) initObj;
        }

        List<ConstExp> constExps = (constInitVal!=null)?constInitVal.getConstExpList():null;
        if(dimensions.isEmpty()){
            int v = 0;
            if(constExps!=null&&!constExps.isEmpty()){
                v = ConstExpEvaluator.eval(constExps.get(0));
            }
            return new IrConstInt(v);
        } else {
            int size = varSymbol.getElementCount();
            SymbolType symbolType = varSymbol.getType();
            IrArrayType irArrayType = (IrArrayType) IrTypeConverter.symbolType2IrType(symbolType,size);
            IrType elementType = irArrayType.getElementType();
            List<IrConstant> inits = new ArrayList<>();
            for(int i = 0; i < size; i++){
                int v = (constExps!=null&&i<constExps.size())?ConstExpEvaluator.eval(constExps.get(i)):0;
                inits.add(new IrConstInt(v));
            }
            return new IrConstArray(size,elementType,varSymbol.getName(),inits);
        }
    }

    private static List<Integer> getInitIntegerList(VariableSymbol varSymbol) {
        List<Integer> dimensions = varSymbol.getDimensions();
        ConstInitVal constInitVal = (ConstInitVal) varSymbol.getInitialValue();
        List<ConstExp> constExps = constInitVal.getConstExpList();
        List<Integer> ans = new ArrayList<>();
        if(dimensions.isEmpty()){
            ans.add(ConstExpEvaluator.eval(constExps.get(0)));
        } else {
            int size = varSymbol.getElementCount();
            for(int i = 0; i < size; i++){
                ans.add((i<constExps.size())?ConstExpEvaluator.eval(constExps.get(i)):0);
            }
        }
        return ans;
    }
}
