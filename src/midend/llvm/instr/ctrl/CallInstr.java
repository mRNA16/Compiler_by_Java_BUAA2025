package midend.llvm.instr.ctrl;

import midend.llvm.IrBuilder;
import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrParameter;
import midend.llvm.constant.IrConstInt;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsJump;
import backend.mips.assembly.MipsLsu;
import backend.mips.assembly.fake.MarsMove;

import java.util.ArrayList;
import java.util.List;

public class CallInstr extends Instr {
    private IrFunction function;
    private List<IrValue> args;

    public CallInstr(IrFunction function, List<IrValue> args) {
        super(function.getReturnType(), InstrType.CALL,
                (function.getReturnType().isVoidType() ? "call" : IrBuilder.getLocalVarNameIr()));
        this.function = function;
        this.addUseValue(function);
        this.args = new ArrayList<>(args);
        args.forEach(this::addUseValue);
    }

    public IrFunction getFunction() {
        return function;
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
        if (!function.getReturnType().isVoidType()) {
            sb.append(irName);
            sb.append(" = ");
        }
        sb.append("call ")
                .append((function.getReturnType().isVoidType()) ? "void" : function.getReturnType())
                .append(" ")
                .append(function.getIrName())
                .append("(");

        boolean haveArgs = false;
        for (IrValue arg : args) {
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

        // 保护现场
        saveCurrent(allocatedRegisterList);

        // 从 useValueList 获取实际参数（第一个元素是函数，参数从索引1开始）
        List<IrValue> actualArgs = new ArrayList<>();
        for (int i = 1; i < this.getUseValueList().size(); i++) {
            actualArgs.add(this.getUseValueList().get(i));
        }

        // 将参数填入对应位置
        fillParams(actualArgs, allocatedRegisterList);

        // 跳转到函数
        new MipsJump(MipsJump.JumpType.JAL, function.getMipsLabel());

        // 恢复现场
        recoverCurrent(allocatedRegisterList);

        // 处理返回值
        if (!function.getReturnType().isVoidType()) {
            Register rd = MipsBuilder.allocateStackForValue(this) == null ? MipsBuilder.getValueToRegister(this)
                    : Register.K0;

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

    private void saveCurrent(List<Register> allocatedRegisterList) {
        int baseOffset = MipsBuilder.getRegSaveOffset();
        for (int i = 0; i < allocatedRegisterList.size(); i++) {
            new MipsLsu(MipsLsu.LsuType.SW, allocatedRegisterList.get(i), Register.SP,
                    baseOffset - (i + 1) * 4);
        }
    }

    private void fillParams(List<IrValue> paramList, List<Register> allocatedRegisterList) {
        int regSaveBase = MipsBuilder.getRegSaveOffset();
        for (int i = 0; i < paramList.size(); i++) {
            IrValue param = paramList.get(i);
            if (i < 3) {
                Register paramRegister = Register.get(Register.A0.ordinal() + i + 1);
                loadValueToRegister(param, paramRegister, regSaveBase, allocatedRegisterList);
            } else {
                Register tempRegister = Register.K0;
                loadValueToRegister(param, tempRegister, regSaveBase, allocatedRegisterList);
                // 参数 4+ 存放在栈顶起始位置 (0, 4, 8, 12, 16...)
                new MipsLsu(MipsLsu.LsuType.SW, tempRegister, Register.SP, i * 4);
            }
        }
    }

    private void recoverCurrent(List<Register> allocatedRegisterList) {
        int baseOffset = MipsBuilder.getRegSaveOffset();
        for (int i = 0; i < allocatedRegisterList.size(); i++) {
            new MipsLsu(MipsLsu.LsuType.LW, allocatedRegisterList.get(i), Register.SP,
                    baseOffset - (i + 1) * 4);
        }
    }

    private void loadValueToRegister(IrValue value, Register reg, int regSaveBase,
            List<Register> allocatedRegisterList) {
        Register srcReg = MipsBuilder.getValueToRegister(value);
        if (srcReg != null) {
            // 如果该值当前在寄存器中，且该寄存器被保护了，需要从保护区加载？
            // 不，在 saveCurrent 之后，寄存器的值还在寄存器里，可以直接 move
            if (srcReg != reg) {
                new MarsMove(reg, srcReg);
            }
        } else if (value instanceof IrConstInt constInt) {
            new MipsAlu(MipsAlu.AluType.ADDIU, reg, Register.ZERO, constInt.getValue());
        } else {
            Integer offset = MipsBuilder.getStackValueOffset(value);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.LW, reg, Register.SP, offset);
            }
        }
    }
}
