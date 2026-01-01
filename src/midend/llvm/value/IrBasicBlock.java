package midend.llvm.value;

import midend.llvm.instr.Instr;
import midend.llvm.instr.ctrl.BrCondInstr;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.instr.ctrl.ReturnInstr;
import midend.llvm.type.IrBasicBlockType;

import backend.mips.assembly.MipsLabel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class IrBasicBlock extends IrValue {
    private final IrFunction function;
    private final List<Instr> instructions;

    public IrBasicBlock(String irName, IrFunction function) {
        super(IrBasicBlockType.BASIC_BLOCK, irName);
        this.function = function;
        this.instructions = new ArrayList<>();
        this.function.addBasicBlock(this);
    }

    public void addInstruction(Instr instruction) {
        this.instructions.add(instruction);
        instruction.setBlock(this);
    }

    public IrFunction getFunction() {
        return function;
    }

    public List<Instr> getInstructions() {
        return instructions;
    }

    public boolean isEmpty() {
        return instructions.isEmpty();
    }

    public boolean hasTerminator() {
        if (instructions.isEmpty()) {
            return false;
        }
        Instr last = instructions.get(instructions.size() - 1);
        return last instanceof ReturnInstr || last instanceof BrInstr || last instanceof BrCondInstr;
    }

    public Instr getLastInstr() {
        if (instructions.isEmpty()) {
            return null;
        }
        return instructions.get(instructions.size() - 1);
    }

    private final ArrayList<IrBasicBlock> beforeBlocks = new ArrayList<>();
    private final ArrayList<IrBasicBlock> nextBlocks = new ArrayList<>();
    private final HashSet<IrBasicBlock> dominatorBlocks = new HashSet<>();
    private final HashSet<IrBasicBlock> dominateFrontier = new HashSet<>();
    private final HashSet<IrBasicBlock> directDominateBlocks = new HashSet<>();
    private IrBasicBlock directDominator = null;

    public void addNextBlock(IrBasicBlock block) {
        this.nextBlocks.add(block);
    }

    public void addBeforeBlock(IrBasicBlock block) {
        this.beforeBlocks.add(block);
    }

    public ArrayList<IrBasicBlock> getNextBlocks() {
        return nextBlocks;
    }

    public ArrayList<IrBasicBlock> getBeforeBlocks() {
        return beforeBlocks;
    }

    public void addDominator(IrBasicBlock block) {
        this.dominatorBlocks.add(block);
    }

    public HashSet<IrBasicBlock> getDominatorBlocks() {
        return dominatorBlocks;
    }

    public void setDirectDominator(IrBasicBlock block) {
        this.directDominator = block;
        block.directDominateBlocks.add(this);
    }

    public IrBasicBlock getDirectDominator() {
        return directDominator;
    }

    public HashSet<IrBasicBlock> getDirectDominateBlocks() {
        return directDominateBlocks;
    }

    public void addDominateFrontier(IrBasicBlock block) {
        this.dominateFrontier.add(block);
    }

    public HashSet<IrBasicBlock> getDominateFrontier() {
        return dominateFrontier;
    }

    /**
     * 在指定索引处插入指令
     */
    public void addInstrAtIndex(Instr instr, int index) {
        this.instructions.add(index, instr);
        instr.setBlock(this);
    }

    /**
     * 获取第一条指令
     */
    public Instr getFirstInstr() {
        if (instructions.isEmpty()) {
            return null;
        }
        return instructions.get(0);
    }

    /**
     * 在跳转指令前插入新指令
     */
    public void addInstrBeforeJump(Instr instr) {
        Instr lastInstr = this.getLastInstr();
        if (lastInstr instanceof BrInstr || lastInstr instanceof BrCondInstr) {
            this.instructions.add(this.instructions.size() - 1, instr);
        } else {
            this.instructions.add(instr);
        }
        instr.setBlock(this);
    }

    /**
     * 检查是否包含 ParallelCopy 指令
     */
    public boolean hasParallelCopyInstr() {
        if (this.instructions.size() < 2) {
            return false;
        }
        return this.instructions.get(this.instructions.size() - 2) instanceof midend.llvm.instr.phi.ParallelCopyInstr;
    }

    /**
     * 获取并移除 ParallelCopy 指令
     */
    public midend.llvm.instr.phi.ParallelCopyInstr getAndRemoveParallelCopyInstr() {
        if (!hasParallelCopyInstr()) {
            return null;
        }
        midend.llvm.instr.phi.ParallelCopyInstr copyInstr = (midend.llvm.instr.phi.ParallelCopyInstr) this.instructions
                .get(this.instructions.size() - 2);
        this.instructions.remove(this.instructions.size() - 2);
        return copyInstr;
    }

    public void clearCfg() {
        this.beforeBlocks.clear();
        this.nextBlocks.clear();
        this.dominatorBlocks.clear();
        this.dominateFrontier.clear();
        this.directDominateBlocks.clear();
        this.directDominator = null;
    }

    @Override
    public String toString() {
        return irName + ":\n" + instructions.stream()
                .map(instr -> "\t" + instr.toString())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String getMipsLabel() {
        return this.irName;
    }

    @Override
    public void toMips() {
        new MipsLabel(this.getMipsLabel());
        for (Instr instr : this.instructions) {
            instr.toMips();
        }
    }
}
