package midend.visit;

import frontend.ast.*;
import frontend.ast.Number;
import frontend.lexer.Token;
import midend.llvm.IrBuilder;
import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.calc.CalculateInstr;
import midend.llvm.instr.comp.ICompInstr;
import midend.llvm.instr.ctrl.BrCondInstr;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.instr.ctrl.CallInstr;
import midend.llvm.instr.io.GetCharInstr;
import midend.llvm.instr.io.GetIntInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.semantic.FunctionSymbol;
import midend.semantic.SymbolManager;
import utils.IrTypeConverter;
import utils.ParserWrapper;

import java.util.ArrayList;
import java.util.List;

public class IrExpVisitor {
    public static IrValue visitExp(Exp exp){
        if (exp == null) {
            return null;
        }
        if(exp instanceof ParserWrapper.ExpWrapper wrapper){
            return visitAddExp((AddExp) wrapper.getWrappedNode());
        }
        throw new RuntimeException("The Wrapped Node of Exp is not AddExp");
    }

    public static IrValue visitAddExp(AddExp addExp){
        List<ASTNode> children = addExp.getChildren();
        AddExp left;
        TokenNode op;
        MulExp right;
        if(children.size()>1){
            left = (AddExp) children.get(0);
            op = (TokenNode) children.get(1);
            right = (MulExp) children.get(2);
        } else {
            left = null;
            op = null;
            right = (MulExp) children.get(0);
        }
        if(left==null){
            return visitMulExp(right);
        } else {
            IrValue leftIrValue = visitAddExp(left);
            IrValue rightIrValue = visitMulExp(right);
            return new CalculateInstr(op.getContent(),leftIrValue,rightIrValue);
        }
    }

    public static IrValue visitMulExp(MulExp mulExp){
        List<ASTNode> children = mulExp.getChildren();
        MulExp left;
        TokenNode op;
        UnaryExp right;
        if(children.size()>1){
            left = (MulExp) children.get(0);
            op = (TokenNode) children.get(1);
            right = (UnaryExp) children.get(2);
        } else {
            left = null;
            op = null;
            right = (UnaryExp) children.get(0);
        }
        if(left==null){
            return visitUnaryExp(right);
        } else {
            IrValue leftIrValue = visitMulExp(left);
            IrValue rightIrValue = visitUnaryExp(right);
            return new CalculateInstr(op.getContent(),leftIrValue,rightIrValue);
        }
    }

    public static IrValue visitUnaryExp(UnaryExp unaryExp){
        if(unaryExp.isPrimary()){
            return visitPrimaryExp(unaryExp.getPrimaryExp());
        } else if(unaryExp.isFunctionCall()){
            return visitFunctionCall(unaryExp.getFuncName().getName(),unaryExp.getFuncRParams());
        } else if(unaryExp.isUnary()){
            return visitUnaryUnary(unaryExp.getUnaryOp(),unaryExp.getUnaryExp());
        } else {
            throw new RuntimeException("The Unary Node of Exp is not UnaryExp");
        }
    }

    public static IrValue visitPrimaryExp(PrimaryExp primaryExp){
        ASTNode pri = primaryExp.getPrimary();
        if(pri instanceof Exp exp) {
            return visitExp(exp);
        } else if(pri instanceof LVal lVal) {
            return IrLValVisitor.visitLValValue(lVal);
        } else if(pri instanceof Number number) {
            return new IrConstInt(number.getValue());
        } else {
            throw new RuntimeException("The Primary Node of Exp is not PrimaryExp");
        }
    }

    public static IrValue visitFunctionCall(String funcName, FuncRParams funcRParams){
        FunctionSymbol functionSymbol = (FunctionSymbol) SymbolManager.getInstance().lookupSymbol(funcName);
        if(!funcName.equals("getint")&&!funcName.equals("getchar")){
            IrFunction irFunction = (IrFunction) functionSymbol.getIrValue();
            if(irFunction==null) throw new RuntimeException("The IrFunction of Function Symbol is null");

            List<IrValue> RParams = new ArrayList<>();
            List<Exp> exps = (funcRParams!=null)?funcRParams.getParams():new ArrayList<>();
            for(Exp exp : exps){
                RParams.add(visitExp(exp));
            }
            return new CallInstr(irFunction,RParams);
        } else {
            if(funcName.equals("getint")){
                return new GetIntInstr();
            } else {
                return new GetCharInstr();
            }
        }

    }

    public static IrValue visitUnaryUnary(UnaryOp unaryOp, UnaryExp unaryExp){
        String op = unaryOp.getOp().getContent();
        IrValue irValue = visitUnaryExp(unaryExp);
        switch (op) {
            case "+" -> {
                return irValue;
            }
            case "-" -> {
                return new CalculateInstr(op,new IrConstInt(0),irValue);
            }
            case "!" -> {
                ICompInstr iCompInstr = new ICompInstr("==",irValue,new IrConstInt(0));
                return IrTypeConverter.toInt32(iCompInstr);
            }
            default -> throw new RuntimeException("The Unary Node of Exp is not UnaryExp");
        }
    }

    // 完成比较表达式的访问
    public static void visitCond(CondExp condExp, IrBasicBlock ifBlock,IrBasicBlock elseBlock){
        visitLOrExp(condExp.getLOrExp(),ifBlock,elseBlock);
    }

    public static void visitLOrExp(LOrExp lOrExp, IrBasicBlock ifBlock, IrBasicBlock elseBlock){
        List<ASTNode> lAndExps = lOrExp.flattenTree(null).getOperands();

        for(int i = 0;i<lAndExps.size()-1;i++){
            LAndExp lAndExp = (LAndExp) lAndExps.get(i);
            IrBasicBlock nextOrBlock = IrBuilder.getNewBasicBlockIr();
            IrValue andValue = visitLAndExp(lAndExp,ifBlock,nextOrBlock);
            andValue = IrTypeConverter.toInt1(andValue);
            BrCondInstr brCondInstr = new BrCondInstr(andValue,ifBlock,nextOrBlock);
            IrBuilder.setCurrentBasicBlock(nextOrBlock);
        }
        visitLAndExp((LAndExp) lAndExps.get(lAndExps.size()-1),ifBlock,elseBlock);
    }

    public static IrValue visitLAndExp(LAndExp lAndExp, IrBasicBlock ifBlock, IrBasicBlock elseBlock){
        List<ASTNode> eqExps = lAndExp.flattenTree(null).getOperands();

        for(int i = 0;i<eqExps.size()-1;i++){
            EqExp eqExp = (EqExp) eqExps.get(i);
            IrBasicBlock nextEqBlock = IrBuilder.getNewBasicBlockIr();
            IrValue eqValue = visitEqExp(eqExp);
            BrCondInstr brCondInstr = new BrCondInstr(eqValue,nextEqBlock,elseBlock);
            IrBuilder.setCurrentBasicBlock(nextEqBlock);
        }
        IrValue eqValue = visitEqExp((EqExp)eqExps.get(eqExps.size()-1));
        BrCondInstr brCondInstr = new BrCondInstr(eqValue,ifBlock,elseBlock);

        return eqValue;
    }

    public static IrValue visitEqExp(EqExp eqExp) {
        List<ASTNode> relExpList = eqExp.flattenTree(null).getOperands();
        List<Token> ops = eqExp.flattenTree(null).getOperators();

        IrValue valueL = visitRelExp((RelExp) relExpList.get(0));
        IrValue valueR = null;
        for (int i = 1; i < relExpList.size(); i++) {
            valueR = visitRelExp((RelExp) relExpList.get(i));
            valueL = IrTypeConverter.toInt32(valueL);
            valueR = IrTypeConverter.toInt32(valueR);

            valueL = new ICompInstr(ops.get(i-1).getContent(),valueL,valueR);
        }
        valueL = IrTypeConverter.toInt32(valueL);

        return new ICompInstr("!=", valueL, new IrConstInt(0));
    }

    public static IrValue visitRelExp(RelExp relExp) {
        List<ASTNode> addExpList = relExp.flattenTree(null).getOperands();
        List<Token> ops = relExp.flattenTree(null).getOperators();

        IrValue valueL = visitAddExp((AddExp) addExpList.get(0));
        IrValue valueR = null;
        for (int i = 1; i < addExpList.size(); i++) {
            valueR = visitAddExp((AddExp) addExpList.get(i));
            valueL = IrTypeConverter.toInt32(valueL);
            valueR = IrTypeConverter.toInt32(valueR);

            valueL = new ICompInstr(ops.get(i-1).getContent(),valueL,valueR);
        }
        return valueL;
    }
}
