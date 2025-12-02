package midend.llvm.value;

import midend.llvm.type.IrType;
import midend.llvm.use.IrUse;

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

    public IrType getIrType(){
        return this.irType;
    }

    public String getIrName(){
        return this.irName;
    }

    public void addUse(IrUse use){
        this.beUsedList.add(use);
    }

}
