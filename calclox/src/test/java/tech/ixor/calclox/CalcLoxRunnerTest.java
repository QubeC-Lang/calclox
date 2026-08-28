package tech.ixor.calclox;

import org.junit.jupiter.api.Test;
import tech.ixor.calclox.exceptions.CalcLoxRunnerError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalcLoxRunnerTest {
    @Test
    void executesArithmeticControlFlowAndFunctions() {
        RecordingFrontend frontend = new RecordingFrontend();

        CalcLoxRunner.run(frontend, """
                fun twice(value) { return value * 2; }
                var total = 0;
                for (var i = 1; i < 4; i = i + 1) { total = total + twice(i); }
                output total;
                output eval("total + 1");
                """);

        assertEquals(List.of("12.0", "13.0"), frontend.outputs);
        assertEquals(12.0, frontend.variables.get("total"));
    }

    @Test
    void readsAndUpdatesFrontendVariables() {
        RecordingFrontend frontend = new RecordingFrontend();
        frontend.variables.put("price", 10.0);

        CalcLoxRunner.run(frontend, "define price; price = price * 1.5; output price;");

        assertEquals(List.of("15.0"), frontend.outputs);
        assertEquals(15.0, frontend.variables.get("price"));
    }

    @Test
    void reportsRuntimeFailuresWithLocation() {
        CalcLoxRunnerError error = assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(new RecordingFrontend(), "output 1 / 0;"));

        assertEquals(1, error.getLine());
        assertEquals("Division by zero.", error.getMessage());
    }

    @Test
    void doesNotLeakStateBetweenRuns() {
        RecordingFrontend frontend = new RecordingFrontend();
        CalcLoxRunner.run(frontend, "var secret = 42;");

        assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(frontend, "output secret;"));
    }

    @Test
    void evaluatesClosuresStringsBooleansAndNil() {
        RecordingFrontend frontend = new RecordingFrontend();

        CalcLoxRunner.run(frontend, """
                fun makeAdder(amount) {
                  fun add(value) { return value + amount; }
                  return add;
                }
                var addTwo = makeAdder(2);
                output addTwo(3);
                output "calc" + "lox";
                output true and !false;
                output nil;
                """);

        assertEquals(List.of("5.0", "calclox", "true", "null"), frontend.outputs);
    }

    @Test
    void reportsSyntaxAndResolutionFailures() {
        CalcLoxRunnerError syntaxError = assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(new RecordingFrontend(), "\noutput 1"));
        CalcLoxRunnerError resolutionError = assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(new RecordingFrontend(), "\nreturn 1;"));

        assertEquals(2, syntaxError.getLine());
        assertEquals("Expect ';' after the value to output.", syntaxError.getMessage());
        assertEquals(2, resolutionError.getLine());
        assertEquals("Can't return from top-level code.", resolutionError.getMessage());
    }

    @Test
    void reportsMissingHostVariable() {
        CalcLoxRunnerError error = assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(new RecordingFrontend(), "define missing;"));

        assertEquals("User-defined variable 'missing' not found in the calculator frontend.",
                error.getMessage());
    }

    @Test
    void validatesPublicArguments() {
        RecordingFrontend frontend = new RecordingFrontend();

        assertEquals("frontend must not be null", assertThrows(IllegalArgumentException.class,
                () -> CalcLoxRunner.run(null, "")).getMessage());
        assertEquals("source must not be null", assertThrows(IllegalArgumentException.class,
                () -> CalcLoxRunner.run(frontend, null)).getMessage());
    }

    @Test
    void rejectsInvalidCallArityAndEvalType() {
        CalcLoxRunnerError arityError = assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(new RecordingFrontend(), "fun f(a) { return a; } output f();"));
        CalcLoxRunnerError evalError = assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(new RecordingFrontend(), "output eval(1);"));

        assertEquals("Expected 1 arguments but got 0.", arityError.getMessage());
        assertEquals("eval() expects a string argument.", evalError.getMessage());
    }

    private static final class RecordingFrontend implements CalculatorFrontend {
        private final Map<String, Double> variables = new HashMap<>();
        private final List<String> outputs = new ArrayList<>();

        @Override public Map<String, Double> variables() { return variables; }
        @Override public void output(String result) { outputs.add(result); }
    }
}
