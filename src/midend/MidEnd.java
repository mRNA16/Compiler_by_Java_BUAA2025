package midend;

import frontend.ast.CompUnit;
import frontend.parser.Parser;
import midend.semantic.SemanticAnalyzer;

public class MidEnd {
    private static CompUnit root;
    private static SemanticAnalyzer semanticAnalyzer;

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

    /**
     * 获取AST根节点
     */
    public static CompUnit getRoot() {
        return root;
    }
}
