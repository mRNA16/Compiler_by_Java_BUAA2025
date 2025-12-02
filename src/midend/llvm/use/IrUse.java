package midend.llvm.use;

import midend.llvm.value.IrValue;

public class IrUse {
    private final IrUser user;
    private final IrValue value;
    public IrUse(IrUser user, IrValue value) {
        this.user = user;
        this.value = value;
    }

    public IrValue getValue() {
        return value;
    }

    public IrUser getUser() {
        return user;
    }
}
