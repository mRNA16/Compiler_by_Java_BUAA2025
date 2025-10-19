package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量初始值节点
 */
public class ConstInitVal extends AbstractASTNode {
    private boolean isArray;
    private List<ConstExp> constExpList = new ArrayList<>();
    
    public ConstInitVal() {
        super(SyntaxType.CONST_INIT_VAL);
    }
    
    /**
     * 添加常量表达式
     */
    public void addConstExp(ConstExp constExp) {
        this.constExpList.add(constExp);
        if (constExp != null) {
            addChild(constExp);
        }
    }

    /**
     * 设置是否为数组初始化定义
     */
    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }

    /**
     * 获取常量表达式列表
     */
    public List<ConstExp> getConstExp() {
        return constExpList;
    }

    /**
     * 是否是数组初始化定义
     */
    public boolean isArray() {
        return isArray;
    }

}