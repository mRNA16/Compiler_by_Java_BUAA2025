package midend.semantic;

/**
 * 符号基类
 * 表示符号表中的条目，可以是变量、常量或函数
 */
public abstract class Symbol {
    protected final String name;
    protected final SymbolType type;
    protected final int lineNumber;

    public Symbol(String name, SymbolType type, int lineNumber) {
        this.name = name;
        this.type = type;
        this.lineNumber = lineNumber;
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return name + " : " + type;
    }
}

