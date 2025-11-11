import error.ErrorRecorder;
import frontend.FrontEnd;
import frontend.ast.CompUnit;
import frontend.lexer.Lexer;
import frontend.lexer.TokenStream;
import java.io.IOException;

import midend.MidEnd;
import utils.IOhelper;

public class Compiler {
    private final static boolean NEED_ERROR_HOLDER = true;
    private final static boolean NEED_LEXER_OUTPUT = true;
    private final static boolean NEED_PARSER_OUTPUT = true;
    
    public static void main(String[] args) throws IOException{    
        IOhelper.initialIO();

        FrontEnd.initialize();
        FrontEnd.setInput();
        FrontEnd.startLexer();
        FrontEnd.setTokenStream(new TokenStream(Lexer.getLexer().getTokens()));
        FrontEnd.startParser();

        MidEnd.initialize();
        MidEnd.GenerateSymbolTable();

        if(NEED_ERROR_HOLDER) {
            if(ErrorRecorder.haveError()){
                IOhelper.printError();
            }
        }
        /*
        IOhelper.printLexer();
        IOhelper.printParser();
        IOhelper.printSymbolTable();
        */
        if(NEED_LEXER_OUTPUT && !ErrorRecorder.haveError()) {
            IOhelper.printLexer();
        }
        if(NEED_PARSER_OUTPUT && !ErrorRecorder.haveError()) {
            IOhelper.printParser();
        }

        if(NEED_PARSER_OUTPUT && !ErrorRecorder.haveError()) {
            IOhelper.printSymbolTable();
        }
    }
}
