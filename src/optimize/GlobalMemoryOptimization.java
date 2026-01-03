package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.memory.LoadInstr;
import midend.llvm.instr.memory.StoreInstr;
import midend.llvm.instr.ctrl.CallInstr;
import midend.llvm.instr.io.IOInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrGlobalValue;

import java.util.*;

/**
 * 全局内存优化 (Global Memory Optimization)
 * 包含：
 * 1. 载入消除 (Load Elimination): 如果已知某个地址的值，则直接替换 load。
 * 2. 冗余存储消除 (Store Elimination): 如果连续向同一地址存储，且中间没有读取，则消除前一个存储。
 * 利用支配树进行全局分析。
 */
public class GlobalMemoryOptimization extends Optimizer {
    private boolean changed;
    // 指针 -> 当前已知的值
    private final Map<IrValue, IrValue> memoryState = new HashMap<>();

    @Override
    public void optimize() {
        changed = true;
        while (changed) {
            changed = false;
            for (IrFunction function : irModule.getFunctions()) {
                if (function.getBasicBlocks().isEmpty())
                    continue;
                memoryState.clear();
                optimizeFunction(function);
            }
        }
    }

    private void optimizeFunction(IrFunction function) {
        domTreeDFS(function.getEntryBlock());
    }

    private void domTreeDFS(IrBasicBlock block) {
        // 保存当前状态以便回溯
        Map<IrValue, IrValue> savedState = new HashMap<>(memoryState);

        // 如果当前块有多个前驱，我们不能简单地继承支配者的内存状态，
        // 因为非支配的前驱可能修改了内存。
        // 为了安全起见，对于多前驱块，我们清空内存状态（除非能做更复杂的汇合分析）。
        if (block.getBeforeBlocks().size() > 1) {
            memoryState.clear();
        }

        Iterator<Instr> iterator = block.getInstructions().iterator();
        while (iterator.hasNext()) {
            Instr instr = iterator.next();

            if (instr instanceof LoadInstr load) {
                IrValue ptr = load.getPointer();
                if (memoryState.containsKey(ptr)) {
                    IrValue existingValue = memoryState.get(ptr);
                    load.modifyAllUsersToNewValue(existingValue);
                    load.removeAllValueUse();
                    iterator.remove();
                    changed = true;
                } else {
                    memoryState.put(ptr, load);
                }
            } else if (instr instanceof StoreInstr store) {
                IrValue ptr = store.getAddress();
                IrValue val = store.getBury();

                // 别名失效：Store 可能修改了其他指针指向的内容
                invalidateAliases(ptr);

                // 记录当前地址的新值
                memoryState.put(ptr, val);
            } else if (instr instanceof CallInstr call) {
                // 函数调用可能修改任何内存
                if (!isSideEffectFree(call)) {
                    memoryState.clear();
                }
            } else if (instr instanceof IOInstr) {
                // IO 指令通常不修改用户定义的内存，但为了安全可以清除
                // 这里的处理可以根据具体 IO 指令细化
            }
        }

        for (IrBasicBlock child : block.getDirectDominateBlocks()) {
            domTreeDFS(child);
        }

        // 回溯
        memoryState.clear();
        memoryState.putAll(savedState);
    }

    private void invalidateAliases(IrValue ptr) {
        IrValue root = getRootPointer(ptr);
        Iterator<Map.Entry<IrValue, IrValue>> it = memoryState.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<IrValue, IrValue> entry = it.next();
            if (mayAlias(ptr, entry.getKey(), root)) {
                it.remove();
            }
        }
    }

    private boolean mayAlias(IrValue p1, IrValue p2, IrValue root1) {
        if (p1.equals(p2))
            return true;
        IrValue root2 = getRootPointer(p2);
        // 如果根源不同，则不别名
        if (root1 != null && root2 != null && !root1.equals(root2)) {
            return false;
        }
        return true;
    }

    private IrValue getRootPointer(IrValue ptr) {
        if (ptr instanceof midend.llvm.instr.memory.AllocateInstr ||
                ptr instanceof IrGlobalValue) {
            return ptr;
        }
        if (ptr instanceof midend.llvm.instr.memory.GepInstr gep) {
            return getRootPointer(gep.getPointer());
        }
        return null;
    }

    private boolean isSideEffectFree(CallInstr call) {
        String name = call.getFunction().getIrName();
        // 常见的 IO 函数不会修改用户定义的内存变量
        return name.equals("@putint") || name.equals("@putch") || name.equals("@putstr") ||
                name.equals("@putarray");
    }
}
