package frontend;

import frontend.ast.CompUnit;
import frontend.lexer.Lexer;
import frontend.lexer.TokenStream;
import frontend.parser.Parser;
import java.io.IOException;
import utils.IOhelper;

public class FrontEnd {
    private static Lexer lexer;
    private static Parser parser;

    public static void initialize() {
        lexer = Lexer.getLexer();
        parser = Parser.getParser();
    }

    public static void setInput() throws IOException{
        if (lexer != null){
            lexer.setInput(IOhelper.getInput());
        }
    }

    public static void startLexer() throws IOException{
        if (lexer != null){
            lexer.produceTokens();
        }
    }

    public static void setTokenStream(TokenStream tokenStream) {
        parser.setTokenStream(tokenStream);
    }
    
    /**
     * 开始语法分析，生成AST
     */
    public static void startParser() {
        if (parser != null) {
            parser.parse();
        }
    }
    
    /**
     * 获取生成的AST根节点
     */
    public static CompUnit getAST() {
        return parser != null ? parser.getAST() : null;
    }
}
