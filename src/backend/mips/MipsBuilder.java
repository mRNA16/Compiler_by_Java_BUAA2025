package backend.mips;

import backend.mips.assembly.MipsAssembly;
import backend.mips.assembly.data.MipsDataAssembly;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrParameter;
import midend.llvm.value.IrValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MipsBuilder {
    private static MipsModule currentModule = null;
    // value-register分配表
    private static HashMap<IrValue, Register> valueRegisterMap = null;
    // 函数栈偏移量分配表
    private static int stackOffset = 0;
    private static HashMap<IrValue, Integer> stackOffsetValueMap = null;

    public static void setBackEndModule(MipsModule mipsModule) {
        currentModule = mipsModule;
    }

    public static MipsModule getCurrentModule() {
        return currentModule;
    }

    public static void addAssembly(MipsAssembly mipsAssembly) {
        if (mipsAssembly instanceof MipsDataAssembly) {
            currentModule.addToData(mipsAssembly);
        } else {
            currentModule.addToText(mipsAssembly);
        }
    }

    public static void setCurrentFunction(IrFunction irFunction) {
        // 设置相应的寄存器分配表
        valueRegisterMap = irFunction.getValueRegisterMap();
        // 初始化栈分配表
        stackOffset = 0;
        stackOffsetValueMap = new HashMap<>();
    }

    public static Register getValueToRegister(IrValue irValue) {
        return valueRegisterMap.get(irValue);
    }

    public static void allocateRegForParam(IrParameter irParameter, Register register) {
        valueRegisterMap.put(irParameter, register);
    }

    public static ArrayList<Register> getAllocatedRegList() {
        return new ArrayList<>(new HashSet<>(valueRegisterMap.values()));
    }

    public static int getCurrentStackOffset() {
        return stackOffset;
    }

    public static Integer getStackValueOffset(IrValue irValue) {
        return stackOffsetValueMap.get(irValue);
    }

    public static Integer allocateStackForValue(IrValue irValue) {
        Integer address = stackOffsetValueMap.get(irValue);
        if (address == null) {
            stackOffset -= 4;
            stackOffsetValueMap.put(irValue, stackOffset);
            address = stackOffset;
        }

        return address;
    }

    public static void allocateStackSpace(int offset) {
        stackOffset -= offset;
    }
}
