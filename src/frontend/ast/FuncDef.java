package frontend.ast;

/**
 * 函数定义节点
 */
public class FuncDef extends AbstractASTNode {
    private FuncType funcType;
    private Ident ident;
    private FuncFParams funcFParams;
    private Block block;
    
    public FuncDef() {
        super(SyntaxType.FUNC_DEF);
    }
    
    /**
     * 设置函数类型
     */
    public void setFuncType(FuncType funcType) {
        this.funcType = funcType;
        if (funcType != null) {
            addChild(funcType);
        }
    }
    
    /**
     * 设置函数名
     */
    public void setIdent(Ident ident) {
        this.ident = ident;
        if (ident != null) {
            addChild(ident);
        }
    }
    
    /**
     * 设置形参列表
     */
    public void setFuncFParams(FuncFParams funcFParams) {
        this.funcFParams = funcFParams;
        if (funcFParams != null) {
            addChild(funcFParams);
        }
    }
    
    /**
     * 设置函数体
     */
    public void setBlock(Block block) {
        this.block = block;
        if (block != null) {
            addChild(block);
        }
    }
    
    /**
     * 获取函数类型
     */
    public FuncType getFuncType() {
        return funcType;
    }
    
    /**
     * 获取函数名
     */
    public Ident getIdent() {
        return ident;
    }
    
    /**
     * 获取形参列表
     */
    public FuncFParams getFuncFParams() {
        return funcFParams;
    }
    
    /**
     * 获取函数体
     */
    public Block getBlock() {
        return block;
    }
    
    /**
     * 获取函数名称
     */
    public String getName() {
        return ident != null ? ident.getName() : "";
    }
}