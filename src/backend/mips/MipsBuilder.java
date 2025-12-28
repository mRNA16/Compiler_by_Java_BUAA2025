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
    private static int frameSize = 0;
    private static HashMap<IrValue, Integer> stackOffsetValueMap = null;
    private static HashMap<IrValue, Integer> allocaDataMap = null;

    private static int raOffset = 0;
    private static int regSaveOffset = 0;
    private static IrFunction currentFunction = null;

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
        currentFunction = irFunction;
        // 设置相应的寄存器分配表
        valueRegisterMap = irFunction.getValueRegisterMap();
        // 预分配栈空间
        preAllocateFrame(irFunction);
    }

    private static void preAllocateFrame(IrFunction irFunction) {
        stackOffset = 0;
        stackOffsetValueMap = new HashMap<>();
        allocaDataMap = new HashMap<>();

        // 1. 预留 RA 空间 (放在栈顶，Old SP 下面)
        stackOffset -= 4;
        raOffset = stackOffset;

        // 2. 遍历所有指令分配空间 (局部变量)
        for (midend.llvm.value.IrBasicBlock block : irFunction.getBasicBlocks()) {
            for (midend.llvm.instr.Instr instr : block.getInstructions()) {
                if (instr instanceof midend.llvm.instr.memory.AllocateInstr allocateInstr) {
                    midend.llvm.type.IrType targetType = allocateInstr.getTargetType();
                    int size = (targetType instanceof midend.llvm.type.IrArrayType arrayType)
                            ? 4 * arrayType.getArraySize()
                            : 4;
                    stackOffset -= size;
                    allocaDataMap.put(instr, stackOffset);

                    if (valueRegisterMap.get(instr) == null) {
                        allocateStackForValue(instr);
                    }
                } else if (!instr.getIrType().isVoidType() && valueRegisterMap.get(instr) == null) {
                    allocateStackForValue(instr);
                }
            }
        }

        // 3. 为进入参数分配空间 (Incoming parameters)
        // 注意：只有前 3 个参数（A1-A3）可能需要溢出到当前栈帧
        // 第 4 个及以后的参数已经在调用者的栈帧中了
        for (int i = 0; i < Math.min(3, irFunction.getParameters().size()); i++) {
            allocateStackForValue(irFunction.getParameters().get(i));
        }

        // 4. 预留寄存器保存空间 (Caller-saved registers)
        ArrayList<Register> allocatedRegs = new ArrayList<>(new HashSet<>(irFunction.getValueRegisterMap().values()));
        regSaveOffset = stackOffset;
        stackOffset -= allocatedRegs.size() * 4;

        // 5. 预留参数空间 (Outgoing arguments, 放在栈底)
        int maxArgs = 0;
        for (midend.llvm.value.IrBasicBlock block : irFunction.getBasicBlocks()) {
            for (midend.llvm.instr.Instr instr : block.getInstructions()) {
                if (instr instanceof midend.llvm.instr.ctrl.CallInstr callInstr) {
                    maxArgs = Math.max(maxArgs, callInstr.getArgs().size());
                }
            }
        }
        int argSpace = Math.max(4, maxArgs) * 4;
        stackOffset -= argSpace;

        // 6. 对齐并确定最终 frameSize
        frameSize = -stackOffset;
        if (frameSize % 8 != 0) {
            frameSize += (8 - frameSize % 8);
        }
    }

    public static int getRaOffset() {
        return raOffset + frameSize;
    }

    public static Integer getAllocaDataOffset(IrValue irValue) {
        Integer offset = allocaDataMap.get(irValue);
        if (offset == null)
            return null;
        return offset + frameSize;
    }

    public static int getRegSaveOffset() {
        return regSaveOffset + frameSize;
    }

    public static int getFrameSize() {
        return frameSize;
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
        if (irValue instanceof IrParameter param) {
            int index = currentFunction.getParameters().indexOf(param);
            if (index >= 3) {
                // 传入参数 4+ 在调用者的栈帧中
                // 调用者将第 i 个参数存放在 i*4($sp_caller)
                return frameSize + index * 4;
            }
        }
        Integer offset = stackOffsetValueMap.get(irValue);
        if (offset == null)
            return null;
        return offset + frameSize;
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
    }
}
