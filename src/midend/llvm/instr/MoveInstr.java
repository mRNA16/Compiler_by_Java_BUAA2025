package midend.llvm.instr;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsLsu;
import backend.mips.assembly.MipsAlu;
import midend.llvm.constant.IrConstInt;
import midend.llvm.type.IrBaseType;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrValue;

/**
 * Move 指令类，用于表示值的复制操作
 */
public class MoveInstr extends Instr {
    public MoveInstr(IrValue srcValue, IrValue dstValue, IrBasicBlock irBasicBlock) {
        super(IrBaseType.VOID, InstrType.MOVE, "move", false);
        this.addUseValue(srcValue);
        this.addUseValue(dstValue);
        this.setBlock(irBasicBlock);
    }

    public IrValue getSrcValue() {
        return this.getUseValueList().get(0);
    }

    public IrValue getDstValue() {
        return this.getUseValueList().get(1);
    }

    public void setSrcValue(IrValue srcValue) {
        this.getSrcValue().deleteUser(this);
        this.getUseValueList().set(0, srcValue);
    }

    @Override
    public String toString() {
        return "move " + this.getDstValue().getIrName() + ", " + this.getSrcValue().getIrName();
    }

    @Override
    public void toMips() {
        super.toMips();

        IrValue srcValue = this.getSrcValue();
        IrValue dstValue = this.getDstValue();
        Register srcRegister = MipsBuilder.getValueToRegister(srcValue);
        Register dstRegister = MipsBuilder.getValueToRegister(dstValue);

        // 如果源和目标是同一个寄存器，不需要 move
        if (srcRegister != null && srcRegister.equals(dstRegister)) {
            return;
        }

        // 获取目标寄存器，如果没有分配则使用 K0
        if (dstRegister == null) {
            dstRegister = Register.K0;
        }

        // 加载源值到目标寄存器
        loadValueToRegister(srcValue, dstRegister);

        // 保存结果
        saveRegisterResult(dstValue, dstRegister);
    }

    /**
     * 将值加载到指定寄存器
     */
    private void loadValueToRegister(IrValue irValue, Register targetRegister) {
        if (irValue instanceof IrConstInt constInt) {
            new MipsAlu(MipsAlu.AluType.ADDIU, targetRegister, Register.ZERO, constInt.getValue());
            return;
        }

        // 处理函数参数 - 前3个参数在 A1, A2, A3 寄存器中
        if (irValue instanceof midend.llvm.value.IrParameter param) {
            midend.llvm.value.IrFunction currentFunc = MipsBuilder.getCurrentFunction();
            if (currentFunc != null) {
                int index = currentFunc.getParameters().indexOf(param);
                if (index >= 0 && index < 3) {
                    Register paramReg = Register.get(Register.A0.ordinal() + index + 1);
                    if (!paramReg.equals(targetRegister)) {
                        new MipsAlu(MipsAlu.AluType.ADDU, targetRegister, paramReg, Register.ZERO);
                    }
                    return;
                }
            }
        }

        Register valueRegister = MipsBuilder.getValueToRegister(irValue);
        if (valueRegister != null) {
            if (!valueRegister.equals(targetRegister)) {
                new MipsAlu(MipsAlu.AluType.ADDU, targetRegister, valueRegister, Register.ZERO);
            }
            return;
        }

        Integer offset = MipsBuilder.getStackValueOffset(irValue);
        if (offset != null) {
            new MipsLsu(MipsLsu.LsuType.LW, targetRegister, Register.SP, offset);
        }
    }

    /**
     * 保存寄存器中的计算结果
     */
    private void saveRegisterResult(IrValue irValue, Register valueRegister) {
        Register register = MipsBuilder.getValueToRegister(irValue);
        if (register == null) {
            // 先确保空间已分配
            MipsBuilder.allocateStackForValue(irValue);
            // 使用 getStackValueOffset 获取正确的偏移（包含 frameSize 调整）
            Integer offset = MipsBuilder.getStackValueOffset(irValue);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.SW, valueRegister, Register.SP, offset);
            }
        } else if (!register.equals(valueRegister)) {
            new MipsAlu(MipsAlu.AluType.ADDU, register, valueRegister, Register.ZERO);
        }
    }
}
