package frontend.ast;

/**
 * 变量定义节点
 */
public class VarDef extends AbstractASTNode {
    private Ident ident;
    private ConstExp constExp;
    private InitVal initVal;
    
    public VarDef() {
        super(SyntaxType.VAR_DEF);
        this.ident = null;
        this.constExp = null;
        this.initVal = null;
    }
    
    /**
     * 设置标识符
     */
    public void setIdent(Ident ident) {
        this.ident = ident;
        if (ident != null) {
            addChild(ident);
        }
    }

    /**
     * 设置常量表达式
     */
    public void setConstExp(ConstExp constExp) {
        this.constExp = constExp;
        if (constExp != null) {
            addChild(constExp);
        }
    }
    
    /**
     * 设置初始值
     */
    public void setInitVal(InitVal initVal) {
        this.initVal = initVal;
        if (initVal != null) {
            addChild(initVal);
        }
    }
    
    /**
     * 获取标识符
     */
    public Ident getIdent() {
        return ident;
    }

    /**
     * 获取常量表达式
     */
    public ConstExp getConstExp() {
        return constExp;
    }
    
    /**
     * 获取初始值
     */
    public InitVal getInitVal() {
        return initVal;
    }
    
    /**
     * 获取变量名称
     */
    public String getName() {
        return ident != null ? ident.getName() : "";
    }
    
    /**
     * 检查是否有初始值
     */
    public boolean hasInitVal() {
        return initVal != null;
    }
}