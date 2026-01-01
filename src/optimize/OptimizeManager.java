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

        // 迭代优化：LVN, 常量传播, 死代码删除, 不可达代码删除
        for (int i = 0; i < 3; i++) {
            this.optimizers.add(new Lvn());
            this.optimizers.add(new ConstantPropagation());
            this.optimizers.add(new DeadCodeElimination());
            this.optimizers.add(new RemoveUnReachCode());
            this.optimizers.add(new CfgBuilder()); // 维护最新的 CFG 和支配信息
        }

        // RemovePhi 需要在所有 SSA 优化完成后执行
        this.optimizers.add(new RemovePhi());
    }

    public void optimize() {
        for (Optimizer optimizer : optimizers) {
            optimizer.optimize();
        }
    }
}
