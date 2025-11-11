package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 变量声明节点
 */
public class VarDecl extends Decl {
    private final List<VarDef> varDefs;
    private boolean isStatic;  // 是否为static变量

    public VarDecl() {
        super(SyntaxType.VAR_DECL);
        this.varDefs = new ArrayList<>();
        this.isStatic = false;
    }

    /**
     * 设置是否为static
     */
    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    /**
     * 检查是否为static
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * 添加变量定义
     */
    public void addVarDef(VarDef varDef) {
        if (varDef != null) {
            varDefs.add(varDef);
            addChild(varDef);
        }
    }

    /**
     * 获取所有变量定义
     */
    public List<VarDef> getVarDefs() {
        return new ArrayList<>(varDefs);
    }
}