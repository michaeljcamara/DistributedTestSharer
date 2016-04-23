package tlb.splitter.correctness;

/**
 * @understands correctness check violation notification
 */
public class IncorrectBalancingException extends IllegalStateException {
    public IncorrectBalancingException(String s) {
        super(s);
    }

    public IncorrectBalancingException(Throwable cause) {
        super(cause);
    }

    public IncorrectBalancingException(String message, Throwable cause) {
        super(message, cause);
    }
}
