package me.binge.ghost.spiders.exception;

public class SpiderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SpiderException() {
        super();
    }

    public SpiderException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SpiderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpiderException(String message) {
        super(message);
    }

    public SpiderException(Throwable cause) {
        super(cause);
    }

}
