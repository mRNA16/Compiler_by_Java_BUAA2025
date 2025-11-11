package midend.semantic;

/**
 * 符号类型枚举
 * 表示变量、常量、函数的类型
 */
public enum SymbolType {
    INT("int"),
    INT_ARRAY("int[]"),
    CONST_INT("const int"),
    CONST_INT_ARRAY("const int[]"),
    VOID("void"),
    FUNCTION_INT("function int"),
    FUNCTION_VOID("function void");

    private final String typeName;

    SymbolType(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }

    /**
     * 检查是否为常量类型
     */
    public boolean isConst() {
        return this == CONST_INT || this == CONST_INT_ARRAY;
    }

    /**
     * 检查是否为数组类型
     */
    public boolean isArray() {
        return this == INT_ARRAY || this == CONST_INT_ARRAY;
    }

    /**
     * 检查是否为函数类型
     */
    public boolean isFunction() {
        return this == FUNCTION_INT || this == FUNCTION_VOID;
    }

    /**
     * 获取基础类型（去除const和array修饰）
     */
    public SymbolType getBaseType() {
        switch (this) {
            case INT:
            case CONST_INT:
                return INT;
            case INT_ARRAY:
            case CONST_INT_ARRAY:
                return INT_ARRAY;
            case FUNCTION_INT:
                return INT;
            case FUNCTION_VOID:
                return VOID;
            default:
                return this;
        }
    }

    /**
     * 从字符串创建类型
     */
    public static SymbolType fromString(String typeStr, boolean isConst, boolean isArray) {
        if (isArray) {
            return isConst ? CONST_INT_ARRAY : INT_ARRAY;
        } else {
            return isConst ? CONST_INT : INT;
        }
    }

    /**
     * 获取类型名称（用于输出）
     */
    public String getTypeName() {
        switch (this) {
            case CONST_INT:
                return "ConstInt";
            case CONST_INT_ARRAY:
                return "ConstIntArray";
            case INT:
                return "Int";
            case INT_ARRAY:
                return "IntArray";
            case FUNCTION_INT:
                return "IntFunc";
            case FUNCTION_VOID:
                return "VoidFunc";
            default:
                return "Unknown";
        }
    }

    /**
     * 获取静态变量类型名称
     */
    public String getStaticTypeName() {
        switch (this) {
            case INT:
                return "StaticInt";
            case INT_ARRAY:
                return "StaticIntArray";
            default:
                return getTypeName();
        }
    }
}

