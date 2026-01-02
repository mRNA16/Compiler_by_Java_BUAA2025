package midend.llvm.constant;

import midend.llvm.type.IrArrayType;
import midend.llvm.type.IrType;
import midend.llvm.value.IrValue;

import java.util.ArrayList;
import java.util.List;

import backend.mips.assembly.data.MipsSpaceOptimize;

public class IrConstArray extends IrConstant {
    private final int arraySize;
    private final List<IrConstant> values;

    public IrConstArray(int arraySize, IrType elementType, String name, List<IrConstant> initValues) {
        super(new IrArrayType(arraySize, elementType), name);
        this.arraySize = arraySize;
        this.values = initValues == null ? new ArrayList<>() : new ArrayList<>(initValues);
    }

    public int getArraySize() {
        return arraySize;
    }

    public List<IrConstant> getValues() {
        return values;
    }

    public int getSize() {
        return arraySize;
    }

    public List<Integer> getFlattenedValues() {
        List<Integer> flattened = new ArrayList<>();
        for (IrConstant constant : values) {
            if (constant instanceof midend.llvm.constant.IrConstInt irConstInt) {
                flattened.add(irConstInt.getValue());
            } else if (constant instanceof IrConstArray irConstArray) {
                flattened.addAll(irConstArray.getFlattenedValues());
            }
        }
        int currentSize = flattened.size();
        int totalSize = arraySize;

        return flattened;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.irType);
        sb.append(" ");

        boolean haveNotZero = false;
        for (IrConstant constant : values) {
            if (constant instanceof IrConstInt irConstInt) {
                if (irConstInt.getValue() != 0) {
                    haveNotZero = true;
                    break;
                }
            }
        }

        if (this.values.isEmpty() || !haveNotZero) {
            sb.append("zeroinitializer");
        } else {
            sb.append("[");
            for (IrValue v : values) {
                sb.append(v.toString());
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            IrType elementType = values.get(0).getIrType();
            String padding = ", " + elementType.toString() + "0";
            sb.append(padding.repeat(Math.max(0, arraySize - this.values.size())));
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public void mipsDeclare(String label) {
        ArrayList<IrConstant> arrayList = new ArrayList<>(values);
        ArrayList<IrConstant> flattened = new ArrayList<>();
        flatten(this, flattened);

        int sizeInWords = this.irType.getSize() / 4;

        new MipsSpaceOptimize(label, sizeInWords, flattened);
    }

    private void flatten(IrConstant constant, ArrayList<IrConstant> list) {
        if (constant instanceof IrConstArray array) {
            for (IrConstant c : array.getValues()) {
                flatten(c, list);
            }
        } else {
            list.add(constant);
        }
    }
}
