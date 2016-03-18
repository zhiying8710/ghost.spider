package me.binge.ghost.spiders.exception;

public class NotImplementationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotImplementationException() {
        super();
    }

    public NotImplementationException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NotImplementationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotImplementationException(String message) {
        super(message);
    }

    public NotImplementationException(Throwable cause) {
        super(cause);
    }

}
