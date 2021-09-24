package moon.mlock.lock.impl;

import moon.mlock.lock.ILock;

import java.util.concurrent.TimeUnit;

/**
 * 无锁
 *
 * @author moon
 */
public class NoLock implements ILock {

    /**
     * 尝试加锁，
     * 因为无锁，所以直接返回true，表示加锁成功
     *
     * @param time 超时时间
     * @param unit 超时时间单位
     * @return 加锁结果， true=成功 false=失败
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        return true;
    }

    /**
     * 解锁
     */
    @Override
    public void unlock() {
        // 因为无锁，所以解锁时，这里什么都不做
    }

    /**
     * 检查锁，而不进行加锁操作，既无需解锁
     * 因为无锁，所以直接返回true，以便进行后续操作
     *
     * @return 检查锁结果，true=成功，也就是锁没有被其他占有，false=失败
     */
    @Override
    public boolean checkLock() {
        return true;
    }

    /**
     * 关闭资源
     */
    @Override
    public void close() {
        // 因为无锁，所以关闭资源时，这里什么都不做
    }
}
