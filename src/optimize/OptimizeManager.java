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
    }

    public void optimize() {
        for (Optimizer optimizer : optimizers) {
            optimizer.optimize();
        }
    }
}
