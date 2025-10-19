package frontend.lexer;

import java.util.HashMap;
import java.util.Map;

public class TokenTypeUtil {
    private static final Map<String, TokenType> stringMap = new HashMap<>();
    private static final Map<Character, TokenType> charMap = new HashMap<>();

    static {
        // 关键字
        stringMap.put("const", TokenType.CONSTTK);
        stringMap.put("int", TokenType.INTTK);
        stringMap.put("static", TokenType.STATICTK);
        stringMap.put("break", TokenType.BREAKTK);
        stringMap.put("continue", TokenType.CONTINUETK);
        stringMap.put("if", TokenType.IFTK);
        stringMap.put("else", TokenType.ELSETK);
        stringMap.put("for", TokenType.FORTK);
        stringMap.put("return", TokenType.RETURNTK);
        stringMap.put("void", TokenType.VOIDTK);
        stringMap.put("main", TokenType.MAINTK);
        stringMap.put("printf", TokenType.PRINTFTK);
        stringMap.put("&&", TokenType.AND);
        stringMap.put("||", TokenType.OR);
        stringMap.put("<=", TokenType.LEQ);
        stringMap.put(">=", TokenType.GEQ);
        stringMap.put("==", TokenType.EQL);
        stringMap.put("!=", TokenType.NEQ);

        // 符号
        charMap.put(';', TokenType.SEMICN);
        charMap.put(',', TokenType.COMMA);
        charMap.put('(', TokenType.LPARENT);
        charMap.put(')', TokenType.RPARENT);
        charMap.put('[', TokenType.LBRACK);
        charMap.put(']', TokenType.RBRACK);
        charMap.put('{', TokenType.LBRACE);
        charMap.put('}', TokenType.RBRACE);
        charMap.put('+', TokenType.PLUS);
        charMap.put('-', TokenType.MINU);
        charMap.put('*', TokenType.MULT);
        charMap.put('/', TokenType.DIV);
        charMap.put('%', TokenType.MOD);
        charMap.put('!', TokenType.NOT);
        charMap.put('=', TokenType.ASSIGN);
        charMap.put('<', TokenType.LSS);
        charMap.put('>', TokenType.GRE);
    }

    public static TokenType trans2Type(String str) {
        // 先查关键字
        if (stringMap.containsKey(str)) {
            return stringMap.get(str);
        }
        // 再查多字符运算符
        return switch (str) {
            /*case "&&" -> TokenType.AND;
            case "||" -> TokenType.OR;
            case "<=" -> TokenType.LEQ;
            case ">=" -> TokenType.GEQ;
            case "==" -> TokenType.EQL;
            case "!=" -> TokenType.NEQ;*/
            default -> {
                if (str.matches("^[0-9]+$")) yield TokenType.INTCON;
                if (str.startsWith("\"") && str.endsWith("\"")) yield TokenType.STRCON;
                yield TokenType.IDENFR;
            }
        };
    }

    public static TokenType trans2Type(char c) {
        return charMap.getOrDefault(c, TokenType.IDENFR);
    }

    public static boolean inStringMap(String str) {
        return stringMap.containsKey(str);
    }

    public static boolean inCharMap(char c) {
        return charMap.containsKey(c);
    }
}
