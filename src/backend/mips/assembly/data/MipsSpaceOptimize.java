package backend.mips.assembly.data;

import midend.llvm.constant.IrConstant;
import midend.llvm.constant.IrConstInt;

import java.util.ArrayList;
import java.util.StringJoiner;

public class MipsSpaceOptimize extends MipsDataAssembly {
    private final String name;
    private final int size;
    private final ArrayList<IrConstant> valueList;

    public MipsSpaceOptimize(String name, int size, ArrayList<IrConstant> valueList) {
        super(name);
        this.name = name;
        this.size = size;
        this.valueList = valueList;
        for (int count = this.valueList.size(); count < this.size; count++) {
            this.valueList.add(new IrConstInt(0));
        }
    }

    private int getNotZeroIndex() {
        int index = -1;
        for (int i = 0; i < this.valueList.size(); i++) {
            if (Integer.parseInt(valueList.get(i).getIrName()) != 0) {
                index = i;
            }
        }
        return index;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.name + ":");

        int index = this.getNotZeroIndex();
        if (index >= 0) {
            builder.append("\t.word ");
            StringJoiner joiner = new StringJoiner(", ");
            for (int i = 0; i <= this.getNotZeroIndex(); i++) {
                joiner.add(this.valueList.get(i).getIrName());
            }

            builder.append(joiner);
            builder.append("\n\t");
        }
        builder.append("\t.space ");
        builder.append(4 * (this.size - index - 1));

        return builder.toString();
    }
}
