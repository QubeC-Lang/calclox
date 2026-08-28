package tech.ixor.calclox;

import tech.ixor.calclox.ast.Statement;

import java.util.List;

/**
 * Runtime representation of a user-defined CalcLox function.
 * <p>
 * A function retains the environment in which it was declared, allowing its body
 * to access captured variables when invoked later.
 * </p>
 */
public class LoxFunction implements LoxCallable {
    /** Parsed declaration containing the function name, parameters, and body. */
    protected final Statement.Function declaration;
    /**
     * The variable scope of the function.
     */
    protected final Environment closure;

    /**
     * Creates a function closure.
     *
     * @param declaration parsed function declaration
     * @param closure declaration-time lexical environment
     */
    public LoxFunction(Statement.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public String toString() {
        return "<fun " + declaration.name.lexeme + ">";
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }

        return null;
    }
}
