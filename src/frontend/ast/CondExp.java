package frontend.ast;

/**
 * 条件表达式节点（三元运算符）
 */
public class CondExp extends Exp {
    private LOrExp lOrExp;
    
    public CondExp() {
        super(SyntaxType.COND_EXP);
    }
    
    /**
     * 设置假值表达式
     */
    public void setLOrExp(LOrExp lOrExp) {
        this.lOrExp = lOrExp;
        if (lOrExp != null) {
            addChild(lOrExp);
        }
    }
    

    /**
     * 获取假值表达式
     */
    public LOrExp getLOrExp() {
        return lOrExp;
    }
}