package frontend.ast;

/**
 * 左值节点
 */
public class LVal extends AbstractASTNode {
    private Ident ident;
    private Exp indexExp;
    
    public LVal() {
        super(SyntaxType.LVAL);
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
     * 设置数组索引表达式
     */
    public void setIndexExp(Exp indexExp) {
        this.indexExp = indexExp;
        if (indexExp != null) {
            addChild(indexExp);
        }
    }
    
    /**
     * 获取标识符
     */
    public Ident getIdent() {
        return ident;
    }
    
    /**
     * 获取数组索引表达式
     */
    public Exp getIndexExp() {
        return indexExp;
    }
    
    /**
     * 获取变量名
     */
    public String getName() {
        return ident != null ? ident.getName() : "";
    }
    
    /**
     * 检查是否为数组访问
     */
    public boolean isArrayAccess() {
        return indexExp != null;
    }
}