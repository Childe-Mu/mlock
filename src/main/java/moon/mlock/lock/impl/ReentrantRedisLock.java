package moon.mlock.lock.impl;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import moon.mlock.common.consts.StringConst;
import moon.mlock.lock.ILock;
import moon.mlock.proxy.RedisLockProxy;
import moon.mlock.task.RedisLockKeyRenewTask;
import moon.mlock.utils.SpringUtils;
import moon.mlock.utils.ThreadUtils;
import moon.mlock.utils.UUIDUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可重入Redis分布式锁
 *
 * @author moon
 */
@Slf4j
public class ReentrantRedisLock implements ILock {

    private static final Map<String, LockHolder> REDIS_LOCK_HOLDERS_MAP = Maps.newConcurrentMap();

    /**
     * redis lock 代理
     */
    private final RedisLockProxy proxy;

    /**
     * 业务领域
     */
    private final String domain;

    /**
     * redis锁的key
     */
    private final String key;

    /**
     * 加锁结果
     */
    private Boolean result;

    /**
     * 锁持有的key，格式：线程名称_锁key
     */
    private final String holderKey;

    /**
     * 锁id，使用UUID
     */
    private final String id;

    public ReentrantRedisLock(String domain, String key) {
        this.proxy = SpringUtils.getBean(RedisLockProxy.class);
        this.domain = domain;
        this.key = key;
        this.result = false;
        this.id = UUIDUtils.getUuid();
        this.holderKey = ThreadUtils.getThreadName() + StringConst.UNDERLINE + this.key;
    }

    /**
     * 尝试加锁
     *
     * @param time 超时时间
     * @param unit 超时时间单位
     * @return 加锁结果， true=成功 false=失败
     * @throws InterruptedException 中断异常
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            result = tryLockSelf(time, unit);
            log.info("ReentrantRedisLock tryLock result={}, id={}, domain={}, key={}", result, id, domain, key);
            return result;
        } catch (InterruptedException e) {
            log.error("ReentrantRedisLock tryLock ex:", e);
            throw e;
        }
    }

    /**
     * 解锁
     */
    @Override
    public void unlock() {
        try {
            if (!result) {
                return;
            }
            LockHolder holder = REDIS_LOCK_HOLDERS_MAP.get(holderKey);
            if (Objects.isNull(holder)) {
                throw new IllegalMonitorStateException("没有持有锁：" + key);
            }
            int newCount = holder.count.decrementAndGet();
            if (newCount == 0) {
                REDIS_LOCK_HOLDERS_MAP.remove(holderKey);
                RedisLockKeyRenewTask.removeLockKey(key);
                proxy.unlock(key, holder.value);
                log.info("domain={},key={},id={},unlock success", domain, key, id);
            } else if (newCount < 0) {
                throw new IllegalMonitorStateException("锁计数器为负: " + key);
            }
        } catch (Exception e) {
            log.error("domain={},key={},id={},unlock ex:", domain, key, id, e);
        }
    }

    /**
     * 检查锁，而不进行加锁操作，既无需解锁
     *
     * @return 检查锁结果，true=成功，也就是锁没有被其他占有，false=失败
     */
    @Override
    public boolean checkLock() {
        try {
            boolean check = proxy.checkRedisLock(key);
            log.info("checkLock: domain={},key={},id={},checkLockResult={}", domain, key, id, check);
            return check;
        } catch (Exception e) {
            log.error("checkLock Exception", e);
            return false;
        }
    }

    /**
     * 关闭资源，在退出try -with-resources 块时自动调用
     */
    @Override
    public void close() {
        this.unlock();
    }

    /**
     * 尝试加锁
     *
     * @param time 等待锁最长时间
     * @param unit 等待锁最长时间单位
     * @return true：加锁成功  false：加锁失败
     * @throws InterruptedException
     */
    private boolean tryLockSelf(long time, TimeUnit unit) throws InterruptedException {
        // 如果当前线程已被中断，直接抛出InterruptedException异常，中断加锁流程
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        boolean isReentrancy = isReentrancy();
        log.info("是否重入={}, key={}, holderKey={}", isReentrancy, key, holderKey);
        if (isReentrancy) {
            return true;
        }
        // lockValue = key_系统纳秒数_本地机器ip
        String lockValue = proxy.tryRedisLock(key, time, unit);
        if (Objects.nonNull(lockValue)) {
            LockHolder lockHolder = new LockHolder(lockValue);
            REDIS_LOCK_HOLDERS_MAP.put(holderKey, lockHolder);
            RedisLockKeyRenewTask.putLockKey(key, lockHolder.value);
            return true;
        }
        return false;
    }

    /**
     * 检查是否重入
     *
     * @return true：重入  false:非重入
     */
    private boolean isReentrancy() {
        LockHolder lockHolder = REDIS_LOCK_HOLDERS_MAP.get(holderKey);
        // 缓存中有holderKey对应的lockHolder，说明该线程的锁已经存在了，给锁计数器+1
        if (Objects.nonNull(lockHolder)) {
            lockHolder.count.incrementAndGet();
            return true;
        }
        return false;
    }

    private static class LockHolder {
        /**
         * value
         */
        public final String value;

        /**
         * 可重入锁计数器
         */
        public final AtomicInteger count;

        private LockHolder(String value) {
            this.count = new AtomicInteger(1);
            this.value = value;
        }
    }
}
