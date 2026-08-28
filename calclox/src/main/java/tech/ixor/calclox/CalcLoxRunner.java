package tech.ixor.calclox;

import tech.ixor.calclox.ast.Statement;
import tech.ixor.calclox.exceptions.CalcLoxRunnerError;
import tech.ixor.calclox.token.Token;

import java.util.List;

/**
 * Entry point for executing CalcLox programs in an embedding application.
 * <p>
 * This class performs the full scan, parse, resolve, and interpret pipeline. It does
 * not retain interpreter state between calls; persistent numeric state belongs in the
 * host's {@link CalculatorFrontend#variables()} map.
 * </p>
 */
public final class CalcLoxRunner {
    private CalcLoxRunner() {
    }

    /**
     * Executes source code with a fresh interpreter. Program state is isolated from
     * previous and concurrent calls, while numeric variables are synchronized through
     * the supplied frontend.
     *
     * @param frontend host integration for variables and output
     * @param source the CalcLox source code to be executed
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws CalcLoxRunnerError if scanning, parsing, resolution, or execution fails
     */
    public static void run(CalculatorFrontend frontend, String source) {
        if (frontend == null) throw new IllegalArgumentException("frontend must not be null");
        if (source == null) throw new IllegalArgumentException("source must not be null");

        Interpreter interpreter = new Interpreter(frontend);

        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Statement> statements = parser.parse();

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        interpreter.interpret(statements);
    }

    static void runtimeError(RuntimeError error) {
        CalcLoxRunnerError runnerError = new CalcLoxRunnerError(error.getMessage(), error.token.line);
        throw runnerError;
    }
}
