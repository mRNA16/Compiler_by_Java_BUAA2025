package midend.visit;

import frontend.ast.*;
import frontend.lexer.Token;
import midend.llvm.IrBuilder;
import midend.llvm.constant.IrConstInt;
import midend.llvm.constant.IrConstString;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.instr.ctrl.ReturnInstr;
import midend.llvm.instr.io.PrintIntInstr;
import midend.llvm.instr.io.PrintStrInstr;
import midend.llvm.instr.memory.StoreInstr;
import midend.llvm.type.IrPointerType;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrLoop;
import midend.llvm.value.IrValue;
import midend.semantic.SymbolManager;
import utils.IrTypeConverter;
import utils.ParserWrapper;

import java.util.ArrayList;
import java.util.List;

public class IrStmtVisitor {
    public static void visitStmt(Stmt stmt){
        if(stmt instanceof ParserWrapper.StmtWrapper wrapper){
            ASTNode node = wrapper.getWrappedNode();
            switch (node.getSyntaxType()){
                case ASSIGN_STMT -> visitAssignStmt((AssignmentStmt) node);
                case EXPR_STMT -> visitExprStmt((ExprStmt) node);
                case BLOCK -> visitBlockStmt((Block) node);
                case IF_STMT -> visitIfStmt((IfStmt) node);
                case FOR_LOOP_STMT -> visitForLoopStmt((ForLoopStmt) node);
                case RETURN_STMT -> visitReturnStmt((ReturnStmt) node);
                case BREAK_STMT -> visitBreakStmt((BreakStmt) node);
                case CONTINUE_STMT -> visitContinueStmt((ContinueStmt) node);
                case PRINT_STMT -> visitPrintStmt((PrintStmt) node);
            }
        }
    }

    public static void visitAssignStmt(AssignmentStmt assignmentStmt){
        LVal lVal = assignmentStmt.getLVal();
        Exp exp = assignmentStmt.getExp();

        IrValue address = IrLValVisitor.visitLValAddress(lVal);
        IrValue bury = IrExpVisitor.visitExp(exp);

        StoreInstr storeInstr = new StoreInstr(bury, address);
    }

    public static void visitExprStmt(ExprStmt exprStmt){
        Exp exp = exprStmt.getExp();
        IrExpVisitor.visitExp(exp);
    }

    public static void visitBlockStmt(Block block){
        SymbolManager.getInstance().enterScope(false);
        IrBlockVisitor.visitBlock(block);
        SymbolManager.getInstance().exitScope();
    }

    public static void visitIfStmt(IfStmt ifStmt){
        CondExp condExp = ifStmt.getCondition();
        Stmt thenStmt = ifStmt.getThenStmt();
        Stmt elseStmt = ifStmt.getElseStmt();
        IrBasicBlock thenBlock = IrBuilder.getNewBasicBlockIr();
        if(elseStmt != null){
            IrBasicBlock elseBlock = IrBuilder.getNewBasicBlockIr();
            // 构建判断逻辑
            IrExpVisitor.visitCond(condExp,thenBlock,elseBlock);
            BrInstr brInstr = new BrInstr(thenBlock);

            // 构建then块内语句逻辑
            IrBuilder.setCurrentBasicBlock(thenBlock);
            visitStmt(thenStmt);

            // 后续块
            IrBasicBlock followBlock = IrBuilder.getNewBasicBlockIr();
            BrInstr brInstr1 = new BrInstr(followBlock);

            // 构建else块内语句逻辑
            IrBuilder.setCurrentBasicBlock(elseBlock);
            visitStmt(elseStmt);
            BrInstr brInstr2 = new BrInstr(followBlock);

            // 处理后续逻辑
            IrBuilder.setCurrentBasicBlock(followBlock);
        } else {
            IrBasicBlock followBlock = IrBuilder.getNewBasicBlockIr();

            // 构建判断逻辑
            IrExpVisitor.visitCond(condExp,thenBlock,followBlock);
            BrInstr brInstr = new BrInstr(thenBlock);

            // 构建then块内语句逻辑
            IrBuilder.setCurrentBasicBlock(thenBlock);
            visitStmt(thenStmt);
            BrInstr brInstr1 = new BrInstr(followBlock);

            // 处理后续逻辑
            IrBuilder.setCurrentBasicBlock(followBlock);
        }
    }

    public static void visitForLoopStmt(ForLoopStmt forLoopStmt){
        IrBasicBlock condBlock = IrBuilder.getNewBasicBlockIr();
        IrBasicBlock bodyBlock = IrBuilder.getNewBasicBlockIr();
        IrBasicBlock stepBlock = IrBuilder.getNewBasicBlockIr();
        IrBasicBlock followBlock = IrBuilder.getNewBasicBlockIr();

        IrLoop irLoop = new IrLoop(condBlock, bodyBlock, stepBlock, followBlock);
        IrBuilder.loopStackPush(irLoop);

        ForStmt forStmt = forLoopStmt.getInitStmt();
        if(forStmt != null){
            visitForStmt(forStmt);
        }
        BrInstr brInstr = new BrInstr(condBlock);

        // 处理条件块
        IrBuilder.setCurrentBasicBlock(condBlock);
        CondExp condExp = forLoopStmt.getCondition();
        if(condExp != null){
            IrExpVisitor.visitCond(condExp,bodyBlock,followBlock);
        }

        // 处理循环体
        IrBuilder.setCurrentBasicBlock(bodyBlock);
        Stmt body = forLoopStmt.getBody();
        if(body != null){
            visitStmt(body);
        }
        BrInstr brInstr2 = new BrInstr(stepBlock);

        // 处理步进更新
        IrBuilder.setCurrentBasicBlock(stepBlock);
        ForStmt step = forLoopStmt.getUpdateStmt();
        if(step != null){
            visitForStmt(step);
        }
        BrInstr brInstr3 = new BrInstr(condBlock);

        IrBuilder.loopStackPop();

        IrBuilder.setCurrentBasicBlock(followBlock);
    }

    public static void visitForStmt(ForStmt forStmt){
        List<LVal> lVals = forStmt.getLvalList();
        List<Exp> exps = forStmt.getExpList();

        for(int i = 0;i < lVals.size();i++){
            IrValue irValueLVal = IrLValVisitor.visitLValAddress(lVals.get(i));
            IrValue irValueExp = IrExpVisitor.visitExp(exps.get(i));
            irValueExp = IrTypeConverter.convertType(irValueExp,((IrPointerType)irValueLVal.getIrType()).getTargetType());
            StoreInstr storeInstr = new StoreInstr(irValueExp,irValueLVal);
        }
    }

    public static void visitReturnStmt(ReturnStmt returnStmt){
        // 默认是void
        IrValue returnValue = null;
        if(returnStmt.hasReturnValue()){
            Exp exp = returnStmt.getExp();
            returnValue = IrExpVisitor.visitExp(exp);
        } else if(IrBuilder.getCurrentFunction().getReturnType().isInt32Type()){
            returnValue = new IrConstInt(0);
        }

        ReturnInstr returnInstr = new ReturnInstr(returnValue);
    }

    public static void visitBreakStmt(BreakStmt breakStmt){
        new BrInstr(IrBuilder.loopStackPeek().getFollowBlock());
    }

    public static void visitContinueStmt(ContinueStmt continueStmt){
        new BrInstr(IrBuilder.loopStackPeek().getStepBlock());
    }

    public static void visitPrintStmt(PrintStmt printStmt) {
        Token stringToken = printStmt.getStringConst();
        String string = stringToken.getContent();
        List<Exp> exps = printStmt.getExpList();
        List<IrValue> expIrValue = new ArrayList<>();
        int expIndex = 0;

        for (Exp exp : exps) {
            expIrValue.add(IrExpVisitor.visitExp(exp));
        }

        StringBuilder sb = new StringBuilder();
        for(int i = 0;i < string.length();i++){
            char ch = string.charAt(i);
            if(ch=='"') continue;
            else if(ch=='\\') {
                sb.append('\n');
                i++;
            }
            else if (ch=='%'){
                if(!sb.isEmpty()) {
                    String tmp = sb.toString();
                    IrConstString irConstString = IrBuilder.getNewConstStringIr(tmp);
                    PrintStrInstr printStrInstr = new PrintStrInstr(irConstString);
                    sb.setLength(0);
                }
                char nextChar = string.charAt(i+1);
                if(nextChar == 'd'){
                    IrValue irValue = expIrValue.get(expIndex++);
                    irValue = IrTypeConverter.toInt32(irValue);
                    PrintIntInstr printIntInstr = new PrintIntInstr(irValue);
                    i++;
                }
            } else {
                sb.append(ch);
            }
        }
        if(!sb.isEmpty()) {
            String tmp = sb.toString();
            IrConstString irConstString = IrBuilder.getNewConstStringIr(tmp);
            PrintStrInstr printStrInstr = new PrintStrInstr(irConstString);
        }
    }
}
