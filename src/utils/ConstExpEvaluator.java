package utils;

import frontend.ast.*;
import frontend.ast.Number;
import midend.semantic.Symbol;
import midend.semantic.SymbolManager;
import midend.semantic.VariableSymbol;

import java.util.List;

/**
 * 常量表达式求值器
 * 用于在编译期计算常量表达式的值
 */
public class ConstExpEvaluator {

    public static int eval(ConstExp constExp) {
        return evalAddExp(constExp.getAddExp());
    }

    public static int evalAddExp(AddExp addExp) {
        List<ASTNode> children = addExp.getChildren();
        if (children.size() == 1) {
            return evalMulExp((MulExp) children.get(0));
        } else if (children.size() == 3) {
            ASTNode leftNode = children.get(0);
            int leftVal;
            if (leftNode instanceof AddExp) {
                leftVal = evalAddExp((AddExp) leftNode);
            } else {
                leftVal = evalMulExp((MulExp) leftNode);
            }

            TokenNode op = (TokenNode) children.get(1);
            MulExp right = (MulExp) children.get(2);
            int rightVal = evalMulExp(right);

            if (op.getContent().equals("+")) {
                return leftVal + rightVal;
            } else {
                return leftVal - rightVal;
            }
        }
        throw new RuntimeException("Invalid AddExp structure: children size " + children.size());
    }

    public static int evalMulExp(MulExp mulExp) {
        List<ASTNode> children = mulExp.getChildren();
        if (children.size() == 1) {
            return evalUnaryExp((UnaryExp) children.get(0));
        } else if (children.size() == 3) {
            ASTNode leftNode = children.get(0);
            int leftVal;
            if (leftNode instanceof MulExp) {
                leftVal = evalMulExp((MulExp) leftNode);
            } else {
                leftVal = evalUnaryExp((UnaryExp) leftNode);
            }

            TokenNode op = (TokenNode) children.get(1);
            UnaryExp right = (UnaryExp) children.get(2);
            int rightVal = evalUnaryExp(right);

            String opStr = op.getContent();
            switch (opStr) {
                case "*" -> {
                    return leftVal * rightVal;
                }
                case "/" -> {
                    return leftVal / rightVal;
                }
                case "%" -> {
                    return leftVal % rightVal;
                }
            }
        }
        throw new RuntimeException("Invalid MulExp structure");
    }

    public static int evalUnaryExp(UnaryExp unaryExp) {
        if (unaryExp.getPrimaryExp() != null) {
            return evalPrimaryExp(unaryExp.getPrimaryExp());
        } else if (unaryExp.getUnaryOp() != null) {
            String op = unaryExp.getUnaryOp().getOp().getContent();
            int val = evalUnaryExp(unaryExp.getUnaryExp());
            switch (op) {
                case "+" -> {
                    return val;
                }
                case "-" -> {
                    return -val;
                }
                case "!" -> {
                    return val == 0 ? 1 : 0;
                }
            }
        }
        throw new RuntimeException("No const");
    }

    public static int evalPrimaryExp(PrimaryExp primaryExp) {
        ASTNode primary = primaryExp.getPrimary();

        if (primary instanceof ParserWrapper.ExpWrapper wrapper) {
            return evalAddExp((AddExp) wrapper.getWrappedNode());
        } else if (primary instanceof Number) {
            return ((Number) primary).getValue();
        } else if (primary instanceof LVal) {
            return evalLVal((LVal) primary);
        }
        return 0;
    }

    public static int evalLVal(LVal lVal) {
        String name = lVal.getIdent().getName();
        Symbol symbol = SymbolManager.getInstance().getCurrentTable().lookup(name);

        if (symbol instanceof VariableSymbol varSymbol) {
            if (varSymbol.isConst()) {
                Object initVal = varSymbol.getInitialValue();
                if (lVal.isArrayAccess()) {
                    // 数组访问
                    if (initVal instanceof ConstInitVal constInitVal) {
                        List <ConstExp> constExps = constInitVal.getConstExpList();
                        Exp arrayLengthExp = lVal.getIndexExp();
                        ConstExp arrayLengthConstExp = new ConstExp();
                        if(arrayLengthExp instanceof ParserWrapper.ExpWrapper wrapper){
                            AddExp wrapped = (AddExp) wrapper.getWrappedNode();
                            arrayLengthConstExp.setAddExp(wrapped);
                        } else if(arrayLengthExp instanceof ConstExp arc){
                            arrayLengthConstExp.setAddExp(arc.getAddExp());
                        }
                        int index = eval(arrayLengthConstExp);
                        if (index >= 0 && index < constExps.size()) {
                            return eval(constExps.get(index));
                        }
                        throw new RuntimeException("Array index out of bounds in constant expression: " + index);
                    }
                    throw new RuntimeException("Not ConstInitVal");
                } else {
                    // 标量访问
                    if (initVal instanceof ConstInitVal constInitVal) {
                        List <ConstExp> constExps = constInitVal.getConstExpList();
                        return eval(constExps.get(0));
                    }
                }
            }
        }
        throw new RuntimeException("Cannot evaluate LVal in constant expression: " + name);
    }
}
