package optimize;

import midend.llvm.IrModule;

public abstract class Optimizer {
    protected static IrModule irModule;

    public static void setIrModule(IrModule module) {
        irModule = module;
    }

    public abstract void optimize();
}
