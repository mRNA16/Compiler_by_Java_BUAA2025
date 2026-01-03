package midend.llvm.instr.ctrl;

import midend.llvm.IrBuilder;
import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.llvm.constant.IrConstInt;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsJump;
import backend.mips.assembly.MipsLsu;
import backend.mips.assembly.fake.MarsMove;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CallInstr extends Instr {

    public CallInstr(IrFunction function, List<IrValue> args) {
        super(function.getReturnType(), InstrType.CALL,
                (function.getReturnType().isVoidType() ? "call" : IrBuilder.getLocalVarNameIr()));
        this.addUseValue(function);
        args.forEach(this::addUseValue);
    }

    public IrFunction getFunction() {
        return (IrFunction) this.getUseValueList().get(0);
    }

    public List<IrValue> getArgs() {
        List<IrValue> actualArgs = new ArrayList<>();
        for (int i = 1; i < this.getUseValueList().size(); i++) {
            actualArgs.add(this.getUseValueList().get(i));
        }
        return actualArgs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        IrFunction func = getFunction();
        List<IrValue> actualArgs = getArgs();

        if (!func.getReturnType().isVoidType()) {
            sb.append(irName);
            sb.append(" = ");
        }
        sb.append("call ")
                .append((func.getReturnType().isVoidType()) ? "void" : func.getReturnType())
                .append(" ")
                .append(func.getIrName())
                .append("(");

        boolean haveArgs = false;
        for (IrValue arg : actualArgs) {
            sb.append(arg.getIrType().toString())
                    .append(" ");
            sb.append(arg.getIrName()).append(", ");
            haveArgs = true;
        }
        if (haveArgs)
            sb.delete(sb.length() - 2, sb.length());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void toMips() {
        super.toMips();
        List<Register> allocatedRegisterList = MipsBuilder.getAllocatedRegList();

        // 1. 计算需要保护的寄存器：在当前指令处活跃的 Caller-Saved 寄存器
        HashSet<Register> registersToSave = new HashSet<>();
        // 活跃变量 (getLiveValuesAt 返回的是指令执行后的活跃变量，即跨越调用的变量)
        HashSet<midend.llvm.value.IrValue> liveValues = this.getBlock().getLiveValuesAt(this);
        for (midend.llvm.value.IrValue val : liveValues) {
            Register reg = MipsBuilder.getValueToRegister(val);
            if (reg != null && MipsBuilder.isCallerSaved(reg)) {
                registersToSave.add(reg);
            }
        }
        // 2. 处理参数覆盖问题：如果参数来源是 $a0-$a3，也需要保护
        List<IrValue> actualArgs = getArgs();
        for (IrValue arg : actualArgs) {
            Register reg = MipsBuilder.getValueToRegister(arg);
            if (reg != null && reg.ordinal() >= Register.A0.ordinal() && reg.ordinal() <= Register.A3.ordinal()) {
                registersToSave.add(reg);
            }
        }

        // 2. 保护现场
        MipsBuilder.saveCurrent(allocatedRegisterList, registersToSave);

        // 3. 将参数填入对应位置
        fillParams(actualArgs, allocatedRegisterList, registersToSave);

        // 4. 跳转到函数
        new MipsJump(MipsJump.JumpType.JAL, getFunction().getMipsLabel());

        // 5. 恢复现场
        MipsBuilder.recoverCurrent(allocatedRegisterList, registersToSave);

        // 6. 处理返回值
        IrFunction func = getFunction();
        if (!func.getReturnType().isVoidType()) {
            Register rd = MipsBuilder.getValueToRegister(this);
            if (rd == null) {
                rd = Register.K0;
            }

            if (rd != Register.V0) {
                new MarsMove(rd, Register.V0);
            }

            if (rd == Register.K0) {
                Integer offset = MipsBuilder.getStackValueOffset(this);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.SW, Register.K0, Register.SP, offset);
                }
            }
        }
    }

    private void fillParams(List<IrValue> paramList, List<Register> allocatedRegisterList,
            HashSet<Register> savedRegisters) {
        int regSaveBase = MipsBuilder.getRegSaveOffset();
        for (int i = 0; i < paramList.size(); i++) {
            IrValue param = paramList.get(i);
            if (i < 4) {
                Register paramRegister = Register.get(Register.A0.ordinal() + i);
                loadValueToRegister(param, paramRegister, regSaveBase, allocatedRegisterList, savedRegisters);
            } else {
                Register tempRegister = Register.K0;
                loadValueToRegister(param, tempRegister, regSaveBase, allocatedRegisterList, savedRegisters);
                // 参数 4+ 存放在栈顶起始位置 (0, 4, 8, 12, 16...)
                new MipsLsu(MipsLsu.LsuType.SW, tempRegister, Register.SP, (i - 4) * 4);
            }
        }
    }

    private void loadValueToRegister(IrValue value, Register reg, int regSaveBase,
            List<Register> allocatedRegisterList, HashSet<Register> savedRegisters) {
        Register srcReg = MipsBuilder.getValueToRegister(value);
        if (srcReg != null) {
            Integer offset = MipsBuilder.getRegisterOffset(srcReg);
            if (offset != null && MipsBuilder.isCallerSaved(srcReg) && savedRegisters.contains(srcReg)) {
                // 如果该值在寄存器中且被保护了，从保护区加载
                new MipsLsu(MipsLsu.LsuType.LW, reg, Register.SP, offset);
            } else if (srcReg != reg) {
                new MarsMove(reg, srcReg);
            }
        } else if (value instanceof IrConstInt constInt) {
            new MipsAlu(MipsAlu.AluType.ADDIU, reg, Register.ZERO, constInt.getValue());
        } else if (value instanceof midend.llvm.value.IrGlobalValue globalValue) {
            new MipsLsu(MipsLsu.LsuType.LA, reg, globalValue.getMipsLabel());
        } else if (value instanceof midend.llvm.constant.IrConstString constString) {
            new MipsLsu(MipsLsu.LsuType.LA, reg, constString.getMipsLabel());
        } else {
            Integer offset = MipsBuilder.getStackValueOffset(value);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.LW, reg, Register.SP, offset);
            }
        }
    }
}
