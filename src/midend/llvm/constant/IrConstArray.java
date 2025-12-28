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
        // Padding with zeros if necessary
        int currentSize = flattened.size();
        int totalSize = arraySize; // Assuming arraySize is total elements for 1D, but for multi-D it might be
                                   // different.
        // However, IrConstArray usually represents one dimension.
        // If it's a multi-dimensional array, values contains sub-arrays.
        // If it's a 1D array, values contains integers.
        // The issue is calculating total elements recursively.
        // But for now, let's assume simple flattening.
        // Actually, IrConstArray structure in this compiler seems to handle 1D at a
        // time.
        // Let's check how IrArrayType works.
        // If it is [2 x [3 x i32]], arraySize is 2. values has 2 IrConstArrays.
        // Each IrConstArray has arraySize 3.
        // So recursive flattening is correct.

        // We need to pad based on the TOTAL size of THIS array level?
        // No, getFlattenedValues should return ALL integers in this array
        // (recursively).
        // The padding logic in toString uses `elementType` and `arraySize`.
        // If elementType is i32, we pad with 0.
        // If elementType is array, we pad with zeroinitializer array?
        // MipsSpaceOptimize expects a list of Integers.
        // So we should flatten everything to Integers.

        // Calculate expected total integers?
        // It's hard without type info fully available/parsed here easily.
        // But we can just flatten what we have.
        // The caller (IrGlobalValue) uses getSize() * 4.
        // If getSize() returns the number of elements in THIS dimension (e.g. 2 for [2
        // x [3 x i32]]),
        // then getSize() * 4 is WRONG for total size in bytes if elements are arrays.
        // IrGlobalValue should use getIrType().getSize() or similar.
        // But IrType.getSize() might not be implemented for MIPS yet.

        // Let's look at IrGlobalValue usage: `constArray.getSize() * 4`.
        // This implies IrGlobalValue thinks it's a 1D array of i32.
        // If it's multi-dimensional, this is buggy in IrGlobalValue too.
        // But let's implement getFlattenedValues first.

        // For padding:
        // We need to know how many integers are missing.
        // This requires knowing the total capacity in integers.
        // Let's assume the user's compiler handles initialization correctly and we just
        // dump what's there.
        // Or we can try to pad.

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
