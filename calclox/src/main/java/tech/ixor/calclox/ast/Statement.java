//> Generated AST: Statement
package tech.ixor.calclox.ast;

import tech.ixor.calclox.token.Token;

import java.util.List;

public abstract class Statement {
    public interface Visitor<R> {
        R visitBlockStatement(Block statement);
        R visitDefineStatement(Define statement);
        R visitExpressionStatement(Expression statement);
        R visitFunctionStatement(Function statement);
        R visitIfStatement(If statement);
        R visitOutputStatement(Output statement);
        R visitReturnStatement(Return statement);
        R visitVarStatement(Var statement);
        R visitWhileStatement(While statement);
    }

    // NestedStatement Classes
    //> statement-block
    public static class Block extends Statement {
        public final List<Statement> statements;

        public Block(List<Statement> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStatement(this);
        }
    }
    //> statement-define
    public static class Define extends Statement {
        public final Token name;

        public Define(Token name) {
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitDefineStatement(this);
        }
    }
    //> statement-expression
    public static class Expression extends Statement {
        public final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStatement(this);
        }
    }
    //> statement-function
    public static class Function extends Statement {
        public final Token name;
        public final List<Token> params;
        public final List<Statement> body;

        public Function(Token name, List<Token> params, List<Statement> body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStatement(this);
        }
    }
    //> statement-if
    public static class If extends Statement {
        public final Expr condition;
        public final Statement thenBranch;
        public final Statement elseBranch;

        public If(Expr condition, Statement thenBranch, Statement elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStatement(this);
        }
    }
    //> statement-output
    public static class Output extends Statement {
        public final Expr expression;

        public Output(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitOutputStatement(this);
        }
    }
    //> statement-return
    public static class Return extends Statement {
        public final Token keyword;
        public final Expr value;

        public Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStatement(this);
        }
    }
    //> statement-var
    public static class Var extends Statement {
        public final Token name;
        public final Expr initializer;

        public Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStatement(this);
        }
    }
    //> statement-while
    public static class While extends Statement {
        public final Expr condition;
        public final Statement body;

        public While(Expr condition, Statement body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStatement(this);
        }
    }

    public abstract <R> R accept(Visitor<R> visitor);
}
//< Generated AST: Statement
