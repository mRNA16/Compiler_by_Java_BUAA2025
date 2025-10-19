package frontend.ast;

/**
 * 常量定义节点
 */
public class ConstDef extends AbstractASTNode {
    private Ident ident;
    private ConstExp constExp;
    private ConstInitVal initVal;
    
    public ConstDef() {
        super(SyntaxType.CONST_DEF);
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
    public void setInitVal(ConstInitVal initVal) {
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
     * 获取标识符
     */
    public ConstExp getConstExp() {
        return constExp;
    }
    
    /**
     * 获取初始值
     */
    public ConstInitVal getInitVal() {
        return initVal;
    }
    
    /**
     * 获取常量名称
     */
    public String getName() {
        return ident != null ? ident.getName() : "";
    }
}