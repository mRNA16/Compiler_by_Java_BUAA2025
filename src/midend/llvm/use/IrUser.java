package midend.llvm.use;

import midend.llvm.type.IrType;
import midend.llvm.value.IrValue;

import java.util.ArrayList;
import java.util.List;

public class IrUser extends IrValue {
    private final List<IrValue> useValueList;

    public IrUser(IrType type, String name) {
        super(type, name);
        this.useValueList = new ArrayList<>();
    }

    public void addUseValue(IrValue value) {
        useValueList.add(value);
        if (value != null) {
            value.addUse(new IrUse(this, value));
        }
    }

    public List<IrValue> getUseValueList() {
        return useValueList;
    }

    public void removeAllValueUse() {
        for (IrValue value : useValueList) {
            if (value != null) {
                value.deleteUser(this);
            }
        }
        useValueList.clear();
    }
}
