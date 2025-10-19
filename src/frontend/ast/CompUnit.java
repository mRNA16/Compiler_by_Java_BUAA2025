package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 编译单元节点 - 整个程序的根节点
 * 包含所有的声明和函数定义
 */
public class CompUnit extends AbstractASTNode {
    private final List<Decl> declarations;
    private final List<FuncDef> functionDefinitions;
    private MainFuncDef mainFunction;
    
    public CompUnit() {
        super(SyntaxType.COMP_UNIT);
        this.declarations = new ArrayList<>();
        this.functionDefinitions = new ArrayList<>();
        this.mainFunction = null;
    }
    
    /**
     * 添加声明
     */
    public void addDeclaration(Decl decl) {
        if (decl != null) {
            declarations.add(decl);
            addChild(decl);
        }
    }
    
    /**
     * 添加函数定义
     */
    public void addFunctionDefinition(FuncDef funcDef) {
        if (funcDef != null) {
            functionDefinitions.add(funcDef);
            addChild(funcDef);
        }
    }
    
    /**
     * 设置主函数
     */
    public void setMainFunction(MainFuncDef mainFunc) {
        this.mainFunction = mainFunc;
        if (mainFunc != null) {
            addChild(mainFunc);
        }
    }
    
    /**
     * 获取所有声明
     */
    public List<Decl> getDeclarations() {
        return new ArrayList<>(declarations);
    }
    
    /**
     * 获取所有函数定义
     */
    public List<FuncDef> getFunctionDefinitions() {
        return new ArrayList<>(functionDefinitions);
    }
    
    /**
     * 获取主函数
     */
    public MainFuncDef getMainFunction() {
        return mainFunction;
    }
    
    /**
     * 获取声明数量
     */
    public int getDeclarationCount() {
        return declarations.size();
    }
    
    /**
     * 获取函数定义数量
     */
    public int getFunctionDefinitionCount() {
        return functionDefinitions.size();
    }
    
    /**
     * 检查是否有主函数
     */
    public boolean hasMainFunction() {
        return mainFunction != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<ASTNode> children = getChildren();
        if(!children.isEmpty()) {
            for (ASTNode child : children) {
                sb.append(child.toString());
            }
        } else {
            if(token!=null) sb.append(token.info());
        }
        if(getToPrint()) sb.append("<").append(getSyntaxType()).append(">");
        return sb.toString();
    }
}