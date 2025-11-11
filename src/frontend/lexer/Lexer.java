package frontend.lexer;

import error.Error;
import error.ErrorRecorder;
import error.ErrorType;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.ArrayList;

public class Lexer {
    private PushbackInputStream input;
    private char curChar;
    private final ArrayList<Token> tokens;
    private int curLine;
    // 单例懒加载Lexer
    private Lexer(){
        this.tokens = new ArrayList<>();
        this.curLine = 1;
    }
    private static class Holder {
        private static final Lexer INSTANCE = new Lexer();
    }
    public static Lexer getLexer(){
        return Holder.INSTANCE;
    }

    public void setInput(PushbackInputStream stream) throws IOException{
        this.input = stream;
        this.curChar = (char) ((this.input).read());
    }

    public ArrayList<Token> getTokens(){
        return this.tokens;
    }

    // 读/回溯方法
    public void nextChar() throws IOException{
        this.curChar = (char) ((this.input).read());
    }
    public void sendbackChar() throws IOException{
        this.input.unread(this.curChar);
    }

    // 产生tokens
    public void produceTokens() throws IOException{
        Token token = this.produceToken();
        while(token.getType()!=TokenType.EOF){
            this.tokens.add(token);
            token = this.produceToken();
        }
    }

    public Token produceToken() throws IOException{
        StringBuilder sb = new StringBuilder();
        skipBlank();
        if(isIdent()) {
            return lexerIdent(sb);
        } else if (isDigit()) {
            return lexerDigit(sb);
        } else if (isQuote()) {
            return lexerString(sb);
        } else if (isOp()){
            if (isSingleOp()) {
                return lexerSingleOp();
            } else if (isMultiOp()) {
                return lexerMultiOp(sb);
            } else {
                return lexerLogicOp(sb);
            }
        } else if (isDiv()) {
            return lexerDivOp();
        } else if (isEof()) {
            return new Token(TokenType.EOF, "eof", curLine);
        } else {
            return new Token(TokenType.EOF, "eof", curLine);
        }
    }

    // 判定方法
    public boolean isBlank() {
        // \r便于Windows测试
        return curChar==' '||curChar=='\t'||curChar=='\n'||curChar=='\r';
    }

    public boolean isNewLine(){
        return curChar=='\n';
    }
    
    public boolean isIdent() {
        return isLetter()||curChar=='_';
    }

    public boolean isDigit() {
        return Character.isDigit(curChar);
    }

    public boolean isLetter() {
        return Character.isLetter(curChar);
    }
    
    public boolean isQuote() {
        return curChar=='"';
    }

    public boolean isDiv() {
        return curChar=='/';
    }

    public boolean isEof(){
        return curChar=='\uFFFF';
    }

    // 单独处理/
    public boolean isOp() {
        return curChar=='&'||curChar=='|'||(curChar!='/'&&TokenTypeUtil.inCharMap(curChar)); 
    }

    // 不包含<>=&|
    public boolean isSingleOp() {
        return curChar!='<'&&curChar!='>'&&curChar!='='&&curChar!='!'&&TokenTypeUtil.inCharMap(curChar);
    }

    public boolean isMultiOp() {
        return curChar=='<'||curChar=='>'||curChar=='='||curChar=='!';
    }

    public void skipBlank() throws IOException{
        while(isBlank()){
            if(isNewLine()){
                curLine++;
            }
            nextChar();
        }
    }

    // 分析方法
    public Token lexerIdent(StringBuilder sb) throws IOException{
        while(isIdent()||isDigit()){
            sb.append(curChar);
            nextChar();
        }
        String content = sb.toString();
        return new Token(TokenTypeUtil.trans2Type(content), content, curLine);
    }

    public Token lexerDigit(StringBuilder sb) throws IOException {
        while(isDigit()){
            sb.append(curChar);
            nextChar();
        }
        String content = sb.toString();
        return new Token(TokenType.INTCON, content, curLine);
    }

    public Token lexerString(StringBuilder sb) throws IOException {
        sb.append(curChar);
        nextChar();
        while(!isQuote()) {
            sb.append(curChar);
            nextChar();
        }
        sb.append(curChar);
        nextChar();
        return new Token(TokenType.STRCON, sb.toString(), curLine);
    }

    public Token lexerSingleOp() throws IOException {
        Token token = new Token(TokenTypeUtil.trans2Type(curChar), ""+curChar, curLine);
        nextChar();
        return token;
    }

    public Token lexerMultiOp(StringBuilder sb) throws IOException {
        char first = curChar;
        sb.append(first);
        nextChar();
        if(curChar=='=') {
            sb.append(curChar);
            nextChar();
            return new Token(TokenTypeUtil.trans2Type(sb.toString()), sb.toString(), curLine);
        }
        return new Token(TokenTypeUtil.trans2Type(first), ""+first, curLine);
    }

    public Token lexerLogicOp(StringBuilder sb) throws IOException {
        char first = curChar;
        sb.append(first);
        nextChar();
        if(curChar!=first) {
            ErrorRecorder errorRecorder = ErrorRecorder.getErrorRecorder();
            errorRecorder.addError(new Error(ErrorType.SINGLE_LOGIC_OP, curLine));
        }
        sb.append(first);
        nextChar();
        return new Token(TokenTypeUtil.trans2Type(sb.toString()), sb.toString(), curLine);
    }

    public Token lexerDivOp() throws IOException {
        nextChar();
        return switch (curChar) {
            case '/' -> {
                // 单行注释
                while(!isNewLine() && !isEof()) {
                    nextChar();
                }
                yield produceToken();
            }
            case '*' -> {
                // 多行注释 
                nextChar();
                while(true) {
                    while(curChar!='*') {
                        nextChar();
                    }
                    nextChar();
                    if(isDiv()){
                        nextChar();
                        break;
                    }
                }
                yield produceToken();
            }
            default -> {
                // 除法
                yield new Token(TokenType.DIV, "/", curLine);
            }
        };
    }
}