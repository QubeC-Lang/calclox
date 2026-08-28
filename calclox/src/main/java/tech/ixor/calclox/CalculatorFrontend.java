package tech.ixor.calclox;

import java.util.Map;

/**
 * Connects a host application to CalcLox input variables and program output.
 */
public interface CalculatorFrontend {
    /**
     * Returns the mutable variable map shared with a program. A {@code define name;}
     * declaration reads from this map; numeric CalcLox declarations and assignments
     * are written back to it.
     *
     * @return the mutable host variable map, or {@code null} when host variables are unsupported
     */
    Map<String, Double> variables();

    /**
     * Receives the textual value produced by an {@code output} statement.
     *
     * @param result the result to be displayed
     */
    void output(String result);
}
