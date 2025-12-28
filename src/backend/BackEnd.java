package backend;

import backend.mips.MipsBuilder;
import backend.mips.MipsModule;
import midend.llvm.IrModule;
import midend.llvm.constant.IrConstString;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrGlobalValue;

import java.util.Map;

public class BackEnd {
    private static IrModule irModule;

    public static void initialize(IrModule module) {
        irModule = module;
        MipsModule mipsModule = new MipsModule();
        MipsBuilder.setBackEndModule(mipsModule);
    }

    public static void generateMips() {
        irModule.toMips();
    }

    public static String getMipsCode() {
        return MipsBuilder.getCurrentModule().toString();
    }
}
