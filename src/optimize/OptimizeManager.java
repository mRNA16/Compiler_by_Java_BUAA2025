package optimize;

import midend.llvm.IrModule;
import java.util.ArrayList;
import java.util.List;

public class OptimizeManager {
    private final IrModule irModule;
    private final List<Optimizer> optimizers;

    public OptimizeManager(IrModule irModule) {
        this.irModule = irModule;
        this.optimizers = new ArrayList<>();
        Optimizer.setIrModule(irModule);

        // 添加优化器
        this.optimizers.add(new RemoveUnReachCode());
        this.optimizers.add(new CfgBuilder());
        // MemToReg 需要在 CfgBuilder 构建完支配信息后执行
        this.optimizers.add(new MemToReg());
        // RemovePhi 需要在 MemToReg 之后执行，将 Phi 指令转换为 Move 指令
        this.optimizers.add(new RemovePhi());
    }

    public void optimize() {
        for (Optimizer optimizer : optimizers) {
            optimizer.optimize();
        }
    }
}
