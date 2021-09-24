package moon.mlock.common.exception;

/**
 * 分布式幂等异常类
 *
 * @author moon
 */
public class IdempotentException extends RuntimeException {
    public IdempotentException() {

    }

    public IdempotentException(String message) {
        super(message);
    }

    public IdempotentException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdempotentException(Throwable cause) {
        super(cause);
    }
}
