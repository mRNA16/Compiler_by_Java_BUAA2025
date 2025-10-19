package frontend.ast;

/**
 * 常量表达式节点
 */
public class ConstExp extends Exp {
    private AddExp addExp;
    
    public ConstExp() {
        super(SyntaxType.CONST_EXP);
    }
    
    /**
     * 设置表达式
     */
    public void setAddExp(AddExp addExp) {
        this.addExp = addExp;
        if (addExp != null) {
            addChild(addExp);
        }
    }
    
    /**
     * 获取表达式
     */
    public AddExp getAddExp() {
        return addExp;
    }
}