package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 初始值节点
 */
public class InitVal extends AbstractASTNode {
    private List<Exp> expList = new ArrayList<>();
    private boolean isArray;
    
    public InitVal() {
        super(SyntaxType.INIT_VAL);
    }
    
    /**
     * 添加表达式
     */
    public void addExp(Exp exp) {
        this.expList.add(exp);
        if (exp != null) {
            addChild(exp);
        }
    }
    
    /**
     * 获取表达式列表
     */
    public List<Exp> getExpList() {
        return expList;
    }

    /**
     * 是否存在表达式值
     */
    public boolean hasExp() {
        return !expList.isEmpty();
    }

    /**
     * 设置是否为数组定义初始值
     */
    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }
}