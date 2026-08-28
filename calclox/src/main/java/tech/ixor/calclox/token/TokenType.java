package tech.ixor.calclox.token;

/**
 * Lexical categories recognized by the CalcLox scanner.
 */
public enum TokenType {
    // Keywords
    /**
     * The keyword "define", used to define a user input value (that asks the user to input when running the program).
     */
    DEFINE,
    /**
     * The keyword "var", used to declare a in-program variable that is not modifiable by the user directly.
     */
    VAR,
    /**
     * The keyword {@code output}, used to send a value to the host frontend.
     */
    OUTPUT,
    /**
     * A "nil" value, which is used to represent a null value. Reserved for future use.
     */
    NIL,
    /**
     * The keyword "fun", used to define a function.
     */
    FUN,
    // The following keywords should be pretty self-explanatory.
    // Logical "and" and "or" are in keyword form ("and" and "or").
    IF, ELSE, WHILE, FOR, BREAK, CONTINUE, RETURN, TRUE, FALSE, AND, OR,

    // Literals.
    /**
     * An identifier, which is a (variable or function) name.
     */
    IDENTIFIER,
    // The following literals should be pretty self-explanatory.
    STRING, NUMBER,

    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // End Of File
    EOF
}
