package jooq;

public class NoRollbackException extends RuntimeException {

    public NoRollbackException(Throwable cause) {
        super(cause);
    }

}
