package midend.visit;

import frontend.ast.*;
import midend.llvm.IrBuilder;
import midend.llvm.instr.memory.AllocateInstr;
import midend.llvm.instr.memory.StoreInstr;
import midend.llvm.type.IrBaseType;
import midend.llvm.type.IrType;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrParameter;
import midend.semantic.*;
import utils.IrTypeConverter;

import java.util.List;

public class IrFuncDefVisitor {
    public static void visitFuncDef(FuncDef funcDef) {
        // 获取符号
        String funcName = funcDef.getName();
        FunctionSymbol functionSymbol = (FunctionSymbol) SymbolManager.getInstance().getCurrentTable().lookup(funcName);
        if(functionSymbol == null) {
            throw new RuntimeException("No such function: " + funcName);
        }

        // 转化为Ir
        IrType returnIrType = IrTypeConverter.symbolType2IrType(functionSymbol.getReturnType(),0);
        IrFunction irFunction = IrBuilder.getNewFunctionIr(funcName, returnIrType);
        functionSymbol.setIrValue(irFunction); // 关联Symbol与Ir

        // 进入作用域
        SymbolManager.getInstance().enterScope(false);

        // 获取参数相关信息
        FuncFParams params = funcDef.getFuncFParams();
        List<String> paramsNames = functionSymbol.getParameterNames();
        List<SymbolType> paramsTypes = functionSymbol.getParameterTypes();

        // 处理参数
        for(int i = 0; i < paramsNames.size(); i++) {
            FuncFParam param = params.getParams().get(i);
            String paramName = paramsNames.get(i);
            SymbolType paramType = paramsTypes.get(i);
            VariableSymbol paramSymbol = (VariableSymbol) SymbolManager.getInstance().getCurrentTable().lookup(paramName);

            // 创建参数Ir
            IrType paramIrType = IrTypeConverter.symbolType2IrType4Param(paramType);
            IrParameter irParameter = new IrParameter(paramIrType, IrBuilder.getLocalVarNameIr());
            irFunction.addParameter(irParameter);

            AllocateInstr allocateInstr = new AllocateInstr(paramIrType);
            StoreInstr storeInstr = new StoreInstr(irParameter,allocateInstr);

            paramSymbol.setIrValue(allocateInstr); // 关联其地址
        }

        // 处理函数体
        Block block = funcDef.getBlock();
        IrBlockVisitor.visitBlock(block);

        // 保证含有返回语句
        if(!IrBuilder.getCurrentBasicBlock().hasTerminator()){
            IrBuilder.getCurrentFunction().promiseReturn();
        }

        // 退出作用域
        SymbolManager.getInstance().exitScope();
    }

    public static void visitMainFuncDef(MainFuncDef mainFunc) {
        /* 创建main函数ir
         * 由于main不进入符号表，因此不去关联
         */
        IrFunction irFunction = IrBuilder.getNewFunctionIr("main", IrBaseType.INT32);


        // 进入作用域
        SymbolManager.getInstance().enterScope(false);

        // 处理函数体
        Block block = mainFunc.getBlock();
        IrBlockVisitor.visitBlock(block);

        // 保证含有返回语句
        if (!IrBuilder.getCurrentBasicBlock().hasTerminator()) {
            IrBuilder.getCurrentFunction().promiseReturn();
        }

        // 退出作用域
        SymbolManager.getInstance().exitScope();
    }
}
