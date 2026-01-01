package midend.llvm.value;

import midend.llvm.type.IrType;
import midend.llvm.use.IrUse;
import midend.llvm.use.IrUser;

import java.util.ArrayList;
import java.util.List;

public class IrValue {
    protected final IrType irType;
    protected final String irName;
    protected final List<IrUse> beUsedList;

    public IrValue(IrType irType, String irName) {
        this.irType = irType;
        this.irName = irName;
        this.beUsedList = new ArrayList<>();
    }

    public IrType getIrType() {
        return this.irType;
    }

    public String getIrName() {
        return this.irName;
    }

    public void addUse(IrUse use) {
        this.beUsedList.add(use);
    }

    public void deleteUser(IrUser user) {
        this.beUsedList.removeIf(use -> use.getUser() == user);
    }

    public List<IrUse> getBeUsedList() {
        return this.beUsedList;
    }

    /**
     * 将所有使用此 value 的 user 替换为使用 newValue
     */
    public void modifyAllUsersToNewValue(IrValue newValue) {
        ArrayList<IrUser> userList = new ArrayList<>();
        for (IrUse use : this.beUsedList) {
            userList.add(use.getUser());
        }
        for (IrUser user : userList) {
            user.modifyValue(this, newValue);
            this.deleteUser(user);
            // 将 newValue 加入到 user 的使用列表中
            newValue.addUse(new IrUse(user, newValue));
        }
    }

    public void toMips() {
    }

    public String getMipsLabel() {
        return this.irName.substring(1);
    }
}
