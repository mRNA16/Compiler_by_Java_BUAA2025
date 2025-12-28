package midend.llvm.type;

public class IrBaseType extends IrType{
    public enum BaseType{
        VOID,
        INT1,
        INT8,
        INT32
    }
    public static final IrBaseType VOID = new IrBaseType(BaseType.VOID);
    public static final IrBaseType INT1 = new IrBaseType(BaseType.INT1);
    public static final IrBaseType INT8 = new IrBaseType(BaseType.INT8);
    public static final IrBaseType INT32 = new IrBaseType(BaseType.INT32);

    private final BaseType baseType;

    public IrBaseType(BaseType baseType){
        this.baseType = baseType;
    }

    public int getSize() {
        return switch (baseType) {
            case VOID -> 0; // void类型大小为0
            case INT1 -> 1; // i1类型占1字节
            case INT8 -> 1; // i8类型占1字节
            case INT32 -> 4; // i32类型占4字节
        };
    }

    @Override
    public boolean isInt1Type(){
        return baseType == BaseType.INT1;
    }

    @Override
    public boolean isInt8Type(){
        return baseType == BaseType.INT8;
    }

    @Override
    public boolean isInt32Type(){
        return baseType == BaseType.INT32;
    }

    @Override
    public boolean isVoidType(){
        return baseType == BaseType.VOID;
    }

    @Override
    public String toString(){
        return switch (baseType) {
            case VOID -> "void";
            case INT1 -> "i1";
            case INT8 -> "i8";
            case INT32 -> "i32";
        };
    }

    @Override
    public boolean equals(Object obj){
        if(this==obj) return true;
        if(!(obj instanceof IrBaseType)) return false;
        return this.baseType == ((IrBaseType)obj).baseType;
    }
}
