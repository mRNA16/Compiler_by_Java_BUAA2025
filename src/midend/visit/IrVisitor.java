package midend.visit;

import frontend.ast.CompUnit;
import frontend.ast.Decl;
import frontend.ast.FuncDef;
import frontend.ast.MainFuncDef;

import java.util.List;

public class IrVisitor {
    private final CompUnit compUnit;

    public IrVisitor(CompUnit compUnit) {
        this.compUnit = compUnit;
    }

    public void visit(){
        List<Decl> declList = compUnit.getDeclarations();
        for(Decl decl : declList){
            // true表全局作用域
            IrDeclVisitor.visitDecl(decl,true);
        }

        List<FuncDef> funcDefList = compUnit.getFunctionDefinitions();
        for(FuncDef funcDef : funcDefList){
            IrFuncDefVisitor.visitFuncDef(funcDef);
        }

        MainFuncDef mainFuncDef = compUnit.getMainFunction();
        if(mainFuncDef != null){
            IrFuncDefVisitor.visitMainFuncDef(mainFuncDef);
        }
    }
}
