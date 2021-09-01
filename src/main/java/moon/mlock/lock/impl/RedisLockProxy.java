package moon.mlock.lock.impl;

import lombok.extern.slf4j.Slf4j;
import moon.mlock.common.consts.StringConst;
import moon.mlock.config.MLockProperties;
import moon.mlock.utils.LocalUtils;
import moon.mlock.utils.SpringContextUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * redis锁代理
 *
 * @author moon
 */
@Slf4j
@Component
public class RedisLockProxy {

    /**
     * redisTemplate实例
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 获取锁默认等待时间，单位毫秒
     */
    private static final long DEFAULT_WAIT_MILLIS = 1000L;

    /**
     * redis锁-重试等待时间，单位毫秒
     */
    private static final int REDIS_LOCK_RETRY_AWAIT_MILLIS = 200;

    /**
     * redis锁-lockKey过期时间，单位毫秒
     */
    private static final long REDIS_LOCK_KEY_EXPIRE_MILLIS = 60000L;

    public RedisLockProxy(MLockProperties mLockProperties) {
        // 具体如何取redisTemplate实例，需要根据项目具体设置，通常是 集群名称+RedisTemplate，如ShopRedisTemplate
        redisTemplate = SpringContextUtils.getBean(mLockProperties.getRedisGroupName() + "RedisTemplate");
        // 初始化redis连接并测试
        String key = "MLock-connect-test-" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(key, key);
        String val = redisTemplate.opsForValue().get(key);
        log.info("MLock connect test: set={}, get={}", key, val);
        redisTemplate.delete(key);
        log.info("Successfully initialized MLock redis connection");
    }

    /**
     * 尝试加redis锁
     *
     * @param key  锁Key
     * @param time 等待锁最长时间
     * @param unit 等待锁最长时间单位
     * @return 加锁成功返回redis k-v中的value值（key_系统纳秒数_本地机器ip）；加锁失败返回null
     */
    public String tryRedisLock(String key, long time, TimeUnit unit) {
        final long start = System.currentTimeMillis();
        long wait = Optional.ofNullable(unit).map(p -> p.toMillis(time)).orElse(DEFAULT_WAIT_MILLIS);
        String value = null;
        int i = 0;
        while (Objects.isNull(value)) {
            log.debug("第{}次，开始获取锁，lockKey={}", i, key);
            long curTime = System.currentTimeMillis();
            value = createRedisLock(key);
            if (Objects.nonNull(value)) {
                log.debug("第{}次，获取锁成功，耗时：{}ms", i, (System.currentTimeMillis() - curTime));
            }
            if ((System.currentTimeMillis() - start + REDIS_LOCK_RETRY_AWAIT_MILLIS) > wait) {
                log.debug("第{}次，获取锁失败，超时退出", i);
                break;
            }
            log.debug("第{}次，获取锁失败，休眠【{}】ms，再次尝试获取锁", i, REDIS_LOCK_RETRY_AWAIT_MILLIS);
            // todo 这里调用parkNanos，之前考虑的不清楚，有时间查一下，应该可以优化，不能是固定时间
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(REDIS_LOCK_RETRY_AWAIT_MILLIS));
            i++;
        }
        return value;
    }

    /**
     * 创建redis锁
     *
     * @param key     锁Key
     * @param timeout 超时时间
     * @param unit    超时时间单位
     * @return 成功返回锁Value，失败返回null
     */
    private String createRedisLock(String key, long timeout, TimeUnit unit) {
        String value = key + StringConst.UNDERLINE + System.nanoTime() + StringConst.UNDERLINE + LocalUtils.getLocalIp();
        boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
        return result ? value : null;
    }


    /**
     * 创建redis锁，使用默认过期时间
     *
     * @param key 锁Key
     * @return 成功返回锁Value，失败返回null
     */
    private String createRedisLock(String key) {
        String value = key + StringConst.UNDERLINE + System.nanoTime() + StringConst.UNDERLINE + LocalUtils.getLocalIp();
        boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, REDIS_LOCK_KEY_EXPIRE_MILLIS, TimeUnit.MILLISECONDS);
        return result ? value : null;
    }

    /**
     * 给对应key的redis锁续约
     *
     * @param key   redis锁 key
     * @param value redis锁 value
     * @return 续约结果 true：续约成功，false：续约失败
     */
    public boolean renewLockKey(String key, String value) {
        String res = redisTemplate.opsForValue().get(key);
        // 判断值是否是该线程设置的，如果不是则不续约
        if (!Objects.equals(value, res)) {
            return false;
        }
        return redisTemplate.expire(key, REDIS_LOCK_KEY_EXPIRE_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查redis锁
     *
     * @param key redis锁的key
     * @return true:成功，也就是锁没有被其他占有，false:失败
     */
    public boolean checkRedisLock(String key) {
        return redisTemplate.hasKey(key);
    }
}
