package midend.llvm.instr.phi;

import midend.llvm.IrBuilder;
import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrType;
import midend.llvm.use.IrUse;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrValue;

import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * Phi 指令类，用于 SSA 形式中处理控制流汇合点的变量定值
 */
public class PhiInstr extends Instr {
    private final ArrayList<IrBasicBlock> beforeBlockList;

    /**
     * 创建 Phi 指令
     * 
     * @param irType       Phi 指令的结果类型
     * @param irBasicBlock Phi 指令所在的基本块
     */
    public PhiInstr(IrType irType, IrBasicBlock irBasicBlock) {
        super(irType, InstrType.PHI, getNewPhiName(irBasicBlock), false);
        this.setBlock(irBasicBlock);

        // 初始化前驱块列表
        this.beforeBlockList = new ArrayList<>(irBasicBlock.getBeforeBlocks());
        // 填充相应数量的 null，等待后续替换
        for (int i = 0; i < this.beforeBlockList.size(); i++) {
            this.addUseValue(null);
        }
    }

    /**
     * 为 Phi 指令生成新名称
     */
    private static String getNewPhiName(IrBasicBlock irBasicBlock) {
        return IrBuilder.getLocalVarNameIr(irBasicBlock.getFunction());
    }

    /**
     * 获取前驱块列表
     */
    public ArrayList<IrBasicBlock> getBeforeBlockList() {
        return this.beforeBlockList;
    }

    public IrValue getValueFromBlock(IrBasicBlock block) {
        int index = this.beforeBlockList.indexOf(block);
        if (index >= 0) {
            return this.getUseValueList().get(index);
        }
        return null;
    }

    /**
     * 将前驱块对应的值转换为实际值
     * 
     * @param irValue     要填入的值
     * @param beforeBlock 对应的前驱块
     */
    public void convertBlockToValue(IrValue irValue, IrBasicBlock beforeBlock) {
        int index = this.beforeBlockList.indexOf(beforeBlock);
        if (index >= 0) {
            // 进行相应的值替换：原先只会是 null
            this.getUseValueList().set(index, irValue);
            // 添加 use 关系
            irValue.addUse(new IrUse(this, irValue));
        }
    }

    /**
     * 移除指定前驱块
     */
    public void removeBlock(IrBasicBlock irBasicBlock) {
        int index = this.beforeBlockList.indexOf(irBasicBlock);
        if (index >= 0) {
            IrValue oldValue = this.getUseValueList().get(index);
            this.getUseValueList().remove(index);
            this.beforeBlockList.remove(index);
            if (oldValue != null) {
                oldValue.deleteUser(this);
            }
        }
    }

    /**
     * 替换前驱块
     */
    public void replaceBlock(IrBasicBlock oldBlock, IrBasicBlock newBlock) {
        int index;
        if (this.beforeBlockList.contains(newBlock)) {
            index = this.beforeBlockList.indexOf(newBlock);
            this.beforeBlockList.remove(index);
            this.getUseValueList().remove(index);
        }

        index = this.beforeBlockList.indexOf(oldBlock);
        if (index >= 0) {
            this.beforeBlockList.set(index, newBlock);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(this.getIrName());
        builder.append(" = phi ");
        builder.append(this.getIrType());
        builder.append(" ");

        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < this.beforeBlockList.size(); i++) {
            final StringBuilder blockBuilder = new StringBuilder();
            blockBuilder.append("[ ");
            IrValue value = this.getUseValueList().get(i);
            blockBuilder.append(value != null ? value.getIrName() : "undef");
            blockBuilder.append(", %");
            blockBuilder.append(this.beforeBlockList.get(i).getIrName());
            blockBuilder.append(" ]");
            joiner.add(blockBuilder);
        }
        builder.append(joiner);

        return builder.toString();
    }

    @Override
    public void toMips() {
        throw new RuntimeException("Phi instruction should be removed before MIPS generation!");
    }
}
