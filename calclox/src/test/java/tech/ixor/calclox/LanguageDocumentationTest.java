package tech.ixor.calclox;

import org.junit.jupiter.api.Test;
import tech.ixor.calclox.exceptions.CalcLoxRunnerError;
import tech.ixor.calclox.token.Token;
import tech.ixor.calclox.token.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executable examples and conformance checks for docs/language-*.md. */
class LanguageDocumentationTest {
    @Test
    void guideExpressionExamplesAndOperatorPrecedenceAreCorrect() {
        RecordingFrontend frontend = run("""
                output 2 + 3 * 4;
                output (2 + 3) * 4;
                output "calc" + "lox";
                output 5 >= 3;
                output nil == nil;
                output 20 / 5 * 2;
                output 10 - 3 - 2;
                output - -2;
                """);

        assertEquals(List.of("14.0", "20.0", "calclox", "true", "true", "8.0", "5.0", "2.0"),
                frontend.outputs);
    }

    @Test
    void valueTypesEqualityAndOperandRulesMatchTheSpecification() {
        RecordingFrontend frontend = run("""
                output nil != false;
                output 1 == 1;
                output "a" == "a";
                output true == true;
                output 1 < 2;
                """);
        assertEquals(List.of("true", "true", "true", "true", "true"), frontend.outputs);

        assertLanguageError("output 1 + \"1\";", "Operands must be two numbers or two strings.");
        assertLanguageError("output \"a\" < \"b\";", "Operands must be numbers.");
        assertLanguageError("output -true;", "Operand must be a number.");
        assertLanguageError("output 1 / -0;", "Division by zero.");
    }

    @Test
    void truthinessAndLogicalOperatorsShortCircuitAndReturnOperands() {
        RecordingFrontend frontend = run("""
                output !nil;
                output !false;
                output !0;
                output !"";
                output nil or "fallback";
                output true and 42;
                var touched = 0;
                false and (touched = 1);
                true or (touched = 2);
                output touched;
                """);
        assertEquals(List.of("true", "true", "false", "false", "fallback", "42.0", "0.0"),
                frontend.outputs);
    }

    @Test
    void assignmentIsRightAssociativeAndProducesItsValue() {
        RecordingFrontend frontend = run("""
                var a = 0;
                var b = 0;
                output a = b = 7;
                output a;
                output b;
                """);
        assertEquals(List.of("7.0", "7.0", "7.0"), frontend.outputs);
        assertLanguageError("(1 + 2) = 3;", "Invalid assignment target.");
    }

    @Test
    void variablesBlocksShadowingAndClosuresUseLexicalScope() {
        RecordingFrontend frontend = run("""
                var outer = "global";
                { var outer = "local"; output outer; }
                output outer;
                var pending;
                output pending;
                fun makeAdder(amount) {
                  fun add(value) { return value + amount; }
                  return add;
                }
                var addTwo = makeAdder(2);
                output addTwo(3);
                """);
        assertEquals(List.of("local", "global", "null", "5.0"), frontend.outputs);

        assertLanguageError("{ var duplicate = 1; var duplicate = 2; }", "Already a variable named");
        assertLanguageError("{ var self = self; }", "Can't read local variable in its own initializer.");
        assertLanguageError("output unknown;", "Undefined variable 'unknown'.");
    }

    @Test
    void globalRedeclarationReplacesTheBinding() {
        RecordingFrontend frontend = run("var value = 1; var value = 2; output value;");
        assertEquals(List.of("2.0"), frontend.outputs);
    }

    @Test
    void conditionalsWhileAndForExamplesExecuteAsDocumented() {
        RecordingFrontend frontend = run("""
                var score = 50;
                if (score >= 50) output "pass"; else output "retry";
                var n = 3;
                while (n > 0) { output n; n = n - 1; }
                for (var i = 0; i < 3; i = i + 1) output i;
                var once = 0;
                for (; once < 1;) once = once + 1;
                output once;
                """);
        assertEquals(List.of("pass", "3.0", "2.0", "1.0", "0.0", "1.0", "2.0", "1.0"),
                frontend.outputs);
        assertLanguageError("output i;", "Undefined variable 'i'.");
        assertLanguageError("if true output 1;", "Expect '(' after 'if'.");
    }

    @Test
    void omittedForConditionDefaultsToTrueUntilTheBodyReturns() {
        RecordingFrontend frontend = run("""
                fun firstIteration() {
                  for (;;) return 1;
                }
                output firstIteration();
                """);
        assertEquals(List.of("1.0"), frontend.outputs);
    }

    @Test
    void standaloneElseCompatibilityQuirkIsCovered() {
        RecordingFrontend frontend = run("else output \"accepted\";");
        assertEquals(List.of("accepted"), frontend.outputs);
    }

    @Test
    void functionsHaveFixedArityReturnsAndFirstClassValues() {
        RecordingFrontend frontend = run("""
                fun identity(value) { return value; }
                var callable = identity;
                output callable("ok");
                fun implicitNil() {}
                fun explicitNil() { return; }
                output implicitNil();
                output explicitNil();
                """);
        assertEquals(List.of("ok", "null", "null"), frontend.outputs);

        assertLanguageError("fun f(a) {} f();", "Expected 1 arguments but got 0.");
        assertLanguageError("var n = 1; n();", "Only functions can be called in CalcLox.");
        assertLanguageError("return 1;", "Can't return from top-level code.");
    }

    @Test
    void parameterAndArgumentLimitIs255() {
        String parameters = IntStream.range(0, 255).mapToObj(i -> "p" + i)
                .reduce((a, b) -> a + "," + b).orElseThrow();
        String arguments = IntStream.range(0, 255).mapToObj(i -> "0")
                .reduce((a, b) -> a + "," + b).orElseThrow();
        run("fun maximum(" + parameters + ") { return p254; } output maximum(" + arguments + ");");

        assertLanguageError("fun tooMany(" + parameters + ",extra) {}", "more than 255 parameters");
        assertLanguageError("fun none() {} none(" + arguments + ",0);", "more than 255 arguments");
    }

    @Test
    void nativeClockAndEvalBehaveAsDocumented() {
        RecordingFrontend frontend = run("""
                var amount = 10;
                output clock() > 0;
                output eval("amount * 1.5");
                output eval("\\\"a\\\" + \\\"b\\\"");
                """);
        assertEquals(List.of("true", "15.0", "ab"), frontend.outputs);

        CalcLoxRunnerError typeError = assertThrows(CalcLoxRunnerError.class,
                () -> run("eval(1);"));
        assertEquals(-1, typeError.getLine());
        assertLanguageError("eval(\"var x = 1;\");", "Expected expression.");
        assertLanguageError("fun local(x) { return eval(\"x\"); } local(1);", "Undefined variable 'x'.");
    }

    @Test
    void hostImportsAndNumericWriteBackMatchTheSpecification() {
        RecordingFrontend frontend = new RecordingFrontend();
        frontend.variables.put("input", 4.0);
        frontend.variables.put("nullable", null);
        CalcLoxRunner.run(frontend, """
                define input;
                define nullable;
                var calculated = input * 2;
                var noInitializer;
                var text = "not exported";
                input = 5;
                output nullable;
                """);

        assertEquals(List.of("null"), frontend.outputs);
        assertEquals(5.0, frontend.variables.get("input"));
        assertEquals(8.0, frontend.variables.get("calculated"));
        assertFalse(frontend.variables.containsKey("noInitializer"));
        assertFalse(frontend.variables.containsKey("text"));
        assertLanguageError("define missing;", "not found in the calculator frontend");
    }

    @Test
    void completeCompoundInterestGuideExampleRuns() {
        RecordingFrontend frontend = new RecordingFrontend();
        frontend.variables.put("principal", 100.0);
        frontend.variables.put("annualRate", 0.1);
        frontend.variables.put("years", 2.0);
        CalcLoxRunner.run(frontend, """
                define principal;
                define annualRate;
                define years;

                fun compound(balance, rate, periods) {
                  var i = 0;
                  while (i < periods) {
                    balance = balance * (1 + rate);
                    i = i + 1;
                  }
                  return balance;
                }

                var finalBalance = compound(principal, annualRate, years);
                output finalBalance;
                """);
        assertEquals(List.of("121.00000000000003"), frontend.outputs);
        assertEquals(121.00000000000003, frontend.variables.get("finalBalance"));
    }

    @Test
    void numericLocalAssignmentsAlsoWriteTheirIdentifierToTheHostMap() {
        RecordingFrontend frontend = run("{ var local = 1; local = 2; }");
        assertEquals(2.0, frontend.variables.get("local"));
    }

    @Test
    void executionEffectsAreSynchronousAndNotRolledBack() {
        RecordingFrontend frontend = new RecordingFrontend();
        assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(frontend, "var saved = 3; output saved; output 1 / 0;"));
        assertEquals(3.0, frontend.variables.get("saved"));
        assertEquals(List.of("3.0"), frontend.outputs);
    }

    @Test
    void eachRunHasFreshLanguageStateButCanReimportHostState() {
        RecordingFrontend frontend = run("var persisted = 9;");
        CalcLoxRunnerError missing = assertThrows(CalcLoxRunnerError.class,
                () -> CalcLoxRunner.run(frontend, "output persisted;"));
        assertEquals("Undefined variable 'persisted'.", missing.getMessage());

        CalcLoxRunner.run(frontend, "define persisted; output persisted;");
        assertEquals(List.of("9.0"), frontend.outputs);
    }

    @Test
    void documentedUnsupportedSyntaxIsRejected() {
        for (String source : List.of(
                "while (true) break;",
                "while (true) continue;",
                "output object.property;",
                "output 5 % 2;",
                "var x = 1; x += 1;",
                "var x = 1; x++;",
                "var f = fun() {};",
                "/* block comment */ output 1;")) {
            assertThrows(CalcLoxRunnerError.class, () -> run(source), source);
        }
    }

    @Test
    void scannerImplementsDocumentedIdentifierNumberCommentAndStringRules() {
        List<Token> tokens = new Scanner("_name9 ABC 1 1.25 .5 1. // comment\n\"a\\nb\" \"x\ny\"")
                .scanTokens();
        assertEquals(List.of(
                TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.NUMBER, TokenType.NUMBER,
                TokenType.DOT, TokenType.NUMBER, TokenType.NUMBER, TokenType.DOT,
                TokenType.STRING, TokenType.STRING, TokenType.EOF),
                tokens.stream().map(token -> token.type).toList());
        assertEquals("a\\nb", tokens.get(8).literal);
        assertEquals("x\ny", tokens.get(9).literal);
        assertEquals(2, tokens.get(8).line);
        assertEquals(3, tokens.get(9).line);

        assertThrows(CalcLoxRunnerError.class, () -> new Scanner("é").scanTokens());
        assertThrows(CalcLoxRunnerError.class, () -> new Scanner("\"unterminated").scanTokens());
        assertThrows(CalcLoxRunnerError.class, () -> run("output 1e2;"));
    }

    private static RecordingFrontend run(String source) {
        RecordingFrontend frontend = new RecordingFrontend();
        CalcLoxRunner.run(frontend, source);
        return frontend;
    }

    private static void assertLanguageError(String source, String messageFragment) {
        CalcLoxRunnerError error = assertThrows(CalcLoxRunnerError.class, () -> run(source));
        assertTrue(error.getMessage().contains(messageFragment), error::getMessage);
    }

    private static final class RecordingFrontend implements CalculatorFrontend {
        private final Map<String, Double> variables = new HashMap<>();
        private final List<String> outputs = new ArrayList<>();

        @Override public Map<String, Double> variables() { return variables; }
        @Override public void output(String result) { outputs.add(result); }
    }
}
