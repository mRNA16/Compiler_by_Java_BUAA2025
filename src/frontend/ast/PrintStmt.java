package frontend.ast;

import frontend.lexer.Token;

import java.util.ArrayList;
import java.util.List;

public class PrintStmt extends Stmt {
    private Token stringConst;
    private List<Exp> expList = new ArrayList<>();
    public PrintStmt() {
        super(SyntaxType.PRINT_STMT);
    }

    public void setStringConst(Token stringConst) {
        this.stringConst = stringConst;
        if(stringConst != null) {
            addChild(new TokenNode(stringConst));
        }
    }

    public void addExp(Exp exp) {
        expList.add(exp);
        if(exp != null) {
            addChild(exp);
        }
    }

    public Token getStringConst() {
        return stringConst;
    }

    public List<Exp> getExpList() {
        return expList;
    }
}
