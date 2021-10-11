package moon.mlock.template;

import moon.mlock.common.exception.LockException;

/**
 * Lock 回调
 *
 * @author moon
 */
public interface ILockCallback<T> {
    /**
     * 加锁成功
     *
     * @return 结果
     * @throws LockException 异常
     */
    T success() throws LockException;

    /**
     * 加锁失败
     *
     * @return 失败信息
     * @throws LockException 异常
     */
    T fail() throws LockException;

    /**
     * 加锁异常
     *
     * @param e 异常信息
     * @return 异常信息
     * @throws LockException 异常
     */
    T ex(Exception e) throws LockException;
}
