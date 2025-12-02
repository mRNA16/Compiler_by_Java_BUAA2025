package frontend.parser;

import error.Error;
import error.ErrorRecorder;
import error.ErrorType;
import frontend.ast.*;
import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.lexer.TokenType;
import utils.ParserWrapper;

import java.util.List;

/**
 * 递归下降语法分析器
 * 实现SysY语言的完整语法分析功能
 */
public class Parser {
    private TokenStream tokenStream;
    private CompUnit rootNode;

    public Parser() {
        this.tokenStream = null;
        this.rootNode = null;
    }

    private static class Holder {
        private static final Parser INSTANCE = new Parser();
    }

    public static Parser getParser() {
        return Holder.INSTANCE;
    }

    public void setTokenStream(TokenStream tokenStream) {
        this.tokenStream = tokenStream;
    }
    
    /**
     * 开始语法分析，生成AST
     */
    public void parse() {
        if (tokenStream == null) {
            throw new RuntimeException("TokenStream not set");
        }
        rootNode = parseCompUnit();
    }
    
    /**
     * 获取生成的AST根节点
     */
    public CompUnit getAST() {
        return rootNode;
    }
    
    // ==================== 词法分析辅助方法 ====================
    
    /**
     * 获取当前token
     */
    private Token getCurrentToken() {
        return tokenStream.peek(0);
    }
    
    /**
     * 获取当前token类型
     */
    private TokenType getCurrentTokenType() {
        Token token = getCurrentToken();
        return token != null ? token.getType() : TokenType.EOF;
    }
    
    /**
     * 向前查看n个token
     */
    private Token peek(int n) {
        return tokenStream.peek(n);
    }

    /**
     * 存储当前位置入栈
     */
    private void pushFlagIndex() {
        tokenStream.pushFlagIndex();
    }

    /**
     * 弹栈并回退
     */
    private int rollback() {
        return tokenStream.rollback();
    }

    /**
     * 弹栈但不回退
     */
    private void justPop() {
        tokenStream.pop();
    }

    /**
     * 获得上一个Token所在的行号
     */
    private int getLastTokenLineId() {
        return tokenStream.getLastTokenLineId();
    }

    private String getLast10Tokens() {
        List<Token> lst = tokenStream.getLast10Tokens();
        StringBuilder sb = new StringBuilder();
        for(Token t : lst) {
            sb.append(t.info());
        }
        return sb.toString();
    }

    /**
     * 消费当前token
     */
    private void consume(AbstractASTNode node) {
        node.addChild(new TokenNode(getCurrentToken()));
        tokenStream.next();
    }

    /**
     * 对于初始化需要Token的Node解析过程中使用
     */
    private void consumeToken() {
        tokenStream.next();
    }
    
    /**
     * 检查当前token类型是否匹配
     */
    private boolean match(TokenType expected) {
        return getCurrentTokenType() == expected;
    }
    
    /**
     * 期望特定token类型，如果不匹配则抛出异常
     */
    private void expect(AbstractASTNode node, TokenType expected) {
        if (!match(expected)) {
            throw new RuntimeException("Expected " + expected + " but got " + getCurrentTokenType() + getLast10Tokens());
        }
        consume(node);
    }
    
    // ==================== 语法分析规则 ====================
    
    /**
     * 编译单元
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     */
    private CompUnit parseCompUnit() {
        CompUnit compUnit = new CompUnit();
        
        // 解析声明
        while (isDecl()) {
            Decl decl = parseDecl();
            compUnit.addDeclaration(decl);
        }
        
        // 解析函数定义
        while (isFuncDef()) {
            FuncDef funcDef = parseFuncDef();
            compUnit.addFunctionDefinition(funcDef);
        }
        
        // 解析主函数
        MainFuncDef mainFunc = parseMainFuncDef();
        compUnit.setMainFunction(mainFunc);
        
        return compUnit;
    }
    
    /**
     * 声明
     * Decl → ConstDecl | VarDecl
     */
    private Decl parseDecl() {
        if (match(TokenType.CONSTTK)) {
            return parseConstDecl();
        } else {
            return parseVarDecl();
        }
    }
    
    /**
     * 常量声明
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';' //i
     */
    private ConstDecl parseConstDecl() {
        ConstDecl constDecl = new ConstDecl();
        
        // 消费 'const'
        expect(constDecl,TokenType.CONSTTK);
        
        // 消费类型
        expect(constDecl,TokenType.INTTK);
        
        // 解析第一个常量定义
        ConstDef constDef = parseConstDef();
        constDecl.addConstDef(constDef);
        
        // 解析后续的常量定义
        while (match(TokenType.COMMA)) {
            consume(constDecl); // 消费 ','
            constDef = parseConstDef();
            constDecl.addConstDef(constDef);
        }
        
        // 消费 ';'
        processErrorI(constDecl);
        
        return constDecl;
    }
    
    /**
     * 变量声明
     * VarDecl → ['static'] BType VarDef { ',' VarDef } ';' //i
     */
    private VarDecl parseVarDecl() {
        VarDecl varDecl = new VarDecl();
        if(match(TokenType.STATICTK)) {
            expect(varDecl,TokenType.STATICTK);
            varDecl.setStatic(true);
        }
        // 消费类型（目前只支持int）
        expect(varDecl,TokenType.INTTK);
        
        // 解析第一个变量定义
        VarDef varDef = parseVarDef();
        varDecl.addVarDef(varDef);
        
        // 解析后续的变量定义
        while (match(TokenType.COMMA)) {
            consume(varDecl); // 消费 ','
            varDef = parseVarDef();
            varDecl.addVarDef(varDef);
        }

        // 消费 ';'
        processErrorI(varDecl);
        
        return varDecl;
    }
    
    /**
     * 常量定义
     * ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal //k
     */
    private ConstDef parseConstDef() {
        ConstDef constDef = new ConstDef();
        
        // 解析标识符
        Ident ident = parseIdent();
        constDef.setIdent(ident);

        if(match(TokenType.LBRACK)) {
            consume(constDef);
            ConstExp constExp = parseConstExp();
            constDef.setConstExp(constExp);
            processErrorK(constDef);
        }

        // 消费 '='
        expect(constDef,TokenType.ASSIGN);
        
        // 解析常量初始值
        ConstInitVal initVal = parseConstInitVal();
        constDef.setInitVal(initVal);
        
        return constDef;
    }
    
    /**
     * 变量定义
     * VarDef → Ident [ '[' ConstExp ']' ] [ '=' InitVal ] //k
     */
    private VarDef parseVarDef() {
        VarDef varDef = new VarDef();
        
        // 解析标识符
        Ident ident = parseIdent();
        varDef.setIdent(ident);

        if(match(TokenType.LBRACK)) {
            consume(varDef);
            ConstExp constExp = parseConstExp();
            varDef.setConstExp(constExp);
            processErrorK(varDef);
        }

        // 如果有初始值
        if (match(TokenType.ASSIGN)) {
            consume(varDef); // 消费 '='
            InitVal initVal = parseInitVal();
            varDef.setInitVal(initVal);
        }
        
        return varDef;
    }
    
    /**
     * 常量初始值
     * ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
     */
    private ConstInitVal parseConstInitVal() {
        ConstInitVal constInitVal = new ConstInitVal();
        if(match(TokenType.LBRACE)) {
            constInitVal.setIsArray(true);
            consume(constInitVal);
            if(match(TokenType.RBRACE)) {
                consume(constInitVal);
            } else {
                ConstExp constExp = parseConstExp();
                constInitVal.addConstExp(constExp);
                while(match(TokenType.COMMA)) {
                    consume(constInitVal);
                    constInitVal.addConstExp(parseConstExp());
                }
                consume(constInitVal);
            }
        } else {
            ConstExp constExp = parseConstExp();
            constInitVal.addConstExp(constExp);
            constInitVal.setIsArray(false);
        }
        return constInitVal;
    }
    
    /**
     * 初始值
     * InitVal → Exp  | '{' [ Exp { ',' Exp } ] '}'
     */
    private InitVal parseInitVal() {
        InitVal initVal = new InitVal();
        if(match(TokenType.LBRACE)) {
            initVal.setIsArray(true);
            consume(initVal);
            if(match(TokenType.RBRACE)) {
                consume(initVal);
            } else {
                Exp exp = parseExp();
                initVal.addExp(exp);
                while(match(TokenType.COMMA)) {
                    consume(initVal);
                    initVal.addExp(parseExp());
                }
                consume(initVal);
            }
        } else {
            Exp exp = parseExp();
            initVal.addExp(exp);
            initVal.setIsArray(false);
        }
        return initVal;
    }
    
    /**
     * 函数定义
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block //j
     */
    private FuncDef parseFuncDef() {
        FuncDef funcDef = new FuncDef();
        
        // 解析函数类型
        FuncType funcType = parseFuncType();
        funcDef.setFuncType(funcType);
        
        // 解析函数名
        Ident ident = parseIdent();
        funcDef.setIdent(ident);
        
        // 消费 '('
        expect(funcDef,TokenType.LPARENT);
        
        // 解析形参列表
        if (match(TokenType.RPARENT)) {
            // 消费 ')'
            consume(funcDef);
        } else {
            // 能解析到FuncParams
            if(match(TokenType.INTTK)) {
                FuncFParams funcFParams = parseFuncFParams();
                funcDef.setFuncFParams(funcFParams);
                processErrorJ(funcDef);
            } else {
                processErrorJ(funcDef);
            }
        }

        // 解析函数体
        Block block = parseBlock();
        funcDef.setBlock(block);
        
        return funcDef;
    }
    
    /**
     * 主函数定义
     * MainFuncDef → 'int' 'main' '(' ')' Block //j
     */
    private MainFuncDef parseMainFuncDef() {
        MainFuncDef mainFuncDef = new MainFuncDef();
        
        // 消费 'int'
        expect(mainFuncDef,TokenType.INTTK);
        
        // 消费 'main'
        expect(mainFuncDef,TokenType.MAINTK);
        
        // 消费 '('
        expect(mainFuncDef,TokenType.LPARENT);
        
        // 消费 ')'
        processErrorJ(mainFuncDef);
        
        // 解析函数体
        Block block = parseBlock();
        mainFuncDef.setBlock(block);
        
        return mainFuncDef;
    }
    
    /**
     * 函数类型
     * FuncType → 'void' | 'int'
     */
    private FuncType parseFuncType() {
        Token token = getCurrentToken();
        if (match(TokenType.VOIDTK) || match(TokenType.INTTK)) {
            consumeToken();
        } else {
            throw new RuntimeException("Expected function irType but got " + getCurrentTokenType());
        }
        return new FuncType(token);
    }
    
    /**
     * 函数形参列表
     * FuncFParams → FuncFParam { ',' FuncFParam }
     */
    private FuncFParams parseFuncFParams() {
        FuncFParams funcFParams = new FuncFParams();
        
        // 解析第一个参数
        FuncFParam funcFParam = parseFuncFParam();
        funcFParams.addParam(funcFParam);
        
        // 解析后续参数
        while (match(TokenType.COMMA)) {
            consume(funcFParams); // 消费 ','
            funcFParam = parseFuncFParam();
            funcFParams.addParam(funcFParam);
        }
        
        return funcFParams;
    }
    
    /**
     * 函数形参
     * FuncFParam → BType Ident ['[' ']'] //k
     */
    private FuncFParam parseFuncFParam() {
        FuncFParam funcFParam = new FuncFParam();
        
        // 消费 'int'
        expect(funcFParam,TokenType.INTTK);
        
        // 解析参数名
        Ident ident = parseIdent();
        funcFParam.setIdent(ident);

        if(match(TokenType.LBRACK)) {
            consume(funcFParam);
            funcFParam.setIsArray(true);
            if(match(TokenType.RBRACK)) {
                consume(funcFParam);
            } else {
                processErrorK(funcFParam);
            }
        }
        
        return funcFParam;
    }
    
    /**
     * 代码块
     * Block → '{' { BlockItem } '}'
     */
    private Block parseBlock() {
        Block block = new Block();
        
        // 消费 '{'
        expect(block,TokenType.LBRACE);
        
        // 解析块项
        while (!match(TokenType.RBRACE) && !match(TokenType.EOF)) {
            BlockItem blockItem = parseBlockItem();
            block.addBlockItem(blockItem);
        }
        
        // 消费 '}'
        expect(block,TokenType.RBRACE);
        
        return block;
    }
    
    /**
     * 块项
     * BlockItem → Decl | Stmt
     */
    private BlockItem parseBlockItem() {
        if (isDecl()) {
            Decl decl = parseDecl();
            // 创建一个包装类来处理类型转换
            return new ParserWrapper.BlockItemWrapper(decl);
        } else {
            Stmt stmt = parseStmt();
            return new ParserWrapper.BlockItemWrapper(stmt);
        }
    }
    
    /**
     * 语句
     * Stmt → LVal '=' Exp ';' |  //i
     *        [Exp] ';' |   // i
     *        Block |
     *       'if' '(' Cond ')' Stmt [ 'else' Stmt ] |  //j
     *       'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt  |
     *       'break' ';' |  //j
     *       'continue' ';' |   //j
     *       'return' [Exp] ';' |   //j
     *       'printf''('StringConst {','Exp}')'';'  //i j
     */
    private Stmt parseStmt() {
        TokenType currentType = getCurrentTokenType();
        
        switch (currentType) {
            case LBRACE: // {
                // 包装Block为Stmt
                return new ParserWrapper.StmtWrapper(parseBlock());
            case IFTK:
                return new ParserWrapper.StmtWrapper(parseIfStmt());
            case FORTK:
                return new ParserWrapper.StmtWrapper(parseForLoopStmt());
            case BREAKTK:
                return new ParserWrapper.StmtWrapper(parseBreakStmt());
            case CONTINUETK:
                return new ParserWrapper.StmtWrapper(parseContinueStmt());
            case RETURNTK:
                return new ParserWrapper.StmtWrapper(parseReturnStmt());
            case IDENFR:
                // 可能是左值或函数调用
                return parseAssignmentOrExprStmt();
            case PRINTFTK:
                return new ParserWrapper.StmtWrapper(parsePrintStmt());
            default:
                // 可能是表达式语句
                return new ParserWrapper.StmtWrapper(parseExprStmt());
        }
    }
    
    /**
     * 赋值语句或函数调用
     */
    private Stmt parseAssignmentOrExprStmt() {
        pushFlagIndex();
        ErrorRecorder.getErrorRecorder().negative();
        Exp exp = parseExp();
        ErrorRecorder.getErrorRecorder().positive();
        if (match(TokenType.ASSIGN)) {
            // 赋值语句
            rollback();
            return new ParserWrapper.StmtWrapper(parseAssignmentStmt());
        } else {
            rollback();
            return new ParserWrapper.StmtWrapper(parseExprStmt());
        }
    }

    /**
     * 赋值语句  //i
     */
    private AssignmentStmt parseAssignmentStmt() {
        AssignmentStmt assignmentStmt = new AssignmentStmt();
        LVal lval = parseLVal();
        assignmentStmt.setLVal(lval);
        consume(assignmentStmt); // 消费 '='
        Exp exp = parseExp();
        assignmentStmt.setExp(exp);
        processErrorI(assignmentStmt);
        return assignmentStmt;
    }

    /**
     * 表达式语句  //i
     */
    private ExprStmt parseExprStmt() {
        ExprStmt exprStmt = new ExprStmt();

        if (!match(TokenType.SEMICN)) {
            Exp exp = parseExp();
            exprStmt.setExp(exp);
        }

        // 消费 ';'
        processErrorI(exprStmt);

        return exprStmt;
    }

    /**
     * if语句 //j
     */
    private IfStmt parseIfStmt() {
        IfStmt ifStmt = new IfStmt();
        
        // 消费 'if'
        expect(ifStmt,TokenType.IFTK);
        
        // 消费 '('
        expect(ifStmt,TokenType.LPARENT);
        
        // 解析条件表达式
        CondExp condition = parseCondExp();
        ifStmt.setCondition(condition);
        
        // 消费 ')'
        processErrorJ(ifStmt);
        
        // 解析then语句
        Stmt thenStmt = parseStmt();
        ifStmt.setThenStmt(thenStmt);
        
        // 解析else语句
        if (match(TokenType.ELSETK)) {
            consume(ifStmt); // 消费 'else'
            Stmt elseStmt = parseStmt();
            ifStmt.setElseStmt(elseStmt);
        }
        
        return ifStmt;
    }
    
    /**
     * for循环语句
     */
    private ForLoopStmt parseForLoopStmt() {
        ForLoopStmt forLoopStmt = new ForLoopStmt();
        
        // 消费 'for'
        expect(forLoopStmt,TokenType.FORTK);
        
        // 消费 '('
        expect(forLoopStmt,TokenType.LPARENT);
        
        // 解析初始化语句
        if (!match(TokenType.SEMICN)) {
            ForStmt initStmt = parseForStmt();
            forLoopStmt.setInitStmt(initStmt);
        }
        
        // 消费 ';'
        expect(forLoopStmt,TokenType.SEMICN);
        
        // 解析条件表达式
        if (!match(TokenType.SEMICN)) {
            CondExp condition = parseCondExp();
            forLoopStmt.setCondition(condition);
        }
        
        // 消费 ';'
        expect(forLoopStmt,TokenType.SEMICN);
        
        // 解析更新语句
        if (!match(TokenType.RPARENT)) {
            ForStmt updateStmt = parseForStmt();
            forLoopStmt.setUpdateStmt(updateStmt);
        }
        
        // 消费 ')'
        expect(forLoopStmt,TokenType.RPARENT);
        
        // 解析循环体
        Stmt body = parseStmt();
        forLoopStmt.setBody(body);
        
        return forLoopStmt;
    }

    /**
     * for语句
     */
    private ForStmt parseForStmt() {
        ForStmt forStmt = new ForStmt();
        LVal lVal = parseLVal();
        forStmt.addLVal(lVal);
        consume(forStmt);
        Exp exp = parseExp();
        forStmt.addExpr(exp);
        while(match(TokenType.COMMA)){
            consume(forStmt);
            lVal = parseLVal();
            forStmt.addLVal(lVal);
            consume(forStmt);
            exp = parseExp();
            forStmt.addExpr(exp);
        }
        return forStmt;
    }

    /**
     * print语句 //i j
     */
    private PrintStmt parsePrintStmt() {
        PrintStmt printStmt = new PrintStmt();
        consume(printStmt); //printf
        consume(printStmt); //(
        printStmt.setStringConst(getCurrentToken());
        consumeToken();
        while(match(TokenType.COMMA)){
            consume(printStmt);
            Exp exp = parseExp();
            printStmt.addExp(exp);
        }
        processErrorJ(printStmt); //)
        processErrorI(printStmt); //;
        return printStmt;
    }

    /**
     * break语句 //i
     */
    private BreakStmt parseBreakStmt() {
        BreakStmt breakStmt = new BreakStmt();
        
        // 消费 'break'
        expect(breakStmt,TokenType.BREAKTK);
        
        // 消费 ';'
        processErrorI(breakStmt);
        
        return breakStmt;
    }
    
    /**
     * continue语句 //i
     */
    private ContinueStmt parseContinueStmt() {
        ContinueStmt continueStmt = new ContinueStmt();
        
        // 消费 'continue'
        expect(continueStmt,TokenType.CONTINUETK);
        
        // 消费 ';'
        processErrorI(continueStmt);
        
        return continueStmt;
    }
    
    /**
     * return语句 //i
     */
    private ReturnStmt parseReturnStmt() {
        ReturnStmt returnStmt = new ReturnStmt();
        
        // 消费 'return'
        expect(returnStmt,TokenType.RETURNTK);
        
        // 解析返回值表达式（可选）
        if (!match(TokenType.SEMICN)) {
            Exp exp = parseExp();
            returnStmt.setExp(exp);
        }
        
        // 消费 ';'
        processErrorI(returnStmt);
        
        return returnStmt;
    }
    
    /**
     * 表达式
     * Exp → AddExp
     */
    private Exp parseExp() {
        AddExp addExp = parseAddExp();
        return new ParserWrapper.ExpWrapper(addExp);
    }

    /**
     * 条件表达式
     * Cond → LOrExp
     */
    private CondExp parseCondExp() {
        CondExp condExp = new CondExp();
        LOrExp lOrExp = parseLOrExp();
        condExp.setLOrExp(lOrExp);
        return condExp;
    }
    
    /**
     * 逻辑或表达式
     * LOrExp → LAndExp  | LOrExp '||' LAndExp
     * 这里产生了左递归，改写文法为
     * LOrExp → LAndExp { '||' LAndExp }
     */
    private LOrExp parseLOrExp() {
        LOrExp lOrExp = new LOrExp();
        LAndExp lAndExp = parseLAndExp();
        lOrExp.addLAndExp(lAndExp);
        while(match(TokenType.OR)) {
            Token op = getCurrentToken();
            lOrExp.addOperator(op);
            consumeToken();
            lAndExp = parseLAndExp();
            lOrExp.addLAndExp(lAndExp);
        }
        return (LOrExp) lOrExp.buildLeftAssociativeTree();
    }
    
    /**
     * 逻辑与表达式
     * 原文法产生了左递归，改写文法为
     * LAndExp → EqExp { '&&' EqExp }
     */
    private LAndExp parseLAndExp() {
        LAndExp lAndExp = new LAndExp();
        EqExp eqExp = parseEqExp();
        lAndExp.addEqExp(eqExp);
        while (match(TokenType.AND)) {
            Token op = getCurrentToken();
            lAndExp.addOperator(op);
            consumeToken();
            eqExp = parseEqExp();
            lAndExp.addEqExp(eqExp);
        }
        return (LAndExp) lAndExp.buildLeftAssociativeTree();
    }
    
    /**
     * 等值表达式
     * EqExp → RelExp { ('==' | '!=') RelExp }
     */
    private EqExp parseEqExp() {
        EqExp eqExp = new EqExp();
        RelExp left = parseRelExp();
        eqExp.addRelExp(left);
        while (match(TokenType.EQL) || match(TokenType.NEQ)) {
            Token op = getCurrentToken();
            eqExp.addOp(op);
            consumeToken();
            RelExp right = parseRelExp();
            eqExp.addRelExp(right);
        }

        return (EqExp) eqExp.buildLeftAssociativeTree();
    }
    
    /**
     * 关系表达式
     * RelExp → AddExp { ('<' | '<=' | '>' | '>=') AddExp }
     */
    private RelExp parseRelExp() {
        RelExp relExp = new RelExp();
        AddExp left = parseAddExp();
        relExp.addAddExp(left);
        while (match(TokenType.LSS) || match(TokenType.LEQ) || 
               match(TokenType.GRE) || match(TokenType.GEQ)) {
            Token op = getCurrentToken();
            relExp.addOp(op);
            consumeToken(); // 消费操作符
            AddExp right = parseAddExp();
            relExp.addAddExp(right);
        }

        return (RelExp) relExp.buildLeftAssociativeTree();
    }
    
    /**
     * 加减表达式
     * AddExp → MulExp { ('+' | '-') MulExp }
     */
    private AddExp parseAddExp() {
        AddExp addExp = new AddExp();
        MulExp left = parseMulExp();
        addExp.addMulExp(left);
        while (match(TokenType.PLUS) || match(TokenType.MINU)) {
            Token op = getCurrentToken();
            addExp.addOp(op);
            consumeToken(); // 消费操作符
            MulExp right = parseMulExp();
            addExp.addMulExp(right);
        }
        return (AddExp) addExp.buildLeftAssociativeTree();
    }
    
    /**
     * 乘除表达式
     * MulExp → UnaryExp { ('*' | '/' | '%') UnaryExp }
     */
    private MulExp parseMulExp() {
        MulExp mulExp = new MulExp();
        UnaryExp left = parseUnaryExp();
        mulExp.addUnaryExp(left);
        while (match(TokenType.MULT) || match(TokenType.DIV) || match(TokenType.MOD)) {
            Token op = getCurrentToken();
            mulExp.addOp(op);
            consumeToken(); // 消费操作符
            UnaryExp right = parseUnaryExp();
            mulExp.addUnaryExp(right);
        }
        return (MulExp) mulExp.buildLeftAssociativeTree();
    }
    
    /**
     * 一元表达式
     * UnaryExp → PrimaryExp | UnaryOp UnaryExp | Ident '(' [FuncRParams] ')'  // j
     */
    private UnaryExp parseUnaryExp() {
        UnaryExp unaryExp = new UnaryExp();
        
        if (match(TokenType.PLUS) || match(TokenType.MINU) || match(TokenType.NOT)) {
            // 一元操作符
            Token op = getCurrentToken();
            UnaryOp unaryOp = new UnaryOp();
            unaryOp.setOp(op);
            unaryExp.setUnaryOp(unaryOp);
            consumeToken(); // 消费操作符
            UnaryExp subUnaryExp = parseUnaryExp();
            unaryExp.setUnaryExp(subUnaryExp);
        } else if (match(TokenType.IDENFR) && peek(1).getType() == TokenType.LPARENT) {
            // 函数调用
            Token funcNameToken = getCurrentToken();
            consumeToken(); // 消费函数名
            Ident funcName = new Ident(funcNameToken);
            unaryExp.setFuncName(funcName);
            expect(unaryExp,TokenType.LPARENT); // 消费 '('
            /* 解析实参列表
             这里可能缺少小括号同时无实参
             考虑根据FIRST级判断是否存在实参
             如：func(;
             */
            if (match(TokenType.LPARENT)||match(TokenType.IDENFR)||match(TokenType.INTCON)||
                    match(TokenType.PLUS)||match(TokenType.MINU)||match(TokenType.NOT)) {
                FuncRParams funcRParams = parseFuncRParams();
                unaryExp.setFuncRParams(funcRParams);
            }
            processErrorJ(unaryExp);
        } else {
            // 基本表达式
            unaryExp.setPrimaryExp(parsePrimaryExp());
        }
        return unaryExp;
    }
    
    /**
     * 基本表达式
     * PrimaryExp → '(' Exp ')' | LVal | Number
     */
    private PrimaryExp parsePrimaryExp() {
        PrimaryExp primaryExp = new PrimaryExp();
        
        if (match(TokenType.LPARENT)) {
            // 括号表达式
            consume(primaryExp); // 消费 '('
            Exp exp = parseExp();
            primaryExp.setPrimary(exp);
            processErrorJ(primaryExp);
        } else if (match(TokenType.IDENFR)) {
            // 标识符（左值）
            LVal lVal = parseLVal();
            primaryExp.setPrimary(lVal);
        } else if (match(TokenType.INTCON)) {
            // 数字
            frontend.ast.Number number = parseNumber();
            primaryExp.setPrimary(number);
        } else {
            throw new RuntimeException("Expected primary expression but got " + getCurrentTokenType() + getLast10Tokens() );
        }
        
        return primaryExp;
    }
    
    /**
     * 左值
     * LVal → Ident ['[' Exp ']']
     */
    private LVal parseLVal() {
        LVal lVal = new LVal();
        
        // 解析标识符
        Ident ident = parseIdent();
        lVal.setIdent(ident);
        
        // 检查是否有数组索引
        if (match(TokenType.LBRACK)) {
            consume(lVal); // 消费 '['
            Exp indexExp = parseExp();
            lVal.setIndexExp(indexExp);
            processErrorK(lVal);
        }
        
        return lVal;
    }
    
    /**
     * 常量表达式
     * ConstExp → AddExp
     */
    private ConstExp parseConstExp() {
        ConstExp constExp = new ConstExp();
        
        AddExp exp = parseAddExp();
        constExp.setAddExp(exp);
        
        return constExp;
    }
    
    /**
     * 函数实参列表
     * FuncRParams → Exp { ',' Exp }
     */
    private FuncRParams parseFuncRParams() {
        FuncRParams funcRParams = new FuncRParams();
        
        // 解析第一个实参
        Exp exp = parseExp();
        funcRParams.addParam(exp);
        // 解析后续实参
        while (match(TokenType.COMMA)) {
            consume(funcRParams); // 消费 ','
            exp = parseExp();
            funcRParams.addParam(exp);
        }
        return funcRParams;
    }
    
    /**
     * 标识符
     */
    private Ident parseIdent() {
        Token token = getCurrentToken();
        if (!match(TokenType.IDENFR)) {
            throw new RuntimeException("Expected identifier but got " + getCurrentTokenType());
        }
        consumeToken();
        return new Ident(token);
    }
    
    /**
     * 数字
     */
    private frontend.ast.Number parseNumber() {
        Token token = getCurrentToken();
        if (!match(TokenType.INTCON)) {
            throw new RuntimeException("Expected number but got " + getCurrentTokenType());
        }
        consumeToken();
        return new frontend.ast.Number(token);
    }
    
    // 判断方法

    /**
     * 判断是否为声明
     */
    private boolean isDecl() {
        if(match(TokenType.CONSTTK) || match(TokenType.STATICTK)) {
            return true;
        } else if(match(TokenType.INTTK)){
            return peek(2).getType() != TokenType.LPARENT;
        } else {
            return false;
        }
    }
    
    /**
     * 判断是否为函数定义
     */
    private boolean isFuncDef() {
        return match(TokenType.VOIDTK) || (match(TokenType.INTTK) && peek(1).getType()!=TokenType.MAINTK &&
               peek(2).getType() == TokenType.LPARENT);
    }

    private void processErrorI(AbstractASTNode node){
        if(match(TokenType.SEMICN)) consume(node);
        else {
            node.addChild(new TokenNode(new Token(TokenType.SEMICN,";",getLastTokenLineId())));
            ErrorRecorder.getErrorRecorder().addError(new Error(ErrorType.MISSING_SEMICOLON,getLastTokenLineId()));
        }
    }

    private void processErrorJ(AbstractASTNode node){
        if(match(TokenType.RPARENT)) consume(node);
        else {
            node.addChild(new TokenNode(new Token(TokenType.RPARENT,")",getLastTokenLineId())));
            ErrorRecorder.getErrorRecorder().addError(new Error(ErrorType.MISSING_RIGHT_PARENTHESES,getLastTokenLineId()));
        }
    }

    private void processErrorK(AbstractASTNode node){
        if(match(TokenType.RBRACK)) consume(node);
        else {
            node.addChild(new TokenNode(new Token(TokenType.RBRACK,"]",getLastTokenLineId())));
            ErrorRecorder.getErrorRecorder().addError(new Error(ErrorType.MISSING_RIGHT_BRACKETS,getLastTokenLineId()));
        }
    }
}
