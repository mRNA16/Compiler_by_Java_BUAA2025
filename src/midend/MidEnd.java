package midend;

import frontend.ast.CompUnit;
import frontend.parser.Parser;
import midend.llvm.IrBuilder;
import midend.llvm.IrModule;
import midend.semantic.SemanticAnalyzer;
import midend.visit.IrBlockVisitor;
import midend.visit.IrVisitor;

public class MidEnd {
    private static CompUnit root;
    private static SemanticAnalyzer semanticAnalyzer;
    private static IrModule irModule;

    public static void initialize(){
        root = Parser.getParser().getAST();
        semanticAnalyzer = new SemanticAnalyzer();
    }

    /**
     * 生成符号表并进行语义分析
     */
    public static void GenerateSymbolTable() {
        if (root != null && semanticAnalyzer != null) {
            semanticAnalyzer.analyze(root);
        } else {
            initialize();
            semanticAnalyzer.analyze(root);
        }
    }

    public static void GenerateLLVMIR() {
        if(root !=null){
            irModule = new IrModule();
            IrBuilder.setCurrentModule(irModule);
            IrVisitor irVisitor = new IrVisitor(root);
            irVisitor.visit();
            IrBuilder.skipBlankBlock();
        }
    }

    /**
     * 获取AST根节点
     */
    public static CompUnit getRoot() {
        return root;
    }

    public static IrModule getIrModule() {
        return irModule;
    }
}
