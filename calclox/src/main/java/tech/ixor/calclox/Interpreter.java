package tech.ixor.calclox;

import tech.ixor.calclox.ast.Expr;
import tech.ixor.calclox.ast.Statement;
import tech.ixor.calclox.token.Token;
import tech.ixor.calclox.token.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes resolved CalcLox syntax trees against lexical environments.
 * <p>
 * The interpreter owns the globals for one run, installs the native {@code clock}
 * and {@code eval} functions, forwards output to a {@link CalculatorFrontend}, and
 * synchronizes numeric declarations and assignments with the frontend variable map.
 * Instances are stateful and are not intended to be shared between runs.
 * </p>
 */
public class Interpreter implements Expr.Visitor<Object>, Statement.Visitor<Void> {
    /** Global environment containing native functions and top-level declarations. */
    public final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();
    private Map<String, Double> frontendVariables = null;

    /**
     * The frontend of the calculator that calls the interpreter.
     * The interpreter interacts with the frontend to get the input and output.
     */
    private final CalculatorFrontend calculatorFrontend;

    /**
     * Creates an interpreter connected to a host frontend.
     *
     * @param calculatorFrontend frontend that supplies variables and receives output
     */
    public Interpreter(CalculatorFrontend calculatorFrontend) {
        this.calculatorFrontend = calculatorFrontend;
        this.frontendVariables = calculatorFrontend.variables();
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fun clock>";
            }
        });
    }

    /**
     * Executes statements sequentially and converts internal runtime failures to the
     * public {@link tech.ixor.calclox.exceptions.CalcLoxRunnerError} type.
     *
     * @param statements resolved statements to execute
     */
    public void interpret(List<Statement> statements) {
        EvalCallableImpl evalCallable = new EvalCallableImpl();
        globals.define("eval", evalCallable);

        try {
            for (Statement statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            CalcLoxRunner.runtimeError(error);
        }
    }

    Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Statement statement) {
        statement.accept(this);
    }

    /**
     * Records the lexical distance computed for a local expression by {@link Resolver}.
     *
     * @param expr local variable expression
     * @param depth number of enclosing environments between the expression and declaration
     */
    public void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    /**
     * Executes a block in a temporary environment and restores the previous environment
     * even when execution exits through an error or function return.
     *
     * @param statements block body
     * @param environment lexical environment for the block
     */
    public void executeBlock(List<Statement> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Statement statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    //> Binary Operators
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
    //< Binary Operators

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        // Update the frontend's variables if the variable is assigned a new value
        if (frontendVariables != null && value instanceof Double) {
            frontendVariables.put(expr.name.lexeme, (Double) value);
        }

        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
            case SLASH -> {
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            }
            case BANG_EQUAL -> {
                return !isEqual(left, right);
            }
            case EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
            case GREATER -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            }
            case LESS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            }
        }
        ;

        throw new RuntimeError(expr.operator, "Non-binary operator visited in visitBinaryExpr: " + expr.operator.lexeme);
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) { // [in-order]
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable function)) {
            throw new RuntimeError(expr.paren, "Only functions can be called in CalcLox.");
        }

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
        }
        throw new RuntimeError(expr.operator, "Non-unary operator visited in visitUnaryExpr: " + expr.operator.lexeme);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, expr.name.lexeme);
        } else {
            return globals.get(expr.name);
        }
    }

    @Override
    public Void visitBlockStatement(Statement.Block statement) {
        executeBlock(statement.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitDefineStatement(Statement.Define statement) {
        if (frontendVariables == null || !frontendVariables.containsKey(statement.name.lexeme)) {
            throw new RuntimeError(statement.name, "User-defined variable '" + statement.name.lexeme + "' not found in the calculator frontend.");
        }
        Double value = frontendVariables.get(statement.name.lexeme);
        environment.define(statement.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitExpressionStatement(Statement.Expression statement) {
        evaluate(statement.expression);
        return null;
    }

    @Override
    public Void visitFunctionStatement(Statement.Function statement) {
        LoxFunction function = new LoxFunction(statement, environment);
        environment.define(statement.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStatement(Statement.If statement) {
        if (isTruthy(evaluate(statement.condition))) {
            execute(statement.thenBranch);
        } else if (statement.elseBranch != null) {
            execute(statement.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitOutputStatement(Statement.Output statement) {
        Object value = evaluate(statement.expression);
        calculatorFrontend.output(String.valueOf(value));
        return null;
    }

    @Override
    public Void visitReturnStatement(Statement.Return statement) {
        Object value = null;
        if (statement.value != null) value = evaluate(statement.value);
        throw new Return(value);
    }

    @Override
    public Void visitVarStatement(Statement.Var statement) {
        Object value = null;
        if (statement.initializer != null) {
            value = evaluate(statement.initializer);
        }
        environment.define(statement.name.lexeme, value);

        // Update the frontend's variables if the variable is declared with an initial value
        if (frontendVariables != null && value instanceof Double) {
            frontendVariables.put(statement.name.lexeme, (Double) value);
        }

        return null;
    }

    @Override
    public Void visitWhileStatement(Statement.While statement) {
        while (isTruthy(evaluate(statement.condition))) {
            execute(statement.body);
        }
        return null;
    }
}
