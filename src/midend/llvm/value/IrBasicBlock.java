package midend.llvm.value;

import midend.llvm.instr.Instr;
import midend.llvm.instr.ctrl.ReturnInstr;
import midend.llvm.type.IrBasicBlockType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IrBasicBlock extends IrValue{
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

    public boolean isEmpty(){
        return instructions.isEmpty();
    }

    public boolean hasTerminator(){
        if(instructions.isEmpty()){
            return false;
        }
        return instructions.get(instructions.size()-1) instanceof ReturnInstr;
    }

    @Override
    public String toString() {
        return irName + ":\n" + instructions.stream()
                                .map(instr -> "\t" + instr.toString())
                                .collect(Collectors.joining("\n"));
    }
}
