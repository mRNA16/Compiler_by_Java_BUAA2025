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
        return args;
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
        // 现场信息
        int currentOffset = MipsBuilder.getCurrentStackOffset();
        List<Register> allocatedRegisterList = MipsBuilder.getAllocatedRegList();

        // 保护现场
        saveCurrent(currentOffset, allocatedRegisterList);

        // 将参数填入对应位置
        fillParams(args, currentOffset, allocatedRegisterList);
        currentOffset = currentOffset - 4 * allocatedRegisterList.size() - 8;

        // 设置新的栈地址
        new MipsAlu(MipsAlu.AluType.ADDI, Register.SP, Register.SP, currentOffset);
        // 跳转到函数
        new MipsJump(MipsJump.JumpType.JAL, function.getMipsLabel());

        // 恢复现场
        currentOffset = currentOffset + 4 * allocatedRegisterList.size() + 8;
        recoverCurrent(currentOffset, allocatedRegisterList);

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

    private void saveCurrent(int currentOffset, List<Register> allocatedRegisterList) {
        int registerNum = 0;
        for (Register register : allocatedRegisterList) {
            registerNum++;
            new MipsLsu(MipsLsu.LsuType.SW, register, Register.SP,
                    currentOffset - registerNum * 4);
        }
        // 保存SP寄存器和RA寄存器
        new MipsLsu(MipsLsu.LsuType.SW, Register.SP, Register.SP,
                currentOffset - registerNum * 4 - 4);
        new MipsLsu(MipsLsu.LsuType.SW, Register.RA, Register.SP,
                currentOffset - registerNum * 4 - 8);
    }

    private void fillParams(List<IrValue> paramList, int currentOffset, List<Register> allocatedRegisterList) {
        for (int i = 0; i < paramList.size(); i++) {
            IrValue param = paramList.get(i);
            if (i < 3) {
                Register paramRegister = Register.get(Register.A0.ordinal() + i + 1);
                if (param instanceof IrParameter) {
                    Register paraRegister = MipsBuilder.getValueToRegister(param);
                    if (allocatedRegisterList.contains(paraRegister)) {
                        new MipsLsu(MipsLsu.LsuType.LW, paramRegister, Register.SP,
                                currentOffset - 4 * allocatedRegisterList.indexOf(paraRegister) - 4);
                    } else {
                        loadValueToRegister(param, paramRegister);
                    }
                } else {
                    loadValueToRegister(param, paramRegister);
                }
            } else {
                Register tempRegister = Register.K0;
                if (param instanceof IrParameter) {
                    Register paraRegister = MipsBuilder.getValueToRegister(param);
                    if (allocatedRegisterList.contains(paraRegister)) {
                        new MipsLsu(MipsLsu.LsuType.LW, tempRegister, Register.SP,
                                currentOffset - 4 * allocatedRegisterList.indexOf(paraRegister) - 4);
                    } else {
                        loadValueToRegister(param, tempRegister);
                    }
                } else {
                    loadValueToRegister(param, tempRegister);
                }
                new MipsLsu(MipsLsu.LsuType.SW, tempRegister, Register.SP,
                        currentOffset - 4 * allocatedRegisterList.size() - 8 - 4 * i - 4);
            }
        }
    }

    private void recoverCurrent(int formerOffset, List<Register> allocatedRegisterList) {
        new MipsLsu(MipsLsu.LsuType.LW, Register.RA, Register.SP, 0);
        new MipsLsu(MipsLsu.LsuType.LW, Register.SP, Register.SP, 4);

        int registerNum = 0;
        for (Register register : allocatedRegisterList) {
            registerNum++;
            new MipsLsu(MipsLsu.LsuType.LW, register, Register.SP,
                    formerOffset - registerNum * 4);
        }
    }

    private void loadValueToRegister(IrValue value, Register reg) {
        Register srcReg = MipsBuilder.getValueToRegister(value);
        if (srcReg != null) {
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
