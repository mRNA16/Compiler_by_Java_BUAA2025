package midend.llvm.type;

public abstract class IrType {
    public boolean isInt1Type(){return false;}
    public boolean isInt8Type(){return false;}
    public boolean isInt32Type(){return false;}
    public boolean isArrayType(){return false;}
    public boolean isVoidType(){return false;}
    public boolean isPointerType(){return false;}

    @Override
    public abstract String toString();
}
