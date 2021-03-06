package moon.mlock.proxy;

import lombok.extern.slf4j.Slf4j;
import moon.mlock.common.consts.StringConst;
import moon.mlock.config.LockProperties;
import moon.mlock.utils.LocalUtils;
import moon.mlock.utils.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
@Service
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

    public RedisLockProxy(LockProperties lockProperties) {
        // 具体如何取redisTemplate实例，需要根据项目具体设置，通常是 集群名称+RedisTemplate，如ShopRedisTemplate
        redisTemplate = SpringUtils.getBean(lockProperties.getRedisGroupName() + "RedisTemplate");
        // 初始化redis连接并测试
        String key = "ILock-connect-test-" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(key, key);
        String val = redisTemplate.opsForValue().get(key);
        log.info("ILock connect test: set={}, get={}", key, val);
        redisTemplate.delete(key);
        log.info("Successfully initialized ILock redis connection");
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
            value = createRedisLock(key, REDIS_LOCK_KEY_EXPIRE_MILLIS, TimeUnit.MILLISECONDS);
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
     * 尝试添加分布式幂等锁
     *
     * @param key     key
     * @param timeout 超时时间(单位:s)
     * @return 加锁结果
     */
    public boolean tryRedisIdempotent(String key, long timeout) {
        String value = createRedisLock(key, timeout, TimeUnit.SECONDS);
        return Objects.nonNull(value);
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

    /**
     * 解锁
     * <p>
     * [注]：这里不用考虑删除失败的问题，因为即使删除失败，锁也会在很短的时间内过期
     *
     * @param key   redis锁 key
     * @param value redis锁 value
     */
    public void unlock(String key, String value) {
        String val = redisTemplate.opsForValue().get(key);
        if (StringUtils.equals(value, val)) {
            redisTemplate.delete(key);
        }
    }

    /**
     * 解锁
     * <p>
     * [注]：这里不用考虑删除失败的问题，因为即使删除失败，锁也会在很短的时间内过期
     *
     * @param key redis锁 key
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
