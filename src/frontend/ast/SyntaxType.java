package frontend.ast;

/**
 * 语法树节点类型枚举
 * 定义了SysY语言中所有可能的语法节点类型
 */
public enum SyntaxType {
    // 编译单元
    COMP_UNIT("CompUnit",true),
    
    // 声明相关
    DECL("Decl",false),
    CONST_DECL("ConstDecl",true),
    VAR_DECL("VarDecl",true),
    CONST_DEF("ConstDef",true),
    VAR_DEF("VarDef",true),
    INIT_VAL("InitVal",true),
    CONST_INIT_VAL("ConstInitVal",true),
    
    // 函数相关
    FUNC_DEF("FuncDef",true),
    MAIN_FUNC_DEF("MainFuncDef",true),
    FUNC_TYPE("FuncType",true),
    FUNC_FPARAM("FuncFParam",true),
    FUNC_FPARAMS("FuncFParams",true),
    FUNC_RPARAMS("FuncRParams",true),
    
    // 语句相关
    BLOCK("Block",true),
    BLOCK_ITEM("BlockItem",false),
    STMT("Stmt",true),
    FOR_STMT("ForStmt",true),
    IF_STMT("IfStmt",false),
    BREAK_STMT("BreakStmt",false),
    CONTINUE_STMT("ContinueStmt",false),
    RETURN_STMT("ReturnStmt",false),
    EXPR_STMT("ExprStmt",false),
    ASSIGN_STMT("AssignStmt",false),
    FOR_LOOP_STMT("ForLoopStmt",false),
    PRINT_STMT("PrintStmt",false),
    
    // 表达式相关
    EXP("Exp",true),
    LVAL("LVal",true),
    PRIMARY_EXP("PrimaryExp",true),
    UNARY_EXP("UnaryExp",true),
    MUL_EXP("MulExp",true),
    ADD_EXP("AddExp",true),
    REL_EXP("RelExp",true),
    EQ_EXP("EqExp",true),
    LAND_EXP("LAndExp",true),
    LOR_EXP("LOrExp",true),
    COND_EXP("Cond",true),
    CONST_EXP("ConstExp",true),
    
    // 操作符
    UNARY_OP("UnaryOp",true),
    
    // 基本类型
    NUMBER("Number",true),
    IDENT("Ident",false),
    
    // Token节点（用于保留原始token信息）
    TOKEN_NODE("TokenNode",false);
    
    private final String typeName;
    private final boolean toPrint;
    
    SyntaxType(String typeName,boolean toPrint) {
        this.typeName = typeName;
        this.toPrint = toPrint;
    }
    
    @Override
    public String toString() {
        return this.typeName;
    }

    public boolean getToPrint() {
        return toPrint;
    }
}
