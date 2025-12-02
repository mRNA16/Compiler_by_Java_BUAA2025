package frontend.ast;

import frontend.lexer.Token;
import frontend.lexer.TokenType;

/**
 * 一元表达式节点
 */
public class UnaryExp extends Exp {
    private UnaryOp unaryOp;
    private UnaryExp unaryExp;
    private Ident funcName;
    private FuncRParams funcRParams;
    private PrimaryExp primaryExp;
    
    public UnaryExp() {
        super(SyntaxType.UNARY_EXP);
    }
    
    /**
     * 设置一元操作符
     */
    public void setUnaryOp(UnaryOp unaryOp) {
        this.unaryOp = unaryOp;
        if(unaryOp!=null) {
            addChild(unaryOp);
        }
    }
    
    /**
     * 设置操作表达式
     */
    public void setUnaryExp(UnaryExp unaryExp) {
        this.unaryExp = unaryExp;
        if (unaryExp != null) {
            addChild(unaryExp);
        }
    }
    
    /**
     * 设置函数名（函数调用）
     */
    public void setFuncName(Ident funcName) {
        this.funcName = funcName;
        if (funcName != null) {
            addChild(funcName);
        }
    }
    
    /**
     * 设置实参列表（函数调用）
     */
    public void setFuncRParams(FuncRParams funcRParams) {
        this.funcRParams = funcRParams;
        if (funcRParams != null) {
            addChild(funcRParams);
        }
    }

    /**
     * 设置初级表达式
     */
    public void setPrimaryExp(PrimaryExp primaryExp) {
        this.primaryExp = primaryExp;
        if (primaryExp != null) {
            addChild(primaryExp);
        }
    }
    
    /**
     * 获取一元操作符
     */
    public UnaryOp getUnaryOp() {
        return unaryOp;
    }
    
    /**
     * 获取操作表达式
     */
    public UnaryExp getUnaryExp() {
        return unaryExp;
    }
    
    /**
     * 获取函数名
     */
    public Ident getFuncName() {
        return funcName;
    }
    
    /**
     * 获取实参列表
     */
    public FuncRParams getFuncRParams() {
        return funcRParams;
    }

    /**
     * 获取初级表达式
     */
    public PrimaryExp getPrimaryExp() {
        return primaryExp;
    }

    
    /**
     * 检查是否为负数
     */
    public boolean isNegative() {
        return unaryOp != null && unaryOp.getOp().getType() == TokenType.MINU;
    }
    
    /**
     * 检查是否为逻辑非
     */
    public boolean isLogicalNot() {
        return unaryOp != null && unaryOp.getOp().getType() == TokenType.NOT;
    }

    public boolean isPrimary(){
        return primaryExp != null;
    }

    public boolean isFunctionCall() {
        return funcName != null;
    }

    public boolean isUnary() {
        return unaryExp != null;
    }
}