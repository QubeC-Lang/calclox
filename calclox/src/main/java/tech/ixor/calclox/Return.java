package tech.ixor.calclox;

/** Internal, stackless control-flow signal used to return from a function body. */
public class Return extends RuntimeException {
    /** Value returned by the function, or {@code null} for an empty return. */
    public final Object value;

    /**
     * Creates a return signal.
     *
     * @param value value carried to the function call boundary
     */
    public Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
