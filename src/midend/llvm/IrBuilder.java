package midend.llvm;

import midend.llvm.constant.IrConstString;
import midend.llvm.constant.IrConstant;
import midend.llvm.instr.Instr;
import midend.llvm.type.IrType;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrGlobalValue;
import midend.llvm.value.IrLoop;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class IrBuilder {
    // 命名前缀
    private static final String GLOBAL_VAR_PREFIX = "@g_";
    private static final String LOCAL_VAR_PREFIX = "%v";
    private static final String BLOCK_PREFIX = "b_";
    private static final String FUNC_PREFIX = "@f_";
    private static final String STRING_PREFIX = "@s_";

    // 当前状态
    private static IrModule currentModule = null;
    private static IrFunction currentFunction = null;
    private static IrBasicBlock currentBasicBlock = null;

    // 计数器
    private static int globalVarCount = 0;
    private static int basicBlockCount = 0;
    private static int stringCount = 0;
    private static final Map<IrFunction, Integer> localVarCountMap = new HashMap<>();
    private static final Stack<IrLoop> loopStack = new Stack<>();

    // 模块管理
    public static void setCurrentModule(IrModule module) {
        currentModule = module;
    }

    public static IrModule getCurrentModule() {
        return currentModule;
    }

    // 函数管理
    public static IrFunction getNewFunctionIr(String name, IrType returnType) {
        String funcIrName = name.equals("main")?"@main":FUNC_PREFIX+name;
        IrFunction newFunction = new IrFunction(funcIrName, returnType);
        currentModule.addIrFunction(newFunction);
        currentFunction = newFunction;
        currentBasicBlock = getNewBasicBlockIr(); // 这里也是入口块
        localVarCountMap.put(newFunction, 0);
        return newFunction;
    }

    public static IrFunction getCurrentFunction() {
        return currentFunction;
    }

    public static IrType getCurrentFunctionReturnType() {
        return currentFunction.getReturnType();
    }

    // 基本块管理
    public static IrBasicBlock getNewBasicBlockIr() {
        String blockIrName = BLOCK_PREFIX+basicBlockCount++;
        return new IrBasicBlock(blockIrName,currentFunction);
    }

    public static IrBasicBlock getCurrentBasicBlock() {
        return currentBasicBlock;
    }

    public static void setCurrentBasicBlock(IrBasicBlock basicBlock) {
        currentBasicBlock = basicBlock;
    }

    // 指令管理
    public static void addInstr(Instr instr) {
        currentBasicBlock.addInstruction(instr);
        instr.setBlock(currentBasicBlock);
    }

    // 全局变量管理
    public static IrGlobalValue getNewGlobalValueIr(IrType irType, IrConstant initValue){
        String globalIrName = GLOBAL_VAR_PREFIX+globalVarCount++;
        IrGlobalValue newGlobalValue = new IrGlobalValue(globalIrName,irType,initValue);
        currentModule.addIrGlobalValue(newGlobalValue);
        return newGlobalValue;
    }

    // 全局字符串管理
    public static IrConstString getNewConstStringIr(String string) {
        return currentModule.getNewConstantStringIr(string);
    }

    // 循环栈处理方法
    public static void loopStackPush(IrLoop loop) {
        loopStack.push(loop);
    }

    public static void loopStackPop() {
        loopStack.pop();
    }

    public static IrLoop loopStackPeek() {
        return loopStack.peek();
    }

    public static void skipBlankBlock() {
        currentModule.skipBlankBlock();
    }

    // 局部变量命名管理
    public static String getLocalVarNameIr(){
        int count = localVarCountMap.get(currentFunction);
        localVarCountMap.put(currentFunction, count+1);
        return LOCAL_VAR_PREFIX+count;
    }

    // 字符串命名管理
    public static String getStringNameIr(){
        return STRING_PREFIX + stringCount++;
    }
}
