package moon.lock.common.exception;

/**
 * 锁异常类
 *
 * @author moon
 */
public class MLockException extends RuntimeException {

    public MLockException() {
    }

    public MLockException(String message) {
        super(message);
    }

    public MLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public MLockException(Throwable cause) {
        super(cause);
    }
}
