package ro.unibuc.hello.exception;

public class AccessViolationException extends RuntimeException {
    public AccessViolationException(String message) {
        super(message);
    }
}
