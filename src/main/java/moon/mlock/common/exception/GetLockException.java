package moon.mlock.common.exception;

/**
 * 获取锁异常.
 *
 * @author moon
 */
public class GetLockException extends Exception {

    public GetLockException() {
        super();
    }

    public GetLockException(String message) {
        super(message);
    }

    public GetLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public GetLockException(Throwable cause) {
        super(cause);
    }

}