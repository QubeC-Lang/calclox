package tech.ixor.calclox.exceptions;

/**
 * Reports a CalcLox scanning, parsing, resolution, or runtime failure to the host.
 * <p>
 * The message is suitable for displaying to a user. {@link #getLine()} identifies
 * the relevant one-based source line when one is available.
 * </p>
 */
public class CalcLoxRunnerError extends RuntimeException {
    private final int line;

    /**
     * Creates an execution error.
     *
     * @param message description of the failure
     * @param line one-based source line, or {@code -1} when no source location is available
     */
    public CalcLoxRunnerError(String message, int line) {
        super(message);
        this.line = line;
    }

    /**
     * Returns the source line associated with the failure.
     *
     * @return the one-based source line, or {@code -1} when unavailable
     */
    public int getLine() {
        return line;
    }
}
