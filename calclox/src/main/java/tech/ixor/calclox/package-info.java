/**
 * Embeds the CalcLox language in a host application.
 * <p>
 * Use {@link tech.ixor.calclox.CalcLoxRunner} to execute source code and implement
 * {@link tech.ixor.calclox.CalculatorFrontend} to provide numeric variables and receive
 * values produced by {@code output} statements. Each execution uses an isolated interpreter;
 * numeric declarations and assignments are synchronized with the frontend's mutable variable map.
 * </p>
 * <p>
 * Invalid source and runtime failures are reported as
 * {@link tech.ixor.calclox.exceptions.CalcLoxRunnerError} instances with source-line information.
 * </p>
 *
 * @see tech.ixor.calclox.CalcLoxRunner
 * @see tech.ixor.calclox.CalculatorFrontend
 * @see tech.ixor.calclox.exceptions.CalcLoxRunnerError
 */
package tech.ixor.calclox;
