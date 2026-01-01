package optimize;

import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.Instr;
import midend.llvm.instr.memory.AllocateInstr;
import midend.llvm.instr.memory.LoadInstr;
import midend.llvm.instr.memory.StoreInstr;
import midend.llvm.instr.phi.PhiInstr;
import midend.llvm.use.IrUse;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

/**
 * InsertPhi 类负责为单个 AllocateInstr 插入 Phi 指令
 * 并完成变量重命名（将 load/store 转换为直接使用值）
 */
public class InsertPhi {
    private final AllocateInstr allocateInstr;
    private final IrBasicBlock entryBlock;
    private final HashSet<Instr> defineInstrs; // 定义该变量的指令集合（store 和 phi）
    private final HashSet<Instr> useInstrs; // 使用该变量的指令集合（load 和 phi）
    private final ArrayList<IrBasicBlock> defineBlocks; // 包含定值的基本块
    private final ArrayList<IrBasicBlock> useBlocks; // 包含使用的基本块
    private Stack<IrValue> valueStack; // 用于 DFS 重命名时追踪当前值

    public InsertPhi(AllocateInstr allocateInstr, IrBasicBlock entryBlock) {
        this.allocateInstr = allocateInstr;
        this.entryBlock = entryBlock;
        this.defineInstrs = new HashSet<>();
        this.useInstrs = new HashSet<>();
        this.defineBlocks = new ArrayList<>();
        this.useBlocks = new ArrayList<>();
        this.valueStack = new Stack<>();
    }

    /**
     * 执行 Phi 插入和变量重命名
     */
    public void addPhi() {
        // 1. 分析该 allocateInstr 的 define 和 use 关系
        this.buildDefineUseRelationship();
        // 2. 找出需要添加 phi 指令的基本块，并添加 phi
        this.insertPhiToBlock();
        // 3. 通过 DFS 进行重命名，同时将相关的 allocate、store、load 指令删除
        this.convertLoadStore(this.entryBlock);
    }

    /**
     * 构建变量的 define-use 关系
     */
    private void buildDefineUseRelationship() {
        // 所有使用该 allocate 的 user
        for (IrUse irUse : this.allocateInstr.getBeUsedList()) {
            Instr userInstr = (Instr) irUse.getUser();
            // load 关系为 use 关系
            if (userInstr instanceof LoadInstr) {
                this.addUseInstr(userInstr);
            }
            // store 关系为 define 关系
            else if (userInstr instanceof StoreInstr) {
                this.addDefineInstr(userInstr);
            }
        }
    }

    /**
     * 添加定值指令
     */
    private void addDefineInstr(Instr instr) {
        this.defineInstrs.add(instr);
        if (!this.defineBlocks.contains(instr.getBlock())) {
            this.defineBlocks.add(instr.getBlock());
        }
    }

    /**
     * 添加使用指令
     */
    private void addUseInstr(Instr instr) {
        this.useInstrs.add(instr);
        if (!this.useBlocks.contains(instr.getBlock())) {
            this.useBlocks.add(instr.getBlock());
        }
    }

    /**
     * 在支配边界处插入 Phi 指令
     */
    private void insertPhiToBlock() {
        // 需要添加 phi 的基本块的集合
        HashSet<IrBasicBlock> addedPhiBlocks = new HashSet<>();

        // 定义变量的基本块的集合
        Stack<IrBasicBlock> defineBlockStack = new Stack<>();
        for (IrBasicBlock defineBlock : this.defineBlocks) {
            defineBlockStack.push(defineBlock);
        }

        while (!defineBlockStack.isEmpty()) {
            IrBasicBlock defineBlock = defineBlockStack.pop();
            // 遍历当前基本块的支配边界
            for (IrBasicBlock frontierBlock : defineBlock.getDominateFrontier()) {
                // 如果支配边界块不在已添加集合中，则添加 phi
                if (!addedPhiBlocks.contains(frontierBlock)) {
                    this.insertPhiInstr(frontierBlock);
                    addedPhiBlocks.add(frontierBlock);
                    // phi 也进行定值操作
                    if (!this.defineBlocks.contains(frontierBlock)) {
                        defineBlockStack.push(frontierBlock);
                    }
                }
            }
        }
    }

    /**
     * 在指定基本块中插入 Phi 指令
     */
    private void insertPhiInstr(IrBasicBlock irBasicBlock) {
        PhiInstr phiInstr = new PhiInstr(this.allocateInstr.getTargetType(), irBasicBlock);
        irBasicBlock.addInstrAtIndex(phiInstr, 0);
        // phi 既是 define，又是 use
        this.useInstrs.add(phiInstr);
        this.defineInstrs.add(phiInstr);
    }

    /**
     * 通过 DFS 遍历支配树进行 load/store 转换
     */
    private void convertLoadStore(IrBasicBlock renameBlock) {
        @SuppressWarnings("unchecked")
        final Stack<IrValue> stackCopy = (Stack<IrValue>) this.valueStack.clone();

        // 移除与当前 allocate 相关的全部 load、store 指令
        this.removeBlockLoadStore(renameBlock);

        // 遍历当前块的后继集合，将最新的 define 填充进每个后继块的 phi 指令中
        this.convertPhiValue(renameBlock);

        // 对直接被当前块支配的块进行 DFS
        for (IrBasicBlock dominateBlock : renameBlock.getDirectDominateBlocks()) {
            this.convertLoadStore(dominateBlock);
        }

        // 恢复栈
        this.valueStack = stackCopy;
    }

    /**
     * 移除基本块中与当前 allocate 相关的 load/store 指令
     */
    private void removeBlockLoadStore(IrBasicBlock visitBlock) {
        Iterator<Instr> iterator = visitBlock.getInstructions().iterator();
        while (iterator.hasNext()) {
            Instr instr = iterator.next();
            // store：将值压入栈中，并移除指令
            if (instr instanceof StoreInstr storeInstr && this.defineInstrs.contains(instr)) {
                this.valueStack.push(storeInstr.getBury());
                storeInstr.removeAllValueUse();
                iterator.remove();
            }
            // load（非 phi）：将使用该 load 的地方替换为栈顶值
            else if (!(instr instanceof PhiInstr) && this.useInstrs.contains(instr)) {
                instr.modifyAllUsersToNewValue(this.peekValueStack());
                instr.removeAllValueUse();
                iterator.remove();
            }
            // phi：将 phi 的结果压入栈中
            else if (instr instanceof PhiInstr && this.defineInstrs.contains(instr)) {
                this.valueStack.push(instr);
            }
            // 当前分析的 allocate：使用 mem2reg 后不需要 allocate
            else if (instr == this.allocateInstr) {
                iterator.remove();
            }
        }
    }

    /**
     * 转换后继块中 phi 指令的值
     */
    private void convertPhiValue(IrBasicBlock visitBlock) {
        for (IrBasicBlock nextBlock : visitBlock.getNextBlocks()) {
            // 遍历后继块的所有指令，寻找属于当前变量的 Phi 指令
            for (Instr instr : nextBlock.getInstructions()) {
                if (instr instanceof PhiInstr phiInstr && this.useInstrs.contains(phiInstr)) {
                    phiInstr.convertBlockToValue(this.peekValueStack(), visitBlock);
                    break; // 每个变量在每个块只会有一个 Phi
                }
                // Phi 指令总是在块的开头，一旦遇到非 Phi 指令就可以停止
                if (!(instr instanceof PhiInstr)) {
                    break;
                }
            }
        }
    }

    /**
     * 查看值栈顶，如果为空则返回默认值 0
     */
    private IrValue peekValueStack() {
        return this.valueStack.isEmpty() ? new IrConstInt(0) : this.valueStack.peek();
    }
}
