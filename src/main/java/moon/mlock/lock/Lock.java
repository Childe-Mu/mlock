package moon.mlock.lock;

import java.util.concurrent.TimeUnit;

/**
 * 锁操作
 *
 * @author moon
 */
public interface Lock extends AutoCloseable {
    /**
     * 尝试加锁
     *
     * @param time 超时时间
     * @param unit 超时时间单位
     * @return 加锁结果， true=成功 false=失败
     * @throws InterruptedException 中断异常
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 解锁
     */
    void unlock();

    /**
     * 检查锁，而不进行加锁操作，既无需解锁
     *
     * @return 检查锁结果，true=成功，也就是锁没有被其他占有，false=失败
     */
    boolean checkLock();
}
