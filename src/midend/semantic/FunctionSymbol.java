package midend.semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数符号
 * 表示函数的符号表条目
 */
public class FunctionSymbol extends Symbol {
    private final List<SymbolType> parameterTypes;
    private final List<String> parameterNames;

    public FunctionSymbol(String name, SymbolType returnType, int lineNumber) {
        super(name, returnType, lineNumber);
        this.parameterTypes = new ArrayList<>();
        this.parameterNames = new ArrayList<>();
    }

    public FunctionSymbol(String name, SymbolType returnType, int lineNumber,
                         List<SymbolType> paramTypes, List<String> paramNames) {
        super(name, returnType, lineNumber);
        this.parameterTypes = paramTypes != null ? new ArrayList<>(paramTypes) : new ArrayList<>();
        this.parameterNames = paramNames != null ? new ArrayList<>(paramNames) : new ArrayList<>();
    }

    public void addParameter(SymbolType paramType, String paramName) {
        parameterTypes.add(paramType);
        parameterNames.add(paramName);
    }

    public List<SymbolType> getParameterTypes() {
        return new ArrayList<>(parameterTypes);
    }

    public List<String> getParameterNames() {
        return new ArrayList<>(parameterNames);
    }

    public int getParameterCount() {
        return parameterTypes.size();
    }

    public SymbolType getReturnType() {
        return type;
    }

    /**
     * 检查参数类型是否匹配
     */
    public boolean matchParameters(List<SymbolType> argTypes) {
        if (argTypes.size() != parameterTypes.size()) {
            return false;
        }
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (!parameterTypes.get(i).getBaseType().equals(argTypes.get(i).getBaseType())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameterTypes.get(i));
        }
        sb.append(") -> ").append(type);
        return sb.toString();
    }
}

