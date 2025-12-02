package midend.semantic;

import error.Error;
import error.ErrorRecorder;
import error.ErrorType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 符号表
 * 使用HashMap存储符号，支持作用域嵌套
 */
public class SymbolTable {
    private final int depth;  // 作用域深度
    private final int scopeNumber;  // 作用域序号（用于输出）
    private final SymbolTable parent;  // 父作用域
    private final Map<String, Symbol> symbols;  // 符号映射表
    private final List<Symbol> symbolList;  // 符号列表（保持插入顺序）
    private final List<SymbolTable> childTables;  // 子符号表列表

    public SymbolTable(int depth, int scopeNumber, SymbolTable parent) {
        this.depth = depth;
        this.scopeNumber = scopeNumber;
        this.parent = parent;
        this.symbols = new HashMap<>();
        this.symbolList = new ArrayList<>();
        this.childTables = new ArrayList<>();
    }

    public int getDepth() {
        return depth;
    }

    public int getScopeNumber() {
        return scopeNumber;
    }

    public SymbolTable getParent() {
        return parent;
    }

    public List<SymbolTable> getChildTables() {
        return new ArrayList<>(childTables);
    }

    public void addChildTable(SymbolTable child) {
        childTables.add(child);
    }

    /**
     * 在当前作用域查找符号
     */
    public Symbol lookupLocal(String name) {
        return symbols.get(name);
    }

    /**
     * 在所有作用域中查找符号（从当前作用域向上查找）
     */
    public Symbol lookup(String name) {
        SymbolTable current = this;
        while (current != null) {
            Symbol symbol = current.lookupLocal(name);
            if (symbol != null) {
                return symbol;
            }
            current = current.parent;
        }
        return null;
    }

    /**
     * 在中间代码生成过程中使用，查找已经具有IrValue的Symbol
     * 防止还未声明，应该去上一级表中寻找时，误认为是当前表中的同名符号
     */
    public Symbol lookupWithIrValue(String name) {
        SymbolTable current = this;
        while (current != null) {
            Symbol symbol = current.lookupLocal(name);
            if (symbol != null && symbol.getIrValue() != null) {
                return symbol;
            }
            current = current.parent;
        }
        return null;
    }

    /**
     * 添加符号到当前作用域
     * @return 是否成功添加（如果已存在同名符号则返回false）
     */
    public boolean addSymbol(Symbol symbol, int lineNumber) {
        String name = symbol.getName();

        // 检查当前作用域是否已有同名符号
        if (symbols.containsKey(name)) {
            ErrorRecorder.getErrorRecorder().addError(
                    new Error(ErrorType.NAME_REDEFINED, lineNumber));
            return false;
        }

        symbols.put(name, symbol);
        symbolList.add(symbol);  // 保持插入顺序
        return true;
    }

    /**
     * 检查是否为全局作用域
     */
    public boolean isGlobalScope() {
        return parent == null;
    }

    /**
     * 获取所有符号（按插入顺序）
     */
    public List<Symbol> getAllSymbols() {
        return new ArrayList<>(symbolList);
    }

    /**
     * 获取符号表输出字符串（用于symbol.txt）
     */
    public String toOutputString() {
        StringBuilder sb = new StringBuilder();
        for (Symbol symbol : symbolList) {
            // 跳过main函数
            if (symbol instanceof FunctionSymbol && ("main".equals(symbol.getName())||"getint".equals(symbol.getName()))) {
                continue;
            }
            sb.append(scopeNumber).append(" ")
                    .append(symbol.getName()).append(" ")
                    .append(getSymbolTypeName(symbol)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取符号的类型名称
     */
    private String getSymbolTypeName(Symbol symbol) {
        if (symbol instanceof VariableSymbol varSymbol) {
            if (varSymbol.isStatic()) {
                return varSymbol.getType().getStaticTypeName();
            } else {
                return varSymbol.getType().getTypeName();
            }
        } else if (symbol instanceof FunctionSymbol funcSymbol) {
            SymbolType returnType = funcSymbol.getReturnType();
            if (returnType == SymbolType.VOID) {
                return "VoidFunc";
            } else if (returnType == SymbolType.INT) {
                return "IntFunc";
            }
        }
        return "Unknown";
    }
}

