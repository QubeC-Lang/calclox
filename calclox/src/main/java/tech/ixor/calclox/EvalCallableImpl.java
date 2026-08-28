package tech.ixor.calclox;

import java.util.List;

import tech.ixor.calclox.ast.Expr;
import tech.ixor.calclox.token.Token;
import tech.ixor.calclox.token.TokenType;

final class EvalCallableImpl implements LoxCallable {

    @Override
    public String toString() {
        return "<native fun eval>";
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (!(arguments.getFirst() instanceof String expression)) {
            throw new RuntimeError(new Token(TokenType.IDENTIFIER, "eval", null, -1),
                    "eval() expects a string argument.");
        }
        List<Token> tokens = new Scanner(expression.replace("\\\"", "\"")).scanTokens();
        Expr parsedExpression = new Parser(tokens).parseExpression();
        new Resolver(interpreter).resolve(parsedExpression);
        return interpreter.evaluate(parsedExpression);
    }
}
