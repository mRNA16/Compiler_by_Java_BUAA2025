package midend.semantic;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 符号管理器
 * 管理符号表的创建、切换和查找
 * 使用栈来管理作用域
 */
public class SymbolManager {
    private static SymbolManager instance;
    private SymbolTable rootTable;  // 全局符号表
    private Stack<SymbolTable> scopeStack;  // 作用域栈
    private List<SymbolTable> allTables;  // 所有符号表（用于输出）
    private int nextScopeNumber;  // 下一个作用域序号
    private int nextOnlyReadScopeNumber;

    private SymbolManager() {
        initialize();
    }

    public static SymbolManager getInstance() {
        if (instance == null) {
            instance = new SymbolManager();
        }
        return instance;
    }

    /**
     * 初始化符号管理器
     */
    public void initialize() {
        nextScopeNumber = 1;  // 全局作用域序号为1
        nextOnlyReadScopeNumber = 1;
        rootTable = new SymbolTable(0, nextScopeNumber++, null);
        rootTable.addSymbol(new FunctionSymbol("getint",SymbolType.INT,0,null,null),0);
        scopeStack = new Stack<>();
        scopeStack.push(rootTable);
        allTables = new ArrayList<>();
        allTables.add(rootTable);
    }

    /**
     * 获取当前符号表
     */
    public SymbolTable getCurrentTable() {
        return scopeStack.isEmpty() ? rootTable : scopeStack.peek();
    }

    /**
     * 获取下一个作用域编号
     */
    public int getNextScopeNumber() {
        return nextScopeNumber;
    }

    /**
     * 获取全局符号表
     */
    public SymbolTable getGlobalTable() {
        return rootTable;
    }

    /**
     * 进入新作用域
     */
    public void enterScope(boolean build) {
        SymbolTable current = getCurrentTable();
        if(build) {
            SymbolTable newTable = new SymbolTable(current.getDepth() + 1, nextScopeNumber++, current);
            current.addChildTable(newTable);
            scopeStack.push(newTable);
            allTables.add(newTable);
        } else {
            SymbolTable nextTable = allTables.get(nextOnlyReadScopeNumber++);
            scopeStack.push(nextTable);
        }

    }

    /**
     * 离开当前作用域
     */
    public void exitScope() {
        if (scopeStack.size() > 1) {
            scopeStack.pop();
        }
    }

    /**
     * 添加符号到当前作用域
     */
    public boolean addSymbol(Symbol symbol, int lineNumber) {
        return getCurrentTable().addSymbol(symbol, lineNumber);
    }

    /**
     * 查找符号（从当前作用域向上查找）
     */
    public Symbol lookupSymbol(String name) {
        return getCurrentTable().lookup(name);
    }

    /**
     * 只在当前作用域查找符号
     */
    public Symbol lookupLocal(String name) {
        return getCurrentTable().lookupLocal(name);
    }

    /**
     * 检查是否在全局作用域
     */
    public boolean isGlobalScope() {
        return getCurrentTable().isGlobalScope();
    }

    /**
     * 重置到全局作用域
     */
    public void resetToGlobal() {
        while (scopeStack.size() > 1) {
            scopeStack.pop();
        }
    }

    /**
     * 获取根符号表（用于输出）
     */
    public SymbolTable getRootTable() {
        return rootTable;
    }

    /**
     * 获取所有符号表（用于输出）
     */
    public List<SymbolTable> getAllTables() {
        return new ArrayList<>(allTables);
    }

    /**
     * 输出所有符号表（按作用域序号排序）
     */
    public String outputAllSymbols() {
        // 按作用域序号排序
        allTables.sort((a, b) -> Integer.compare(a.getScopeNumber(), b.getScopeNumber()));
        StringBuilder sb = new StringBuilder();
        for (SymbolTable table : allTables) {
            sb.append(table.toOutputString());
        }
        return sb.toString();
    }
}

