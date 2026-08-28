package tech.ixor.calclox;

import tech.ixor.calclox.token.Token;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores variables for one lexical scope in a CalcLox execution.
 * <p>
 * Environments form an enclosing chain. Normal lookup and assignment walk that
 * chain, while resolved lookup uses a precomputed lexical distance.
 * </p>
 */
public class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Returns the values declared directly in this scope.
     *
     * @return the live, mutable scope map
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /** Creates a global environment with no enclosing scope. */
    public Environment() {
        this.enclosing = null;
    }

    /**
     * Creates a nested lexical environment.
     *
     * @param enclosing immediately enclosing scope
     */
    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * Looks up a variable in this scope or an enclosing scope.
     *
     * @param name variable token used for lookup and error location
     * @return stored value, including {@code null}
     * @throws RuntimeError if the variable is undefined
     */
    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /**
     * Updates the nearest existing declaration in the environment chain.
     *
     * @param name variable token used for lookup and error location
     * @param value replacement value
     * @throws RuntimeError if the variable is undefined
     */
    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /**
     * Declares or replaces a value directly in this scope.
     *
     * @param name variable name
     * @param value initial value
     */
    public void define(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Returns an enclosing environment at an exact lexical distance.
     *
     * @param distance number of enclosing links to traverse; zero returns this environment
     * @return resolved ancestor
     */
    public Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }

    /**
     * Returns a resolved local value without performing a name search.
     *
     * @param distance lexical distance to the declaring environment
     * @param name variable name
     * @return stored value, including {@code null}
     */
    public Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    /**
     * Updates a resolved local value without performing a name search.
     *
     * @param distance lexical distance to the declaring environment
     * @param name variable token
     * @param value replacement value
     */
    public void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        String result = values.toString();
        if (enclosing != null) {
            result += " -> " + enclosing;
        }
        return "<scope" + result + ">";
    }
}
