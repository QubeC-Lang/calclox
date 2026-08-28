package tech.ixor.calclox;

import tech.ixor.calclox.token.Token;

/** Internal execution failure paired with the token that caused it. */
public class RuntimeError extends RuntimeException {
    /** Token used to report the failure's source line. */
    public final Token token;

    /**
     * Creates an internal runtime failure.
     *
     * @param token token responsible for the failure
     * @param message user-facing error description
     */
    public RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
