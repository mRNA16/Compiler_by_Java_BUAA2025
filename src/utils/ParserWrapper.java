package utils;

import frontend.ast.*;

public class ParserWrapper {
    /**
     * BlockItem包装类，用于解决类型转换问题
     */
    public static class BlockItemWrapper extends BlockItem {
        private final ASTNode wrappedNode;

        public BlockItemWrapper(Decl decl) {
            super(SyntaxType.BLOCK_ITEM);
            this.wrappedNode = decl;
            addChild(decl);
        }

        public BlockItemWrapper(Stmt stmt) {
            super(SyntaxType.BLOCK_ITEM);
            this.wrappedNode = stmt;
            addChild(stmt);
        }

        public ASTNode getWrappedNode() {
            return wrappedNode;
        }
    }

    /**
     * Stmt包装类，用于解决类型转换问题
     */
    public static class StmtWrapper extends Stmt {
        private final ASTNode wrappedNode;

        public StmtWrapper(Block block) {
            super(SyntaxType.STMT);
            this.wrappedNode = block;
            addChild(block);
        }

        public StmtWrapper(AssignmentStmt assignment) {
            super(SyntaxType.STMT);
            this.wrappedNode = assignment;
            addChild(assignment);
        }

        public StmtWrapper(ExprStmt exprStmt) {
            super(SyntaxType.STMT);
            this.wrappedNode = exprStmt;
            addChild(exprStmt);
        }

        public StmtWrapper(IfStmt ifStmt) {
            super(SyntaxType.STMT);
            this.wrappedNode = ifStmt;
            addChild(ifStmt);
        }

        public StmtWrapper(ForLoopStmt forLoopStmt) {
            super(SyntaxType.STMT);
            this.wrappedNode = forLoopStmt;
            addChild(forLoopStmt);
        }

        public StmtWrapper(BreakStmt breakStmt) {
            super(SyntaxType.STMT);
            this.wrappedNode = breakStmt;
            addChild(breakStmt);
        }

        public StmtWrapper(ContinueStmt continueStmt) {
            super(SyntaxType.STMT);
            this.wrappedNode = continueStmt;
            addChild(continueStmt);
        }

        public StmtWrapper(ReturnStmt returnStmt) {
            super(SyntaxType.STMT);
            this.wrappedNode = returnStmt;
            addChild(returnStmt);
        }

        public StmtWrapper(PrintStmt printStmt) {
            super(SyntaxType.STMT);
            this.wrappedNode = printStmt;
            addChild(printStmt);
        }

        public ASTNode getWrappedNode() {
            return wrappedNode;
        }
    }

    /**
     * Exp包装类，用于解决类型转换问题
     */
    public static class ExpWrapper extends Exp {
        private final ASTNode wrappedNode;

        public ExpWrapper(AddExp addExp) {
            super(SyntaxType.EXP);
            this.wrappedNode = addExp;
            addChild(addExp);
        }

        public ASTNode getWrappedNode() {
            return wrappedNode;
        }
    }
}
