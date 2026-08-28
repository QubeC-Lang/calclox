package tech.ixor.calclox;

import tech.ixor.calclox.ast.Expr;
import tech.ixor.calclox.ast.Statement;
import tech.ixor.calclox.token.Token;
import tech.ixor.calclox.exceptions.CalcLoxRunnerError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Resolves lexical variable references before interpretation.
 * <p>
 * The resolver computes each local expression's environment depth and rejects
 * invalid constructs such as duplicate local declarations, self-initialization,
 * and top-level return statements.
 * </p>
 */
public class Resolver implements Expr.Visitor<Void>, Statement.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION
    }

    /**
     * Resolves all statements in source order.
     *
     * @param statements parsed program
     * @throws CalcLoxRunnerError if the program violates lexical-scope rules
     */
    public void resolve(List<Statement> statements) {
        for (Statement statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Statement Statement) {
        Statement.accept(this);
    }

    void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveFunction(Statement.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            throw new CalcLoxRunnerError("Already a variable named '" + name.lexeme + "' in this scope.", name.line);
        }
        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            throw new CalcLoxRunnerError("Can't read local variable in its own initializer.", expr.name.line);
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBlockStatement(Statement.Block Statement) {
        beginScope();
        resolve(Statement.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitDefineStatement(Statement.Define statement) {
        declare(statement.name);
        define(statement.name);
        return null;
    }

    @Override
    public Void visitExpressionStatement(Statement.Expression Statement) {
        resolve(Statement.expression);
        return null;
    }

    @Override
    public Void visitFunctionStatement(Statement.Function Statement) {
        declare(Statement.name);
        define(Statement.name);
        resolveFunction(Statement, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStatement(Statement.If Statement) {
        resolve(Statement.condition);
        resolve(Statement.thenBranch);
        if (Statement.elseBranch != null) resolve(Statement.elseBranch);
        return null;
    }

    @Override
    public Void visitOutputStatement(Statement.Output statement) {
        resolve(statement.expression);
        return null;
    }

    @Override
    public Void visitReturnStatement(Statement.Return Statement) {
        if (currentFunction == FunctionType.NONE) {
            throw new CalcLoxRunnerError("Can't return from top-level code.", Statement.keyword.line);
        }

        if (Statement.value != null) {
            resolve(Statement.value);
        }

        return null;
    }

    @Override
    public Void visitVarStatement(Statement.Var Statement) {
        declare(Statement.name);
        if (Statement.initializer != null) {
            resolve(Statement.initializer);
        }
        define(Statement.name);
        return null;
    }

    @Override
    public Void visitWhileStatement(Statement.While Statement) {
        resolve(Statement.condition);
        resolve(Statement.body);
        return null;
    }
}
