package tech.ixor.calclox;

import tech.ixor.calclox.token.Token;
import tech.ixor.calclox.token.TokenType;
import tech.ixor.calclox.exceptions.CalcLoxRunnerError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tech.ixor.calclox.token.TokenType.*;

/**
 * Converts CalcLox source text into the token stream consumed by {@link Parser}.
 * <p>
 * The scanner recognizes identifiers, reserved words, decimal numbers, strings,
 * punctuation, and operators. Spaces, tabs, carriage returns, and {@code //} line
 * comments are ignored. Line breaks are retained as token line metadata so later
 * compilation and runtime errors can identify their source location.
 * </p>
 * <p>
 * A scanner instance is single-use. Call {@link #scanTokens()} once for a given source.
 * </p>
 */
public class Scanner {
    //> Keyword Map
    /**
     * A map of keywords to their respective token types.
     */
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<String, TokenType>();
        keywords.put("define", DEFINE);
        keywords.put("var", VAR);
        keywords.put("output", OUTPUT);
        keywords.put("nil", NIL);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("else", ELSE);
        keywords.put("while", WHILE);
        keywords.put("for", FOR);
        keywords.put("break", BREAK);
        keywords.put("continue", CONTINUE);
        keywords.put("return", RETURN);
        keywords.put("true", TRUE);
        keywords.put("false", FALSE);
        keywords.put("and", AND);
        keywords.put("or", OR);
    }
    //< Keyword Map

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    //> Scan Info
    private int start = 0;
    private int current = 0;
    private int line = 1;
    //< Scan Info

    /**
     * Creates a scanner positioned at the beginning of the supplied source.
     *
     * @param source complete CalcLox source text
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public Scanner(String source) {
        this.source = java.util.Objects.requireNonNull(source, "source must not be null");
    }

    //> Scanning and Misc
    /**
     * Scans the complete source and appends a final {@link TokenType#EOF EOF} token.
     * Number literals are represented as {@link Double}; string literal values omit
     * their surrounding quotation marks.
     *
     * @return tokens in source order, including the terminating EOF token
     * @throws CalcLoxRunnerError if the source contains an unknown character or an
     *         unterminated string
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            //>> Single-Character Tokens
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case ',' -> addToken(COMMA);
            case '.' -> addToken(DOT);
            case '-' -> addToken(MINUS);
            case '+' -> addToken(PLUS);
            case ';' -> addToken(SEMICOLON);
            case '*' -> addToken(STAR);
            //<< Single-Character Tokens
            //>> Slash (Single-Character or Comment)
            case '/' -> {
                if (match('/')) {
                    // A Comment
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
            }
            //<< Slash (Single-Character or Comment)
            //>> One or Two-Character Tokens
            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
            //<< One or Two-Character Tokens
            //>> Whitespace and Line Breaks
            case ' ', '\r', '\t' -> {
                // Ignore whitespace.
            }
            case '\n' -> line++;
            //<< Whitespace and Line Breaks
            //>> Start of String
            case '"' -> string();
            //<< Start of String
            //>> Literals and Unknown
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw new CalcLoxRunnerError("Unexpected character: '" + c + "'.", line);
                }
            }
            //<< Literals and Unknown
        }
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }
    //< Scanning and Misc

    //> Is Methods
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }
    //< Is Methods

    //> Number and Identifier
    private void number() {
        while (isDigit(peek())) advance();

        // Looking for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            do {
                advance();
            } while (isDigit(peek()));
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        // If the token is not a keyword, it is an identifier.
        if (type == null) type = IDENTIFIER;

        addToken(type);
    }
    //< Number and Identifier

    //> String Finding
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            if (peek() == '\\' && current + 1 < source.length()) {
                // Keep escape sequences in their source form while ensuring an escaped
                // quote does not terminate the literal.
                advance();
                advance();
            } else {
                advance();
            }
        }

        if (isAtEnd()) {
            throw new CalcLoxRunnerError("Unterminated string.", line);
        }

        // The closing ".
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }
    //< String Finding

    //> Two-Character Tokens Finding
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }
    //< Two-Character Tokens Finding
}
