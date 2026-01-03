package backend.mips;

import backend.mips.assembly.MipsAssembly;
import backend.mips.assembly.data.MipsDataAssembly;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrParameter;
import midend.llvm.value.IrValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MipsBuilder {
    private static MipsModule currentModule = null;
    // value-register分配表
    private static HashMap<IrValue, Register> valueRegisterMap = null;
    // 函数栈偏移量分配表
    private static int stackOffset = 0;
    private static int frameSize = 0;
    private static HashMap<IrValue, Integer> stackOffsetValueMap = null;
    private static HashMap<IrValue, Integer> allocaDataMap = null;
    private static List<Register> registersNeedSaveList = null;

    private static int raOffset = 0;
    private static int regSaveOffset = 0;
    private static IrFunction currentFunction = null;

    public static void setBackEndModule(MipsModule mipsModule) {
        currentModule = mipsModule;
    }

    public static MipsModule getCurrentModule() {
        return currentModule;
    }

    private static boolean autoAdd = true;

    public static void setAutoAdd(boolean value) {
        autoAdd = value;
    }

    public static void addAssembly(MipsAssembly mipsAssembly) {
        if (!autoAdd)
            return;
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

    public static IrFunction getCurrentFunction() {
        return currentFunction;
    }

    private static void preAllocateFrame(IrFunction irFunction) {
        stackOffset = 0;
        stackOffsetValueMap = new HashMap<>();
        allocaDataMap = new HashMap<>();

        // 1. 预留 RA 空间 (放在栈顶，Old SP 下面)
        if (!irFunction.isLeafFunction()) {
            stackOffset -= 4;
            raOffset = stackOffset;
        } else {
            raOffset = 0;
        }

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
                } else if (instr instanceof midend.llvm.instr.MoveInstr moveInstr) {
                    // MoveInstr 的目标值需要分配栈空间（来自 Phi 指令）
                    IrValue dstValue = moveInstr.getDstValue();
                    if (valueRegisterMap.get(dstValue) == null && !stackOffsetValueMap.containsKey(dstValue)) {
                        allocateStackForValue(dstValue);
                    }
                } else if (!instr.getIrType().isVoidType() && valueRegisterMap.get(instr) == null) {
                    allocateStackForValue(instr);
                }
            }
        }

        // 3. 为进入参数分配空间 (Incoming parameters)
        for (int i = 0; i < Math.min(4, irFunction.getParameters().size()); i++) {
            allocateStackForValue(irFunction.getParameters().get(i));
        }

        // 4. 预留寄存器保存空间 (Caller-saved & Callee-saved registers)
        HashSet<Register> registersNeedSave = new HashSet<>();
        for (midend.llvm.value.IrBasicBlock block : irFunction.getBasicBlocks()) {
            for (midend.llvm.instr.Instr instr : block.getInstructions()) {
                if (instr instanceof midend.llvm.instr.ctrl.CallInstr) {
                    HashSet<midend.llvm.value.IrValue> liveValues = block.getLiveValuesAt(instr);
                    for (midend.llvm.value.IrValue val : liveValues) {
                        Register reg = valueRegisterMap.get(val);
                        if (reg != null && isCallerSaved(reg)) {
                            registersNeedSave.add(reg);
                        }
                    }
                    // 额外逻辑：如果参数来源是 $a 寄存器，必须预留空间以防覆盖
                    if (instr instanceof midend.llvm.instr.ctrl.CallInstr callInstr) {
                        for (IrValue arg : callInstr.getArgs()) {
                            Register reg = valueRegisterMap.get(arg);
                            if (reg != null && reg.ordinal() >= Register.A0.ordinal()
                                    && reg.ordinal() <= Register.A3.ordinal()) {
                                registersNeedSave.add(reg);
                            }
                        }
                    }
                } else if (instr instanceof midend.llvm.instr.io.IOInstr) {
                    // 对于 IO 指令，我们只担心 $v0 和 $a0 被覆盖
                    HashSet<midend.llvm.value.IrValue> liveValues = block.getLiveValuesAt(instr);
                    for (midend.llvm.value.IrValue val : liveValues) {
                        Register reg = valueRegisterMap.get(val);
                        if (reg == Register.V0 || reg == Register.A0) {
                            registersNeedSave.add(reg);
                        }
                    }
                }
            }
        }
        // 关键改进：添加所有被使用的 Callee-Saved 寄存器到保存列表
        for (Register reg : valueRegisterMap.values()) {
            if (isCalleeSaved(reg)) {
                registersNeedSave.add(reg);
            }
        }

        registersNeedSaveList = new ArrayList<>(registersNeedSave);
        registersNeedSaveList.sort((r1, r2) -> r1.ordinal() - r2.ordinal());
        regSaveOffset = stackOffset;
        stackOffset -= registersNeedSaveList.size() * 4;

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
        ArrayList<Register> list = new ArrayList<>(new HashSet<>(valueRegisterMap.values()));
        list.sort((r1, r2) -> r1.ordinal() - r2.ordinal());
        return list;
    }

    public static int getCurrentStackOffset() {
        return stackOffset;
    }

    public static Integer getStackValueOffset(IrValue irValue) {
        if (irValue instanceof IrParameter param) {
            int index = currentFunction.getParameters().indexOf(param);
            if (index >= 4) {
                // 传入参数 4+ 在调用者的栈帧中
                // 调用者将第 i 个参数存放在 (i-4)*4($sp_caller)
                return frameSize + (index - 4) * 4;
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

    public static void loadValueToReg(IrValue value, Register reg) {
        Register srcReg = getValueToRegister(value);
        if (srcReg != null) {
            if (srcReg != reg) {
                new backend.mips.assembly.fake.MarsMove(reg, srcReg);
            }
            return;
        }
        if (value instanceof midend.llvm.constant.IrConstInt constInt) {
            new backend.mips.assembly.fake.MarsLi(reg, constInt.getValue());
            return;
        }
        if (value instanceof midend.llvm.value.IrGlobalValue globalValue) {
            new backend.mips.assembly.MipsLsu(backend.mips.assembly.MipsLsu.LsuType.LW, reg,
                    globalValue.getMipsLabel());
            return;
        }
        Integer offset = getStackValueOffset(value);
        if (offset != null) {
            new backend.mips.assembly.MipsLsu(backend.mips.assembly.MipsLsu.LsuType.LW, reg, Register.SP, offset);
        }
    }

    public static void saveCurrent(List<Register> allocatedRegisterList, Set<Register> registersToSave) {
        int baseOffset = getRegSaveOffset();
        for (Register reg : registersToSave) {
            int index = registersNeedSaveList.indexOf(reg);
            if (index != -1) {
                new backend.mips.assembly.MipsLsu(backend.mips.assembly.MipsLsu.LsuType.SW,
                        reg, Register.SP, baseOffset - (index + 1) * 4);
            }
        }
    }

    public static void recoverCurrent(List<Register> allocatedRegisterList, Set<Register> registersToRestore) {
        int baseOffset = getRegSaveOffset();
        for (Register reg : registersToRestore) {
            int index = registersNeedSaveList.indexOf(reg);
            if (index != -1) {
                new backend.mips.assembly.MipsLsu(backend.mips.assembly.MipsLsu.LsuType.LW,
                        reg, Register.SP, baseOffset - (index + 1) * 4);
            }
        }
    }

    public static boolean isCalleeSaved(Register register) {
        return register.ordinal() >= Register.S0.ordinal() && register.ordinal() <= Register.S7.ordinal();
    }

    public static boolean isCallerSaved(Register register) {
        return !isCalleeSaved(register) && register != Register.ZERO && register != Register.K0
                && register != Register.K1
                && register != Register.GP && register != Register.SP && register != Register.FP
                && register != Register.RA;
    }

    public static Integer getRegisterOffset(Register register) {
        int index = registersNeedSaveList.indexOf(register);
        if (index == -1)
            return null;
        return getRegSaveOffset() - (index + 1) * 4;
    }

    public static void allocateStackSpace(int offset) {
    }
}
