package tech.ixor.calclox;

import tech.ixor.calclox.ast.Expr;
import tech.ixor.calclox.ast.Statement;
import tech.ixor.calclox.token.Token;
import tech.ixor.calclox.token.TokenType;
import tech.ixor.calclox.exceptions.CalcLoxRunnerError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static tech.ixor.calclox.token.TokenType.*;

/**
 * Builds CalcLox statements and expressions from a token stream.
 * <p>
 * Expressions are parsed according to CalcLox precedence rules, from assignment
 * through logical, equality, comparison, arithmetic, unary, and call expressions.
 * Loop syntax is lowered to block and while statements before interpretation.
 * </p>
 */
public class Parser {
    /**
     * The error thrown when the parser encounters an issue.
     */
    private static class ParseError extends CalcLoxRunnerError {
        public ParseError(Token token, String message) {
            super(message, token.line);
        }
    }

    private ParseError error(Token token, String message) {
        return new ParseError(token, message);
    }

    private final List<Token> tokens;
    private int current = 0;

    /**
     * Creates a parser for a scanner-produced token stream.
     *
     * @param tokens tokens ending in {@link TokenType#EOF}
     * @throws NullPointerException if {@code tokens} is {@code null}
     */
    public Parser(List<Token> tokens) {
        this.tokens = java.util.Objects.requireNonNull(tokens, "tokens must not be null");
    }

    /**
     * Parses all declarations and statements up to the EOF token.
     *
     * @return statements in execution order
     * @throws CalcLoxRunnerError if the token stream does not form a valid program
     */
    public List<Statement> parse() {
        List<Statement> statements = new ArrayList<>();

        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    Expr parseExpression() {
        Expr result = expression();
        if (!isAtEnd()) throw error(peek(), "Expect end of expression.");
        return result;
    }

    private Expr expression() {
        return assignment();
    }

    //> Token Getters
    /**
     * Get the previous token.
     * @return the previous token
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Get the next token without consuming it.
     * @return the next token
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Get the next token and consume it.
     * @return the next token
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    //< Token Getters

    //> End Checker
    /**
     * Check if the current token is the last token of the input.
     * @return true if the current token is the last token, false otherwise
     */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }
    //< End Checker

    //> Token Checkers
    /**
     * Check if the current token matches the expected type.
     * @param type the expected token type
     * @return true if the current token matches, false otherwise
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }
    //< Token Checkers

    //> Token Matching and Consuming
    /**
     * Check if the current token matches any of the expected types.
     * @param types the expected token types
     * @return true if the current token matches any of the expected types, false otherwise
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Consume the token that should be paired with another token of desired type.
     * @param type    the expected token type
     * @param message the error message to be thrown if the token type does not match
     * @return the consumed token
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }
    //< Token Matching and Consuming

    //> Statement Parsing
    private Statement statement() {
        if (match(OUTPUT)) return outputStatement();
        if (match(RETURN)) return returnStatement();
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(FOR)) return forStatement();
        if (match(LEFT_BRACE)) return new Statement.Block(block());
        if (match(ELSE)) {
            // If we encounter an else without a preceding if, create a default if statement
            // with a false condition so that the else branch is executed
            Expr condition = new Expr.Literal(false); // Always false condition
            Statement thenBranch = new Statement.Expression(new Expr.Literal(null)); // Empty then branch
            Statement elseBranch = statement(); // Parse the else branch
            return new Statement.If(condition, thenBranch, elseBranch);
        }
        return expressionStatement();
    }

    private Statement outputStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after the value to output.");
        return new Statement.Output(value);
    }

    private Statement returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
          value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Statement.Return(keyword, value);
    }

    //>> Control Flow Statements
    private Statement ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition."); // [parens]

        Statement thenBranch = statement();
        Statement elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Statement.If(condition, thenBranch, elseBranch);
    }

    private Statement whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Statement body = statement();

        return new Statement.While(condition, body);
    }

    private Statement forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        // The for-loop initialization part.
        Statement initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        // The stopping condition of the for-loop.
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        // The clause that is executed after each iteration of the for-loop.
        Expr clause = null;
        if (!check(RIGHT_PAREN)) {
            clause = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        // The body of the for-loop.
        Statement body = statement();
        if (clause != null) {
          body = new Statement.Block(
                  Arrays.asList(body, new Statement.Expression(clause))
          );
        }
        if (condition == null) condition = new Expr.Literal(true);
        body = new Statement.While(condition, body);
        if (initializer != null) {
            body = new Statement.Block(Arrays.asList(initializer, body));
        }

        return body;
    }
    //<< Control Flow Statements

    private Statement expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Statement.Expression(expr);
    }
    //< Statement Parsing

    //> Declaration Parsing
    private Statement declaration() {
        if (match(DEFINE)) return defineDeclaration();
        if (match(FUN)) return function("function");
        if (match(VAR)) return varDeclaration();
        return statement();
    }

    /**
     * Process variable declaration statements.
     */
    private Statement varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Statement.Var(name, initializer);
    }

    /**
     * Process user-defined value declaration statements.
     */
    private Statement defineDeclaration() {
        Token name = consume(IDENTIFIER, "Expect user-defined value name.");
        consume(SEMICOLON, "Expect ';' after user-defined value declaration.");
        return new Statement.Define(name);
    }
    //< Declaration Parsing

    //> Function Parsing
    private Statement.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    throw error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Statement> body = block();
        return new Statement.Function(name, parameters, body);
    }
    //< Function Parsing

    //> Statement and State Block
    /**
     * Process block statements (codes between '{' and '}').
     */
    private List<Statement> block() {
        List<Statement> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
          statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }
    //< Statement and State Block

    //> Unary and Binary Expressions
    /**
     * Process a unary (!, -) expression.
     */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /**
     * Process a factor (/, *) expression.
     */
    private Expr factor() {
        // The left-hand side of the expression should be a unary expression (a single value).
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            // The right-hand side of the expression should be a unary expression.
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Process term (+, -) expressions.
     */
    private Expr term() {
        // Since factor (/, *) operations have higher precedence than term (+, -) operations,
        // we need to process the factor first.
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Process non-equality comparison (>, >=, <, <=) expressions.
     */
    private Expr comparison() {
        // Since any calculation has a higher precedence than comparison,
        // we need to do all the calculations first.
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Process equality (==, !=) expressions.
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    //< Unary and Binary Expressions

    //> Control Flow
    /**
     * Process logical 'and' expressions.
     */
    private Expr and() {
        // Boolean value computations should be done first before logical expressions
        // being performed.
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * Process logical 'or' expressions.
     */
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }
    //< Control Flow

    //> Assignment

    /**
     * Process variable assignment expressions.
     */
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            throw error(equals, "Invalid assignment target.");
        }

        return expr;
    }
    //< Assignment

    //> Method Call and Call-Finishing
    /**
     * Process a method call.
     */
    private Expr call() {
        Expr expr = primary();

        while (true) {
            // It is expected to have a '(' after the method name.
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr expr) {
        List<Expr> arguments = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    throw error(peek(), "Cannot have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments.");
        return new Expr.Call(expr, paren, arguments);
    }
    //< Method Call and Call-Finishing

    //> Synchronization
    /**
     * Synchronize to the next token that matches the expected rule to recover from an error.
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case DEFINE:
                case VAR:
                case OUTPUT:
                case FUN:
                case IF:
                case WHILE:
                case FOR:
                case RETURN:
                    return;
            }

            advance();
        }
    }
    //< Synchronization

    //> Primary
    private Expr primary() {
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expected expression.");
    }
    //< Primary
}
