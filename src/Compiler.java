import backend.BackEnd;
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
    private final static boolean NEED_LLVM_IR_OUTPUT = true;
    private final static boolean NEED_MIPS_OUTPUT = true;
    private final static boolean NEED_OPTIMIZE = false;
    private final static boolean ALL_OUTPUT = true;

    public static void main(String[] args) throws IOException {
        IOhelper.initialIO();

        FrontEnd.initialize();
        FrontEnd.setInput();
        FrontEnd.startLexer();
        if (ALL_OUTPUT)
            IOhelper.printLexer();
        FrontEnd.setTokenStream(new TokenStream(Lexer.getLexer().getTokens()));
        FrontEnd.startParser();
        if (ALL_OUTPUT)
            IOhelper.printParser();

        MidEnd.initialize();
        MidEnd.GenerateSymbolTable();
        if (ALL_OUTPUT)
            IOhelper.printSymbolTable();

        if (!ErrorRecorder.haveError()) {
            MidEnd.GenerateLLVMIR();
            if (NEED_OPTIMIZE) {
                new optimize.OptimizeManager(MidEnd.getIrModule()).optimize();
            }
            if (ALL_OUTPUT)
                IOhelper.printLLVMIR();
            BackEnd.initialize(MidEnd.getIrModule());
            BackEnd.generateMips();
            if (ALL_OUTPUT)
                IOhelper.printMips();
        }

        if (NEED_ERROR_HOLDER) {
            if (ErrorRecorder.haveError()) {
                IOhelper.printError();
            }
        }
        if (!ALL_OUTPUT) {
            if (NEED_LEXER_OUTPUT && !ErrorRecorder.haveError()) {
                IOhelper.printLexer();
            }
            if (NEED_PARSER_OUTPUT && !ErrorRecorder.haveError()) {
                IOhelper.printParser();
            }

            if (NEED_PARSER_OUTPUT && !ErrorRecorder.haveError()) {
                IOhelper.printSymbolTable();
            }

            if (NEED_LLVM_IR_OUTPUT && !ErrorRecorder.haveError()) {
                IOhelper.printLLVMIR();
            }

            if (NEED_MIPS_OUTPUT && !ErrorRecorder.haveError()) {
                IOhelper.printMips();
            }
        }
    }
}
