package by.nhorushko.crudgeneric.flex.exception;

/**
 * Client-side error in a page request: unknown filter field, disallowed
 * filter operation, a value that fails conversion, or a malformed sort
 * expression. Applications should map it to HTTP 400.
 */
public class FilterValidationException extends RuntimeException {

    public FilterValidationException(String message) {
        super(message);
    }

    public FilterValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
