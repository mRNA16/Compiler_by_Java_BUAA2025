package midend.llvm.value;

import midend.llvm.IrBuilder;
import midend.llvm.constant.IrConstChar;
import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.instr.ctrl.ReturnInstr;
import midend.llvm.type.IrFunctionType;
import midend.llvm.type.IrType;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsLsu;
import backend.mips.assembly.MipsLabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class IrFunction extends IrValue {
    private final List<IrParameter> parameters;
    private final List<IrBasicBlock> basicBlocks;
    private final HashMap<IrValue, Register> valueRegisterMap;

    public IrFunction(String name, IrType returnType) {
        super(new IrFunctionType(returnType), name);
        this.parameters = new ArrayList<>();
        this.basicBlocks = new ArrayList<>();
        this.valueRegisterMap = new HashMap<>();
    }

    public void addParameter(IrParameter parameter) {
        parameters.add(parameter);
    }

    public void addBasicBlock(IrBasicBlock basicBlock) {
        basicBlocks.add(basicBlock);
    }

    public List<IrParameter> getParameters() {
        return parameters;
    }

    public List<IrBasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public IrType getReturnType() {
        return ((IrFunctionType) this.irType).getReturnType();
    }

    public IrBasicBlock getEntryBlock() {
        return basicBlocks.get(0);
    }

    public boolean isMainFunction() {
        return this.irName.equals("@main");
    }

    public void promiseReturn() {
        IrBasicBlock basicBlock = IrBuilder.getCurrentBasicBlock();
        if (!basicBlock.hasTerminator()) {
            IrValue returnValue = null;
            if (this.getReturnType().isInt8Type()) {
                returnValue = new IrConstChar(0);
            } else if (this.getReturnType().isInt32Type()) {
                returnValue = new IrConstInt(0);
            }
            // TODO:这里暂且不进行类型转换，应该不会出问题
            ReturnInstr returnInstr = new ReturnInstr(returnValue);
        }
    }

    public void skipBlankBlock() {
        for (int i = 0; i < basicBlocks.size(); i++) {
            IrBasicBlock block = basicBlocks.get(i);
            if (block.isEmpty()) {
                block.addInstruction(new BrInstr(basicBlocks.get(i + 1), block));
            }
        }
    }

    public HashMap<IrValue, Register> getValueRegisterMap() {
        return this.valueRegisterMap;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("define dso_local ").append(getReturnType()).append(" ").append(irName);
        // 参数声明
        builder.append("(");
        builder.append(parameters.stream().map(IrParameter::toString).collect(Collectors.joining(", ")));
        builder.append(") {\n");
        // 语句声明
        builder.append(basicBlocks.stream().map(IrBasicBlock::toString).collect(Collectors.joining("\n")));
        builder.append("\n}");
        return builder.toString();
    }

    @Override
    public void toMips() {
        new MipsLabel(this.getMipsLabel());

        // 必须在 setCurrentFunction 之前分配参数寄存器，
        // 这样 preAllocateFrame 才能正确计算寄存器保存空间
        for (int i = 0; i < this.parameters.size(); i++) {
            if (i < 4) {
                this.valueRegisterMap.put(this.parameters.get(i),
                        Register.get(Register.A0.ordinal() + i));
            }
        }

        MipsBuilder.setCurrentFunction(this);

        int frameSize = MipsBuilder.getFrameSize();
        if (frameSize > 0) {
            new MipsAlu(MipsAlu.AluType.ADDIU, Register.SP, Register.SP, -frameSize);
            // 保存 RA 寄存器
            new MipsLsu(MipsLsu.LsuType.SW, Register.RA, Register.SP, MipsBuilder.getRaOffset());
        }

        for (int i = 0; i < this.parameters.size(); i++) {
            IrParameter param = this.parameters.get(i);
            if (i < 4) {
                Register reg = Register.get(Register.A0.ordinal() + i);
                Integer offset = MipsBuilder.getStackValueOffset(param);
                if (offset != null) {
                    new MipsLsu(MipsLsu.LsuType.SW, reg, Register.SP, offset);
                }
            }
        }

        for (IrBasicBlock irBasicBlock : this.basicBlocks) {
            irBasicBlock.toMips();
        }
    }
}
