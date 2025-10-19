package frontend.lexer;

public enum TokenType {
    // 标识符 & 常量
    IDENFR,     // Ident
    INTCON,     // IntConst
    STRCON,     // StringConst

    // 关键字
    CONSTTK,    // const
    INTTK,      // int
    STATICTK,   // static
    BREAKTK,    // break
    CONTINUETK, // continue
    IFTK,       // if
    ELSETK,     // else
    FORTK,      // for
    RETURNTK,   // return
    VOIDTK,     // void
    MAINTK,     // main
    PRINTFTK,   // printf

    // 运算符
    NOT,    // !
    AND,    // &&
    OR,     // ||
    LSS,    // <
    LEQ,    // <=
    GRE,    // >
    GEQ,    // >=
    EQL,    // ==
    NEQ,    // !=
    ASSIGN, // =
    PLUS,   // +
    MINU,   // -
    MULT,   // *
    DIV,    // /
    MOD,    // %

    // 界符
    SEMICN, // ;
    COMMA,  // ,
    LPARENT,// (
    RPARENT,// )
    LBRACK, // [
    RBRACK, // ]
    LBRACE, // {
    RBRACE,  // }

    // Else TokenType
    EOF;
}
