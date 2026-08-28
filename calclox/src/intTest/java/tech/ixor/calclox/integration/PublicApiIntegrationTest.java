package tech.ixor.calclox.integration;

import org.junit.jupiter.api.Test;
import tech.ixor.calclox.CalcLoxRunner;
import tech.ixor.calclox.CalculatorFrontend;
import tech.ixor.calclox.exceptions.CalcLoxRunnerError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicApiIntegrationTest {
    @Test
    void consumerCanRunAProgramUsingOnlyThePublicApi() {
        ConsumerFrontend frontend = new ConsumerFrontend(Map.of("principal", 100.0, "rate", 0.05));

        CalcLoxRunner.run(frontend, """
                define principal;
                define rate;
                var interest = principal * rate;
                output interest;
                """);

        assertEquals(List.of("5.0"), frontend.outputs);
        assertEquals(5.0, frontend.variables.get("interest"));
    }

    @Test
    void hostCanPersistVariablesAcrossIsolatedExecutions() {
        ConsumerFrontend frontend = new ConsumerFrontend(Map.of("balance", 100.0));

        CalcLoxRunner.run(frontend, "define balance; balance = balance + 25; output balance;");
        CalcLoxRunner.run(frontend, "define balance; balance = balance * 2; output balance;");

        assertEquals(List.of("125.0", "250.0"), frontend.outputs);
        assertEquals(250.0, frontend.variables.get("balance"));
    }

    @Test
    void publicErrorExposesRuntimeMessageAndSourceLine() {
        ConsumerFrontend frontend = new ConsumerFrontend(Map.of());

        CalcLoxRunnerError error = assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(frontend, "\n\noutput unknown;"));

        assertEquals("Undefined variable 'unknown'.", error.getMessage());
        assertEquals(3, error.getLine());
    }

    private static final class ConsumerFrontend implements CalculatorFrontend {
        private final Map<String, Double> variables;
        private final List<String> outputs = new ArrayList<>();

        private ConsumerFrontend(Map<String, Double> variables) {
            this.variables = new HashMap<>(variables);
        }

        @Override public Map<String, Double> variables() { return variables; }
        @Override public void output(String result) { outputs.add(result); }
    }
}
