package frontend.ast;

/**
 * 函数形参节点
 */
public class FuncFParam extends AbstractASTNode {
    private Ident ident;
    
    public FuncFParam() {
        super(SyntaxType.FUNC_FPARAM);
    }
    
    /**
     * 设置参数名
     */
    public void setIdent(Ident ident) {
        this.ident = ident;
        if (ident != null) {
            addChild(ident);
        }
    }
    
    /**
     * 获取参数名
     */
    public Ident getIdent() {
        return ident;
    }
    
    /**
     * 获取参数名称
     */
    public String getName() {
        return ident != null ? ident.getName() : "";
    }
}