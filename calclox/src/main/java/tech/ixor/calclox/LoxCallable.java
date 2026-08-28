package tech.ixor.calclox;

import java.util.List;

/** Represents a native or user-defined value that CalcLox can invoke. */
public interface LoxCallable {
    /**
     * Returns the required argument count.
     *
     * @return the exact number of arguments accepted by this callable
     */
    int arity();

    /**
     * Invokes the callable with already evaluated arguments.
     *
     * @param interpreter active interpreter
     * @param arguments argument values in source order
     * @return the call result, or {@code null} for CalcLox {@code nil}
     */
    Object call(Interpreter interpreter, List<Object> arguments);
}
