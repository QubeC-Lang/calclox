package tech.ixor.calclox;

import tech.ixor.calclox.ast.Expr;
import tech.ixor.calclox.ast.Statement;
import tech.ixor.calclox.token.Token;

import java.util.List;

/**
 * The AST Printer for the CalcLox language that prints the generated AST structure from the code.
 */
public class AstPrinter implements Expr.Visitor<String>, Statement.Visitor<String> {
    //> Printing Utility Methods
    String print(Expr expr) {
        return "[" + expr.accept(this) + "]";
    }

    String print(Statement statement) {
        return "[" + statement.accept(this) + "]";
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    private String parenthesize2(String name, Object... parts) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        transform(builder, parts);
        builder.append(")");

        return builder.toString();
    }

    private void transform(StringBuilder builder, Object... parts) {
        for (Object part : parts) {
            builder.append(" ");
            switch (part) {
                case Expr expr -> builder.append(expr.accept(this));
                case Statement statement -> builder.append(statement.accept(this));
                case Token token -> builder.append(token.lexeme);
                case List list -> transform(builder, list.toArray());
                case null, default -> builder.append(part);
            }
        }
    }
    //< Printing Utility Methods

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return String.format("(Expr) Assign %s to %s", expr.value, expr.name.lexeme);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return "(Expr) " + parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return parenthesize2("(Expr) Call", expr.callee, expr.arguments);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("(Expr) Group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "(Expr) Literal: nil";
        return "(Expr) Literal: " + expr.value;
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return "(Expr) " + parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return "(Expr) Variable: " + expr.name.lexeme;
    }

    @Override
    public String visitBlockStatement(Statement.Block statement) {
        StringBuilder builder = new StringBuilder();
        builder.append("(block");
        for (Statement stmt : statement.statements) {
            builder.append("\n\t").append(stmt.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitDefineStatement(Statement.Define statement) {
        return parenthesize2("var", statement.name);
    }

    @Override
    public String visitExpressionStatement(Statement.Expression statement) {
        return parenthesize("expression", statement.expression);
    }

    @Override
    public String visitFunctionStatement(Statement.Function statement) {
        StringBuilder builder = new StringBuilder();
        builder.append("{fun " + statement.name.lexeme + "(");

        for (Token param : statement.params) {
            if (param != statement.params.get(0)) builder.append(", ");
            builder.append(param.lexeme);
        }

        builder.append(")");

        for (Statement body : statement.body) {
            builder.append("\n\t").append(body.accept(this));
        }

        builder.append("}");
        return builder.toString();
    }

    @Override
    public String visitIfStatement(Statement.If statement) {
        if (statement.elseBranch == null) {
            return parenthesize2("if", statement.condition, statement.thenBranch);
        }
        return parenthesize2("if-else", statement.condition, statement.thenBranch, statement.elseBranch);
    }

    @Override
    public String visitOutputStatement(Statement.Output statement) {
        return parenthesize("output", statement.expression);
    }

    @Override
    public String visitReturnStatement(Statement.Return statement) {
        if (statement.value == null) return "(return)";
        return parenthesize("return", statement.value);
    }

    @Override
    public String visitVarStatement(Statement.Var statement) {
        if (statement.initializer == null) {
            return parenthesize2("var", statement.name);
        }

        return parenthesize2("var", statement.name, "=", statement.initializer);
    }

    @Override
    public String visitWhileStatement(Statement.While statement) {
        return parenthesize2("while", statement.condition, statement.body);
    }
}

class AstPrint {
    public static void main(String[] args) {
        AstPrinter printer = new AstPrinter();

        String code = """
                define x;
                
                var a = 1;
                while (a < 5) {
                    a = a + 1;
                }
                for (var i = 0; i < 5; i = i + 1) { // For loop.
                    a = a + 1;
                }
                
                var result = "";
                if (a <= 10 or a >= 100) {
                    eval("[\\"Add\\", [\\"Power\\", \\"x\\", 2], [\\"Multiply\\", -5, \\"x\\"], \\"a\\"]");
                }
                
                output result;
                """;

        Scanner scanner = new Scanner(code);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Statement> statements = parser.parse();

        for (Statement statement : statements) {
            System.out.println("Statement: " + statement.getClass().getSimpleName());
            String result = printer.print(statement);
            System.out.println(result);
        }
    }
}
