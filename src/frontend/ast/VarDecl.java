package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 变量声明节点
 */
public class VarDecl extends Decl {
    private final List<VarDef> varDefs;
    
    public VarDecl() {
        super(SyntaxType.VAR_DECL);
        this.varDefs = new ArrayList<>();
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