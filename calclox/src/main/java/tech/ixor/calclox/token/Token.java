package tech.ixor.calclox.token;

/**
 * One lexical unit of CalcLox source.
 * <p>
 * Tokens retain the original lexeme and one-based line number. Literal tokens also
 * carry their decoded runtime value: a {@link Double} for numbers, a {@link String}
 * for strings, and {@code null} for tokens without a literal value.
 * </p>
 */
public class Token {
    /** Syntactic category recognized by the scanner. */
    public final TokenType type;
    /** Exact source text occupied by this token. */
    public final String lexeme;
    /** Parsed literal value, or {@code null} when the token has none. */
    public final Object literal;
    /** One-based source line on which the token ends. */
    public final int line;

    /**
     * Creates a token.
     *
     * @param type syntactic category
     * @param lexeme original source text
     * @param literal parsed literal value
     * @param line one-based source line
     */
    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    /**
     * Formats the token as its type, lexeme, and literal value for diagnostics.
     *
     * @return diagnostic token representation
     */
    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
