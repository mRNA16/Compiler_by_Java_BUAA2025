package midend.llvm.value;

public class IrLoop {
    private final IrBasicBlock condBlock;
    private final IrBasicBlock bodyBlock;
    private final IrBasicBlock stepBlock;
    private final IrBasicBlock followBlock;

    public IrLoop(IrBasicBlock condBlock, IrBasicBlock bodyBlock,
                  IrBasicBlock stepBlock, IrBasicBlock followBlock) {
        this.condBlock = condBlock;
        this.bodyBlock = bodyBlock;
        this.stepBlock = stepBlock;
        this.followBlock = followBlock;
    }

    public IrBasicBlock getCondBlock() {
        return this.condBlock;
    }

    public IrBasicBlock getBodyBlock() {
        return this.bodyBlock;
    }

    public IrBasicBlock getStepBlock() {
        return this.stepBlock;
    }

    public IrBasicBlock getFollowBlock() {
        return this.followBlock;
    }
}
