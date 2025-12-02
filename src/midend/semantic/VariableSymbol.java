package midend.semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * 变量符号
 * 表示变量或常量的符号表条目
 */
public class VariableSymbol extends Symbol {
    private final boolean isGlobal;
    private final boolean isStatic;  // 是否为static变量
    private final List<Integer> dimensions;  // 数组维度，空列表表示非数组
    private Object initialValue;  // 初始值

    public VariableSymbol(String name, SymbolType type, int lineNumber, boolean isGlobal) {
        super(name, type, lineNumber);
        this.isGlobal = isGlobal;
        this.isStatic = false;
        this.dimensions = new ArrayList<>();
        // 如果是数组，进行占位
        if(type.isArray()) this.dimensions.add(1);
    }

    public VariableSymbol(String name, SymbolType type, int lineNumber, 
                         boolean isGlobal, List<Integer> dimensions) {
        super(name, type, lineNumber);
        this.isGlobal = isGlobal;
        this.isStatic = false;
        this.dimensions = dimensions != null ? new ArrayList<>(dimensions) : new ArrayList<>();
    }

    public VariableSymbol(String name, SymbolType type, int lineNumber, 
                         boolean isGlobal, boolean isStatic, List<Integer> dimensions) {
        super(name, type, lineNumber);
        this.isGlobal = isGlobal;
        this.isStatic = isStatic;
        this.dimensions = dimensions != null ? new ArrayList<>(dimensions) : new ArrayList<>();
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isArray() {
        return !dimensions.isEmpty();
    }

    public int getDimensionCount() {
        return dimensions.size();
    }

    public List<Integer> getDimensions() {
        return new ArrayList<>(dimensions);
    }

    public int getElementCount() {
        if(dimensions.isEmpty()||dimensions.get(0)==0) {
            return 1;
        } else {
            int ans = 1;
            for(Integer dim : dimensions) {
                ans*=dim;
            }
            return ans;
        }
    }

    public void setInitialValue(Object value) {
        this.initialValue = value;
    }

    public Object getInitialValue() {
        return initialValue;
    }

    public boolean isConst() {
        return type.isConst();
    }

}

