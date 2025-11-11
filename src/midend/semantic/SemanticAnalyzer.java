package midend.semantic;

import error.Error;
import error.ErrorRecorder;
import error.ErrorType;
import frontend.ast.*;
import frontend.lexer.Token;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.ParserWrapper;

/**
 * 语义分析器
 * 负责遍历AST，建立符号表，检查语义错误
 */
public class SemanticAnalyzer {
    private final SymbolManager symbolManager;
    private String currentFunctionReturnType;  // 当前函数的返回类型
    // private final Map<Integer,Boolean> inLoop;  // 是否在循环中
    private boolean inLoop;
    private final Map<Integer,Boolean> hasReturnStatement;  // 当前函数是否有return语句

    public SemanticAnalyzer() {
        this.symbolManager = SymbolManager.getInstance();
        this.currentFunctionReturnType = null;
        // this.inLoop = new HashMap<>();
        this.inLoop = false;
        this.hasReturnStatement = new HashMap<>();
    }

    public int getCurrentScopeNumber(){
        return symbolManager.getCurrentTable().getScopeNumber();
    }

    public int getNextScopeNumber(){
        return symbolManager.getNextScopeNumber();
    }

    /**
     * 分析编译单元
     */
    public void analyze(CompUnit compUnit) {
        symbolManager.initialize();

        // 处理所有声明和函数定义
        for (Decl decl : compUnit.getDeclarations()) {
            analyzeDeclaration(decl);
        }

        for (FuncDef funcDef : compUnit.getFunctionDefinitions()) {
            analyzeFunction(funcDef);
        }

        // 分析主函数
        if (compUnit.hasMainFunction()) {
            analyzeMainFunction(compUnit.getMainFunction());
        }
    }

    /**
     * 分析声明
     */
    private void analyzeDeclaration(Decl decl) {
        if (decl instanceof ConstDecl constDecl) {
            analyzeConstDecl(constDecl);
        } else if (decl instanceof VarDecl varDecl) {
            analyzeVarDecl(varDecl);
        }
    }

    /**
     * 分析常量声明
     */
    private void analyzeConstDecl(ConstDecl constDecl) {
        boolean isGlobal = symbolManager.isGlobalScope();

        for (ConstDef constDef : constDecl.getConstDefs()) {
            analyzeConstDef(constDef, isGlobal);
        }
    }

    /**
     * 分析常量定义
     */
    private void analyzeConstDef(ConstDef constDef, boolean isGlobal) {
        Ident ident = constDef.getIdent();
        if (ident == null) {
            return;
        }

        String name = ident.getName();
        int lineNumber = ident.getLineNumber();

        // 确定类型（简化处理，假设都是int类型）
        SymbolType type = SymbolType.CONST_INT;

        // 检查是否有数组维度
        List<Integer> dimensions = new ArrayList<>();
        if (constDef.getConstExp() != null) {
            // 这里简化处理，实际应该计算常量表达式
            dimensions.add(0);  // 占位
            type = SymbolType.CONST_INT_ARRAY;
        }

        VariableSymbol symbol = new VariableSymbol(name, type, lineNumber, isGlobal, dimensions);
        symbol.setInitialValue(constDef.getInitVal());

        symbolManager.addSymbol(symbol, lineNumber);
    }

    /**
     * 分析变量声明
     */
    private void analyzeVarDecl(VarDecl varDecl) {
        boolean isGlobal = symbolManager.isGlobalScope();
        boolean isStatic = varDecl.isStatic();

        for (VarDef varDef : varDecl.getVarDefs()) {
            analyzeVarDef(varDef, isGlobal, isStatic);
        }
    }

    /**
     * 分析变量定义
     */
    private void analyzeVarDef(VarDef varDef, boolean isGlobal, boolean isStatic) {
        Ident ident = varDef.getIdent();
        if (ident == null) {
            return;
        }

        String name = ident.getName();
        int lineNumber = ident.getLineNumber();

        // 确定类型
        SymbolType type = SymbolType.INT;

        // 检查是否有数组维度
        List<Integer> dimensions = new ArrayList<>();
        if (varDef.getConstExp() != null) {
            // 这里简化处理，实际应该计算常量表达式
            dimensions.add(0);  // 占位
            type = SymbolType.INT_ARRAY;
        }

        VariableSymbol symbol = new VariableSymbol(name, type, lineNumber, isGlobal, isStatic, dimensions);
        if (varDef.hasInitVal()) {
            symbol.setInitialValue(varDef.getInitVal());
        }

        symbolManager.addSymbol(symbol, lineNumber);
    }

    /**
     * 分析函数定义
     */
    private void analyzeFunction(FuncDef funcDef) {
        Ident ident = funcDef.getIdent();
        if (ident == null) {
            return;
        }

        String funcName = ident.getName();
        int lineNumber = ident.getLineNumber();

        // 确定返回类型
        FuncType funcType = funcDef.getFuncType();
        SymbolType returnType = funcType != null && funcType.isVoid()
                ? SymbolType.VOID
                : SymbolType.INT;

        // 分析形参
        List<SymbolType> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();

        if (funcDef.getFuncFParams() != null) {
            for (FuncFParam param : funcDef.getFuncFParams().getParams()) {
                // 检查参数类型（可能是数组）
                SymbolType paramType = SymbolType.INT;
                if (param.isArray()) {
                    paramType = SymbolType.INT_ARRAY;
                }
                paramTypes.add(paramType);
                if (param.getIdent() != null) {
                    paramNames.add(param.getIdent().getName());
                }
            }
        }

        FunctionSymbol funcSymbol = new FunctionSymbol(
                funcName, returnType, lineNumber, paramTypes, paramNames);

        symbolManager.addSymbol(funcSymbol, lineNumber);

        // 进入函数作用域
        symbolManager.enterScope();
        currentFunctionReturnType = returnType.toString();
        hasReturnStatement.put(getCurrentScopeNumber(), Boolean.FALSE);

        // 将形参添加到符号表（参数属于函数内部作用域）
        if (funcDef.getFuncFParams() != null) {
            for (FuncFParam param : funcDef.getFuncFParams().getParams()) {
                Ident paramIdent = param.getIdent();
                if (paramIdent != null) {
                    // 检查参数类型（可能是数组）
                    SymbolType paramType = SymbolType.INT;
                    if(param.isArray()){
                        paramType = SymbolType.INT_ARRAY;
                    }
                    VariableSymbol paramSymbol = new VariableSymbol(
                            paramIdent.getName(), paramType, paramIdent.getLineNumber(), false);
                    symbolManager.addSymbol(paramSymbol, paramIdent.getLineNumber());
                }
            }
        }

        // 分析函数体（函数体的Block不创建新作用域，直接在当前作用域中分析）
        Block block= funcDef.getBlock();
        if (block != null) {
            analyzeFunctionBlock(block);
        }

        // 检查返回值
        if (returnType != SymbolType.VOID && !hasReturnStatement.get(getCurrentScopeNumber())) {
            if (block != null) {
                ErrorRecorder.getErrorRecorder().addError(
                        new Error(ErrorType.MISSING_RETURN, block.getChildren().get(block.getChildren().size()-1).getLineNumber()));
            }
        }

        // 退出函数作用域
        symbolManager.exitScope();
        currentFunctionReturnType = null;
    }

    /**
     * 分析主函数
     */
    private void analyzeMainFunction(MainFuncDef mainFunc) {
        // 进入主函数作用域
        symbolManager.enterScope();
        currentFunctionReturnType = "int";
        hasReturnStatement.put(getCurrentScopeNumber(),Boolean.FALSE);

        // 分析函数体（main函数的Block不创建新作用域）
        Block block = mainFunc.getBlock();
        if (block != null) {
            analyzeFunctionBlock(block);
        }

        // 检查返回值（main函数必须有return）
        if (!hasReturnStatement.get(getCurrentScopeNumber())) {
            int lineNumber = 0;
            if (block != null) {
                lineNumber = block.getChildren().get(block.getChildren().size()-1).getLineNumber();
            }
            ErrorRecorder.getErrorRecorder().addError(
                    new Error(ErrorType.MISSING_RETURN, lineNumber));
        }

        // 退出主函数作用域
        symbolManager.exitScope();
        currentFunctionReturnType = null;
    }

    /**
     * 分析代码块（会创建新作用域）
     */
    private void analyzeBlock(Block block) {
        symbolManager.enterScope();

        for (BlockItem item : block.getBlockItems()) {
            analyzeBlockItem(item);
        }

        symbolManager.exitScope();
    }

    /**
     * 分析函数体Block（不创建新作用域，直接在函数作用域中分析）
     */
    private void analyzeFunctionBlock(Block block) {
        // 函数体的Block不创建新作用域，直接在函数作用域中分析
        for(int i = 0;i<block.getBlockItems().size();i++) {
            if(i==block.getBlockItems().size()-1) {
                BlockItem lastItem = block.getBlockItems().get(i);
                if (lastItem instanceof ParserWrapper.BlockItemWrapper wrapper) {
                    ASTNode wrapped = wrapper.getWrappedNode();
                    if (wrapped instanceof Stmt stmt) {
                        if (stmt instanceof ReturnStmt) {
                            hasReturnStatement.put(getCurrentScopeNumber(), Boolean.TRUE);
                        } else if(stmt instanceof ParserWrapper.StmtWrapper stmtWrapper) {
                            ASTNode wrapped2 = stmtWrapper.getWrappedNode();
                            if (wrapped2 instanceof ReturnStmt) {
                                hasReturnStatement.put(getCurrentScopeNumber(), Boolean.TRUE);
                            }
                        }
                    }
                }
            }
            analyzeBlockItem(block.getBlockItems().get(i));
        }
    }

    /**
     * 分析块项
     */
    private void analyzeBlockItem(BlockItem item) {
        if (item instanceof ParserWrapper.BlockItemWrapper wrapper) {
            ASTNode wrapped = wrapper.getWrappedNode();
            if (wrapped instanceof Decl decl) {
                analyzeDeclaration(decl);
            } else if (wrapped instanceof Stmt stmt) {
                analyzeStatement(stmt);
            }
        }
    }

    /**
     * 分析语句
     */
    private void analyzeStatement(Stmt stmt) {
        if (stmt instanceof AssignmentStmt assignStmt) {
            analyzeAssignment(assignStmt);
        } else if (stmt instanceof ReturnStmt returnStmt) {
            analyzeReturn(returnStmt);
        } else if (stmt instanceof IfStmt ifStmt) {
            analyzeIf(ifStmt);
        } else if (stmt instanceof ForLoopStmt forLoopStmt) {
            analyzeForLoop(forLoopStmt);
        } else if (stmt instanceof BreakStmt breakStmt) {
            analyzeBreak(breakStmt);
        } else if (stmt instanceof ContinueStmt continueStmt) {
            analyzeContinue(continueStmt);
        } else if (stmt instanceof ParserWrapper.StmtWrapper stmtWrapper) {
            ASTNode wrapped = stmtWrapper.getWrappedNode();
            if (wrapped instanceof Block block) {
                analyzeBlock(block);
            } else if (wrapped instanceof AssignmentStmt assignStmt) {
                analyzeAssignment(assignStmt);
            } else if (wrapped instanceof ExprStmt exprStmt) {
                if (exprStmt.getExp() != null) {
                    analyzeExpression(exprStmt.getExp());
                }
            } else if (wrapped instanceof IfStmt ifStmt) {
                analyzeIf(ifStmt);
            } else if (wrapped instanceof ForLoopStmt forLoopStmt) {
                analyzeForLoop(forLoopStmt);
            } else if (wrapped instanceof BreakStmt breakStmt) {
                analyzeBreak(breakStmt);
            } else if (wrapped instanceof ContinueStmt continueStmt) {
                analyzeContinue(continueStmt);
            } else if (wrapped instanceof ReturnStmt returnStmt) {
                analyzeReturn(returnStmt);
            } else if (wrapped instanceof PrintStmt printStmt) {
                analyzePrintStmt(printStmt);
            }
        } else if (stmt instanceof ExprStmt exprStmt) {
            if (exprStmt.getExp() != null) {
                analyzeExpression(exprStmt.getExp());
            }
        } else if (stmt instanceof PrintStmt printStmt) {
            analyzePrintStmt(printStmt);
        }
    }

    /**
     * 分析赋值语句
     */
    private void analyzeAssignment(AssignmentStmt assignStmt) {
        LVal lVal = assignStmt.getLVal();
        if (lVal == null || lVal.getIdent() == null) {
            return;
        }

        String name = lVal.getIdent().getName();
        int lineNumber = lVal.getIdent().getLineNumber();

        // 查找符号
        Symbol symbol = symbolManager.lookupSymbol(name);
        if (symbol == null) {
            ErrorRecorder.getErrorRecorder().addError(
                    new Error(ErrorType.NAME_UNDEFINED, lineNumber));
            return;
        }

        // 检查是否为常量
        if (symbol instanceof VariableSymbol varSymbol && varSymbol.isConst()) {
            ErrorRecorder.getErrorRecorder().addError(
                    new Error(ErrorType.MODIFY_CONSTANT, lineNumber));
        }

        // 分析表达式
        if (assignStmt.getExp() != null) {
            analyzeExpression(assignStmt.getExp());
        }
    }

    /**
     * 分析return语句
     */
    private void analyzeReturn(ReturnStmt returnStmt) {
        // hasReturnStatement.put(getCurrentScopeNumber(),Boolean.TRUE);

        if (returnStmt.hasReturnValue()) {
            // 有返回值，检查类型
            analyzeExpression(returnStmt.getExp());

            if ("void".equals(currentFunctionReturnType)) {
                ErrorRecorder.getErrorRecorder().addError(
                        new Error(ErrorType.RETURN_TYPE_MISMATCH, returnStmt.getChildren().get(0).getLineNumber()));
            }
        }
    }

    /**
     * 分析if语句
     */
    private void analyzeIf(IfStmt ifStmt) {
        if (ifStmt.getCondition() != null) {
            analyzeExpression(ifStmt.getCondition());
        }

        if (ifStmt.getThenStmt() != null) {
            analyzeStatement(ifStmt.getThenStmt());
        }

        if (ifStmt.getElseStmt() != null) {
            analyzeStatement(ifStmt.getElseStmt());
        }
    }

    /**
     * 分析for循环
     */
    private void analyzeForLoop(ForLoopStmt forLoopStmt) {
        // 分析初始化
        if (forLoopStmt.getInitStmt() != null) {
            analyzeForInit(forLoopStmt.getInitStmt());
        }

        // 分析条件
        if (forLoopStmt.getCondition() != null) {
            analyzeExpression(forLoopStmt.getCondition());
        }

        // 分析更新
        if (forLoopStmt.getUpdateStmt() != null) {
            analyzeForUpdate(forLoopStmt.getUpdateStmt());
        }

        // 进入循环

        boolean oldInLoop = inLoop;
        inLoop = true;


        // 分析循环体
        if (forLoopStmt.getBody() != null) {
            /*if(forLoopStmt.getBody() instanceof ParserWrapper.StmtWrapper stmtWrapper) {
                if(stmtWrapper.getWrappedNode() instanceof Block) {
                    inLoop.put(getNextScopeNumber(),Boolean.TRUE);
                }
            }*/
            analyzeStatement(forLoopStmt.getBody());
        }

        // 退出循环
        inLoop = oldInLoop;
    }

    /**
     * 分析for循环初始化语句
     */
    private void analyzeForInit(ForStmt forStmt) {
        // ForStmt包含LVal和Exp列表，可能是赋值语句
        for (LVal lVal : forStmt.getLvalList()) {
            String name = lVal.getIdent().getName();
            int lineNumber = lVal.getIdent().getLineNumber();
            Symbol symbol = symbolManager.lookupSymbol(name);
            // 检查是否为常量
            if (symbol instanceof VariableSymbol varSymbol && varSymbol.isConst()) {
                ErrorRecorder.getErrorRecorder().addError(
                        new Error(ErrorType.MODIFY_CONSTANT, lineNumber));
            }
            analyzeLVal(lVal);
        }
        for (Exp exp : forStmt.getExpList()) {
            analyzeExpression(exp);
        }
    }

    /**
     * 分析for循环更新语句
     */
    private void analyzeForUpdate(ForStmt forStmt) {
        // ForStmt包含LVal和Exp列表，可能是赋值语句
        for (LVal lVal : forStmt.getLvalList()) {
            String name = lVal.getIdent().getName();
            int lineNumber = lVal.getIdent().getLineNumber();
            Symbol symbol = symbolManager.lookupSymbol(name);
            // 检查是否为常量
            if (symbol instanceof VariableSymbol varSymbol && varSymbol.isConst()) {
                ErrorRecorder.getErrorRecorder().addError(
                        new Error(ErrorType.MODIFY_CONSTANT, lineNumber));
            }
            analyzeLVal(lVal);
        }
        for (Exp exp : forStmt.getExpList()) {
            analyzeExpression(exp);
        }
    }

    /**
     * 分析break语句
     */
    private void analyzeBreak(BreakStmt breakStmt) {
        if (!inLoop) {
        // if (!inLoop.getOrDefault(getCurrentScopeNumber(),false)) {
            ErrorRecorder.getErrorRecorder().addError(
                    new Error(ErrorType.BREAK_CONTINUE_OUTSIDE_LOOP, breakStmt.getChildren().get(0).getLineNumber()));
        }
    }

    /**
     * 分析continue语句
     */
    private void analyzeContinue(ContinueStmt continueStmt) {
        if (!inLoop) {
        // if (!inLoop.getOrDefault(getCurrentScopeNumber(),false)) {
            ErrorRecorder.getErrorRecorder().addError(
                    new Error(ErrorType.BREAK_CONTINUE_OUTSIDE_LOOP, continueStmt.getChildren().get(0).getLineNumber()));
        }
    }

    /**
     * 分析表达式
     */
    private void analyzeExpression(Exp exp) {
        if (exp == null) {
            return;
        }

        // 递归分析子表达式
        for (ASTNode child : exp.getChildren()) {
            if (child instanceof UnaryExp unaryExp) {
                analyzeUnaryExp(unaryExp);
            } else if (child instanceof CondExp condExp) {
                analyzeExpression(condExp);
            } else if (child instanceof Exp childExp) {
                analyzeExpression(childExp);
            } else if (child instanceof LVal lVal) {
                analyzeLVal(lVal);
            } else if (child instanceof AddExp addExp) {
                // AddExp继承RecursionNode，需要分析其操作数
                for (ASTNode operand : addExp.getOperands()) {
                    if (operand instanceof Exp operandExp) {
                        analyzeExpression(operandExp);
                    } else {
                        analyzeASTNode(operand);
                    }
                }
            } else if (child instanceof MulExp mulExp) {
                for (ASTNode operand : mulExp.getOperands()) {
                    if (operand instanceof Exp operandExp) {
                        analyzeExpression(operandExp);
                    } else {
                        analyzeASTNode(operand);
                    }
                }
            } else if (child instanceof RecursionNode recursionNode) {
                for (ASTNode operand : recursionNode.getOperands()) {
                    analyzeASTNode(operand);
                }
            } else {
                analyzeASTNode(child);
            }
        }
    }

    /**
     * 分析AST节点（通用方法）
     */
    private void analyzeASTNode(ASTNode node) {
        if (node instanceof UnaryExp unaryExp) {
            analyzeUnaryExp(unaryExp);
        } else if (node instanceof Exp exp) {
            analyzeExpression(exp);
        } else if (node instanceof LVal lVal) {
            analyzeLVal(lVal);
        } else {
            // 递归分析子节点
            if (node != null) {
                for (ASTNode child : node.getChildren()) {
                    analyzeASTNode(child);
                }
            }
        }
    }

    /**
     * 分析左值
     */
    private void analyzeLVal(LVal lVal) {
        if (lVal == null || lVal.getIdent() == null) {
            return;
        }

        String name = lVal.getIdent().getName();
        int lineNumber = lVal.getIdent().getLineNumber();

        // 查找符号
        Symbol symbol = symbolManager.lookupSymbol(name);
        if (symbol == null) {
            ErrorRecorder.getErrorRecorder().addError(
                    new Error(ErrorType.NAME_UNDEFINED, lineNumber));
        }

        // 分析索引表达式
        if (lVal.isArrayAccess() && lVal.getIndexExp() != null) {
            analyzeExpression(lVal.getIndexExp());
        }
    }

    /**
     * 分析一元表达式
     */
    private void analyzeUnaryExp(UnaryExp unaryExp) {
        if (unaryExp == null) {
            return;
        }

        // 如果是函数调用
        if (unaryExp.isFunctionCall()) {
            analyzeFunctionCall(unaryExp);
        }

        // 分析子表达式
        if (unaryExp.getUnaryExp() != null) {
            analyzeUnaryExp(unaryExp.getUnaryExp());
        }

        if (unaryExp.getPrimaryExp() != null) {
            analyzePrimaryExp(unaryExp.getPrimaryExp());
        }
    }

    /**
     * 分析函数调用
     */
    private void analyzeFunctionCall(UnaryExp unaryExp) {
        Ident funcName = unaryExp.getFuncName();
        if (funcName == null) {
            return;
        }

        String name = funcName.getName();
        int lineNumber = funcName.getLineNumber();

        // 查找函数符号
        Symbol symbol = symbolManager.lookupSymbol(name);
        if (symbol == null) {
            ErrorRecorder.getErrorRecorder().addError(
                    new Error(ErrorType.NAME_UNDEFINED, lineNumber));
        } else if (!(symbol instanceof FunctionSymbol)) {
            ErrorRecorder.getErrorRecorder().addError(
                    new Error(ErrorType.NAME_UNDEFINED, lineNumber));
        }

        // 分析实参
        List<SymbolType> argTypes = new ArrayList<>();
        if (unaryExp.getFuncRParams() != null) {
            for (Exp arg : unaryExp.getFuncRParams().getParams()) {
                analyzeExpression(arg);
                // 获取实参的类型（考虑数组）
                SymbolType argType = getExpressionType(arg);
                argTypes.add(argType);
            }
        }

        if(symbol instanceof FunctionSymbol funcSymbol){
            // 检查参数数量
            if (argTypes.size() != funcSymbol.getParameterCount()) {
                ErrorRecorder.getErrorRecorder().addError(
                        new Error(ErrorType.FUNCTION_PARAM_COUNT_MISMATCH, lineNumber));
                return;
            }

            // 检查参数类型
            if (!funcSymbol.matchParameters(argTypes)) {
                ErrorRecorder.getErrorRecorder().addError(
                        new Error(ErrorType.FUNCTION_PARAM_TYPE_MISMATCH, lineNumber));
            }
        }
    }

    /**
     * 分析初级表达式
     */
    private void analyzePrimaryExp(PrimaryExp primaryExp) {
        if (primaryExp == null) {
            return;
        }

        // 递归分析子节点
        for (ASTNode child : primaryExp.getChildren()) {
            if (child instanceof Exp exp) {
                analyzeExpression(exp);
            } else if (child instanceof LVal lVal) {
                analyzeLVal(lVal);
            }
        }
    }

    /**
     * 获取表达式的类型
     * 考虑数组类型的情况
     */
    private SymbolType getExpressionType(Exp exp) {
        if (exp == null) {
            return SymbolType.INT;
        }

        // 递归查找表达式中的 LVal
        LVal lVal = findLValInExpression(exp);

        if (lVal != null) {
            // 如果 LVal 有索引（数组访问），返回 INT（数组元素类型）
            if (lVal.isArrayAccess()) {
                return SymbolType.INT;
            }

            // 如果 LVal 没有索引，查找符号表确定类型
            if (lVal.getIdent() != null) {
                String name = lVal.getIdent().getName();
                Symbol symbol = symbolManager.lookupSymbol(name);

                if (symbol instanceof VariableSymbol varSymbol) {
                    SymbolType type = varSymbol.getType();
                    // 如果是数组类型，返回数组类型；否则返回 INT
                    if (type.isArray()) {
                        return type;
                    } else {
                        return SymbolType.INT;
                    }
                }
            }
        }

        // 默认返回 INT（数字、函数调用结果等）
        return SymbolType.INT;
    }

    /**
     * 在表达式中查找 LVal 节点
     * 如果表达式中包含 LVal，返回最左边的 LVal
     */
    private LVal findLValInExpression(ASTNode node) {
        if (node == null) {
            return null;
        }
        // 递归查找子节点中的 LVal
        for (ASTNode child : node.getChildren()) {
            if (child instanceof LVal lVal) {
                return lVal;
            } else {
                // 递归查找子表达式
                LVal lVal = findLValInExpression(child);
                if (lVal != null) {
                    return lVal;
                }
            }
        }

        return null;
    }

    /**
     * 分析printf语句
     */
    private void analyzePrintStmt(PrintStmt printStmt) {
        if (printStmt == null) {
            return;
        }

        // 分析所有表达式
        for (Exp exp : printStmt.getExpList()) {
            analyzeExpression(exp);
        }

        // 检查printf格式匹配
        Token stringConst = printStmt.getStringConst();
        if (stringConst != null) {
            String formatString = stringConst.getContent();
            int expCount = printStmt.getExpList().size();

            // 计算格式字符串中%d的数量
            int formatSpecifierCount = countFormatSpecifiers(formatString);

            // 检查格式说明符数量是否与表达式数量匹配
            if (formatSpecifierCount != expCount) {
                ErrorRecorder.getErrorRecorder().addError(
                        new Error(ErrorType.PRINTF_FORMAT_MISMATCH, printStmt.getChildren().get(0).getLineNumber()));
            }
        }
    }

    /**
     * 统计格式字符串中%d的数量
     */
    private int countFormatSpecifiers(String formatString) {
        if (formatString == null || formatString.length() < 2) {
            return 0;
        }

        int count = 0;
        // 移除首尾的引号
        String content = formatString;
        if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }

        // 查找%d
        for (int i = 0; i < content.length() - 1; i++) {
            if (content.charAt(i) == '%' && content.charAt(i + 1) == 'd') {
                count++;
                i++;
            }
        }

        return count;
    }
}

