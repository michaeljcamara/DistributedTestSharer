package tlb.splitter.correctness;

/**
 * @understands result of performing correctness check with the server
 */
public class ValidationResult {

    public static enum Status {
        OK(true), FIRST(true), FAILED(false);

        private final boolean valid;

        Status(boolean valid) {
            this.valid = valid;
        }

        public boolean isValid() {
            return valid;
        }
    }

    private final Status status;
    private final String message;

    public ValidationResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public ValidationResult(Status status) {
        this(status, "no-message");
    }

    public boolean hasFailed() {
        return ! status.isValid();
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                '}';
    }
}
