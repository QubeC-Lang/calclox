package tech.ixor.calclox;

import org.junit.jupiter.api.Test;
import tech.ixor.calclox.exceptions.CalcLoxRunnerError;
import tech.ixor.calclox.token.Token;
import tech.ixor.calclox.token.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScannerTest {
    @Test
    void scansKeywordsLiteralsAndOperators() {
        List<Token> tokens = new Scanner("var answer = 40.5 + 1.5; output answer;").scanTokens();

        assertEquals(List.of(TokenType.VAR, TokenType.IDENTIFIER, TokenType.EQUAL, TokenType.NUMBER,
                TokenType.PLUS, TokenType.NUMBER, TokenType.SEMICOLON, TokenType.OUTPUT,
                TokenType.IDENTIFIER, TokenType.SEMICOLON, TokenType.EOF),
                tokens.stream().map(token -> token.type).toList());
        assertEquals(40.5, tokens.get(3).literal);
    }

    @Test
    void reportsLexicalErrorWithLineNumber() {
        CalcLoxRunnerError error = assertThrows(CalcLoxRunnerError.class,
                () -> new Scanner("\n@").scanTokens());

        assertEquals(2, error.getLine());
        assertEquals("Unexpected character: '@'.", error.getMessage());
    }

    @Test
    void ignoresCommentsAndTracksLines() {
        List<Token> tokens = new Scanner("// ignored\noutput 1;\noutput 2;").scanTokens();

        assertEquals(List.of(2, 2, 2, 3, 3, 3, 3),
                tokens.stream().map(token -> token.line).toList());
        assertEquals(TokenType.EOF, tokens.getLast().type);
    }

    @Test
    void scansStringsWithoutTreatingEscapedQuoteAsTerminator() {
        List<Token> tokens = new Scanner("output \"left\\\"right\";").scanTokens();

        Token string = tokens.get(1);
        assertEquals(TokenType.STRING, string.type);
        assertEquals("left\\\"right", string.literal);
        assertEquals("\"left\\\"right\"", string.lexeme);
    }

    @Test
    void scansEveryReservedWord() {
        List<TokenType> types = new Scanner("""
                define var output nil fun if else while for break continue return
                true false and or identifier_name
                """).scanTokens().stream().map(token -> token.type).toList();

        assertEquals(List.of(TokenType.DEFINE, TokenType.VAR, TokenType.OUTPUT, TokenType.NIL,
                TokenType.FUN, TokenType.IF, TokenType.ELSE, TokenType.WHILE, TokenType.FOR,
                TokenType.BREAK, TokenType.CONTINUE, TokenType.RETURN, TokenType.TRUE,
                TokenType.FALSE, TokenType.AND, TokenType.OR, TokenType.IDENTIFIER, TokenType.EOF), types);
    }

    @Test
    void emptySourceProducesOnlyEof() {
        List<Token> tokens = new Scanner("").scanTokens();

        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.getFirst().type);
        assertNull(tokens.getFirst().literal);
    }

    @Test
    void rejectsUnterminatedStringAtItsFinalLine() {
        CalcLoxRunnerError error = assertThrows(CalcLoxRunnerError.class,
                () -> new Scanner("\"first\nsecond").scanTokens());

        assertEquals(2, error.getLine());
        assertEquals("Unterminated string.", error.getMessage());
    }

    @Test
    void rejectsNullSource() {
        NullPointerException error = assertThrows(NullPointerException.class, () -> new Scanner(null));

        assertEquals("source must not be null", error.getMessage());
    }
}
