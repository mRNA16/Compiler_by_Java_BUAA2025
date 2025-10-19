package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量声明节点
 */
public class ConstDecl extends Decl {
    private final List<ConstDef> constDefs;
    
    public ConstDecl() {
        super(SyntaxType.CONST_DECL);
        this.constDefs = new ArrayList<>();
    }
    
    /**
     * 添加常量定义
     */
    public void addConstDef(ConstDef constDef) {
        if (constDef != null) {
            constDefs.add(constDef);
            addChild(constDef);
        }
    }
    
    /**
     * 获取所有常量定义
     */
    public List<ConstDef> getConstDefs() {
        return new ArrayList<>(constDefs);
    }
}