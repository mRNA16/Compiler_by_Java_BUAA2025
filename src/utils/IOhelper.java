package utils;

import error.Error;
import error.ErrorRecorder;
import frontend.FrontEnd;
import frontend.ast.CompUnit;
import frontend.lexer.Lexer;
import frontend.lexer.Token;
import frontend.parser.Parser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.ArrayList;

public class IOhelper {
    private static PushbackInputStream input = null;
    private static FileOutputStream lexerOutput = null;
    private static FileOutputStream errorOutput = null;
    private static FileOutputStream parserOutput = null;

    // 分离类加载和流初始化
    public static void initialIO() throws IOException {
        input = new PushbackInputStream(new FileInputStream("testfile.txt"),16);
        lexerOutput = new FileOutputStream("lexer.txt");
        errorOutput = new FileOutputStream("error.txt");
        parserOutput = new FileOutputStream("parser.txt");
    }

    // 获得输入流
    public static PushbackInputStream getInput() {
        return input;
    }

    // Lexer输出
    public static void printLexer() throws IOException {
        for(Token token : Lexer.getLexer().getTokens()){
            lexerOutput.write(token.info().getBytes());
        }
    }

    // Error输出
    public static void printError() throws IOException {
        ArrayList<Error> errors = ErrorRecorder.getErrorRecorder().getErrors();
        for (int i = 0; i < errors.size(); i++) {
            errorOutput.write(errors.get(i).info().getBytes());
            if (i != errors.size() - 1) {
                errorOutput.write(System.lineSeparator().getBytes());
            }
        }
    }


    //Parser输出
    public static void printParser() throws IOException {
        CompUnit ast = FrontEnd.getAST();
        if (ast != null) {
            parserOutput.write(ast.toString().getBytes());
        }
    }
}