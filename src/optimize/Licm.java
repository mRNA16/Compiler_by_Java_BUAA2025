package optimize;

import midend.llvm.instr.Instr;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.instr.ctrl.BrCondInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.llvm.instr.memory.LoadInstr;
import midend.llvm.instr.memory.StoreInstr;
import midend.llvm.instr.ctrl.CallInstr;
import midend.llvm.instr.io.IOInstr;

import java.util.*;

/**
 * 循环不变代码外提 (LICM) 优化
 */
public class Licm extends Optimizer {
    private static class NaturalLoop {
        IrBasicBlock header;
        Set<IrBasicBlock> blocks;
        IrBasicBlock preHeader;

        NaturalLoop(IrBasicBlock header) {
            this.header = header;
            this.blocks = new HashSet<>();
            this.blocks.add(header);
        }
    }

    @Override
    public void optimize() {
        for (IrFunction function : irModule.getFunctions()) {
            if (function.getBasicBlocks().isEmpty())
                continue;

            // 1. 发现自然循环
            List<NaturalLoop> loops = findNaturalLoops(function);

            // 2. 按照循环深度排序（从内向外处理）
            // 简单的启发式：块数越少的循环通常越靠内
            loops.sort(Comparator.comparingInt(l -> l.blocks.size()));

            for (NaturalLoop loop : loops) {
                processLoop(loop);
            }
        }
    }

    private List<NaturalLoop> findNaturalLoops(IrFunction function) {
        List<NaturalLoop> loops = new ArrayList<>();
        for (IrBasicBlock n : function.getBasicBlocks()) {
            for (IrBasicBlock d : n.getNextBlocks()) {
                // 如果 d 支配 n，则 (n, d) 是一条回边
                if (n.getDominatorBlocks().contains(d)) {
                    NaturalLoop loop = buildNaturalLoop(d, n);
                    // 合并具有相同 header 的循环
                    boolean merged = false;
                    for (NaturalLoop existing : loops) {
                        if (existing.header == d) {
                            existing.blocks.addAll(loop.blocks);
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        loops.add(loop);
                    }
                }
            }
        }
        return loops;
    }

    private NaturalLoop buildNaturalLoop(IrBasicBlock header, IrBasicBlock tail) {
        NaturalLoop loop = new NaturalLoop(header);
        Stack<IrBasicBlock> stack = new Stack<>();
        if (tail != header) {
            loop.blocks.add(tail);
            stack.push(tail);
        }

        while (!stack.isEmpty()) {
            IrBasicBlock m = stack.pop();
            for (IrBasicBlock p : m.getBeforeBlocks()) {
                if (!loop.blocks.contains(p)) {
                    loop.blocks.add(p);
                    stack.push(p);
                }
            }
        }
        return loop;
    }

    private void processLoop(NaturalLoop loop) {
        // 1. 确保有 Pre-header
        ensurePreHeader(loop);

        // 2. 寻找循环不变指令
        List<Instr> invariants = findInvariants(loop);

        // 3. 移动指令到 Pre-header
        for (Instr instr : invariants) {
            moveInstrToPreHeader(instr, loop.preHeader);
        }
    }

    private void ensurePreHeader(NaturalLoop loop) {
        IrBasicBlock header = loop.header;
        List<IrBasicBlock> externalPreds = new ArrayList<>();
        for (IrBasicBlock pred : header.getBeforeBlocks()) {
            if (!loop.blocks.contains(pred)) {
                externalPreds.add(pred);
            }
        }

        // 如果只有一个外部前驱，且该前驱只有一个后继（即 header），则它就是 pre-header
        if (externalPreds.size() == 1) {
            IrBasicBlock pred = externalPreds.get(0);
            if (pred.getNextBlocks().size() == 1) {
                loop.preHeader = pred;
                return;
            }
        }

        // 否则，创建一个新的 Pre-header
        IrFunction func = header.getFunction();
        IrBasicBlock preHeader = new IrBasicBlock(header.getIrName() + "_pre", func);
        // 将 preHeader 插入到 header 之前（为了保持代码顺序，虽然不强制）
        int headerIndex = func.getBasicBlocks().indexOf(header);
        func.getBasicBlocks().remove(preHeader); // 构造函数会自动加到末尾，先移除
        func.getBasicBlocks().add(headerIndex, preHeader);

        // 修改外部前驱的跳转目标
        for (IrBasicBlock pred : externalPreds) {
            Instr last = pred.getLastInstr();
            if (last instanceof BrInstr br) {
                br.setTargetBlock(preHeader);
            } else if (last instanceof BrCondInstr brCond) {
                if (brCond.getSucBlock() == header)
                    brCond.setSucBlock(preHeader);
                if (brCond.getFailBlock() == header)
                    brCond.setFailBlock(preHeader);
            }
            // 更新 CFG
            pred.getNextBlocks().remove(header);
            pred.getNextBlocks().add(preHeader);
            header.getBeforeBlocks().remove(pred);
            preHeader.addBeforeBlock(pred);
        }

        // Pre-header 跳转到 Header
        preHeader.addInstruction(new BrInstr(header, preHeader));
        preHeader.addNextBlock(header);
        header.addBeforeBlock(preHeader);

        loop.preHeader = preHeader;
    }

    private List<Instr> findInvariants(NaturalLoop loop) {
        List<Instr> invariants = new ArrayList<>();
        Set<IrValue> loopDefinedValues = new HashSet<>();
        for (IrBasicBlock block : loop.blocks) {
            for (Instr instr : block.getInstructions()) {
                if (!instr.getIrType().isVoidType()) {
                    loopDefinedValues.add(instr);
                }
            }
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (IrBasicBlock block : loop.blocks) {
                for (Instr instr : block.getInstructions()) {
                    if (invariants.contains(instr))
                        continue;
                    if (!isMovable(instr, loop))
                        continue;

                    boolean isInvariant = true;
                    for (IrValue operand : instr.getUseValueList()) {
                        if (loopDefinedValues.contains(operand)) {
                            // 如果操作数在循环内定义，检查它是否已经是 invariant
                            if (!invariants.contains(operand)) {
                                isInvariant = false;
                                break;
                            }
                        }
                    }

                    if (isInvariant) {
                        invariants.add(instr);
                        changed = true;
                    }
                }
            }
        }
        return invariants;
    }

    private boolean isMovable(Instr instr, NaturalLoop loop) {
        // 只有无副作用的计算指令可以移动
        // 排除：Store, Call (可能有副作用), IO, Return, Br, Phi
        if (instr instanceof StoreInstr || instr instanceof CallInstr ||
                instr instanceof IOInstr || instr instanceof BrInstr ||
                instr instanceof BrCondInstr || instr instanceof midend.llvm.instr.phi.PhiInstr) {
            return false;
        }

        if (instr instanceof LoadInstr load) {
            return isMemoryInvariant(load, loop);
        }

        // CalculateInstr, GepInstr, ZextInstr, TruncInstr, ICompInstr 都是可以移动的
        return !instr.getIrType().isVoidType();
    }

    private boolean isMemoryInvariant(LoadInstr load, NaturalLoop loop) {
        IrValue ptr = load.getPointer();

        // 检查循环内是否有冲突的写操作
        for (IrBasicBlock block : loop.blocks) {
            for (Instr instr : block.getInstructions()) {
                if (instr instanceof StoreInstr store) {
                    if (mayAlias(ptr, store.getAddress())) {
                        return false; // 可能被修改，不能外提
                    }
                } else if (instr instanceof CallInstr call) {
                    // 假设外部函数调用会修改所有可能的内存
                    // 除非确定该函数是无副作用的
                    if (!isSideEffectFree(call)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean mayAlias(IrValue p1, IrValue p2) {
        if (p1.equals(p2))
            return true;

        // 获取指针的根源（哪个 alloca 或 global）
        IrValue root1 = getRootPointer(p1);
        IrValue root2 = getRootPointer(p2);

        // 如果根源不同，在 SysY 中一定不别名
        if (root1 != null && root2 != null && !root1.equals(root2)) {
            return false;
        }

        // 根源相同或未知，保守认为可能别名
        return true;
    }

    private IrValue getRootPointer(IrValue ptr) {
        if (ptr instanceof midend.llvm.instr.memory.AllocateInstr ||
                ptr instanceof midend.llvm.value.IrGlobalValue) {
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
                name.equals("@putarray") || name.equals("@getint") || name.equals("@getch") ||
                name.equals("@getarray");
    }

    private void moveInstrToPreHeader(Instr instr, IrBasicBlock preHeader) {
        IrBasicBlock oldBlock = instr.getBlock();
        oldBlock.getInstructions().remove(instr);
        preHeader.addInstrBeforeJump(instr);
    }
}
