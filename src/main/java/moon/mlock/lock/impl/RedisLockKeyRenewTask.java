package moon.mlock.lock.impl;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Iterator;
import java.util.Map;

/**
 * Redis Key 续约
 *
 * @author moon
 */
@Slf4j
public class RedisLockKeyRenewTask {

    /**
     * 所有redis锁的key
     */
    private static final Map<String, String> REDIS_LOCK_KEY_MAP = Maps.newConcurrentMap();

    /**
     * redis lock 代理
     */
    @Autowired
    private RedisLockProxy proxy;

    /**
     * 给所有redis锁续约（定时任务执行）
     * 暂定5秒钟执行一次
     * todo 这里也有需要在仔细考虑下，之前写的时候没有考虑情况所有情况，不能盲目续约，有可能会导致锁一直超时不了，同时续约应该是可选的
     */
    @Scheduled(cron = "*/5 * * * * ?")
    private void renew() {
        try {
            log.debug("REDIS_LOCK_KEY_MAP Size={}", REDIS_LOCK_KEY_MAP.size());
            Iterator<Map.Entry<String, String>> iterator = REDIS_LOCK_KEY_MAP.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> next = iterator.next();
                String key = next.getKey();
                String value = next.getValue();
                log.debug("redis key={}，开始续约，value={}", key, value);
                boolean result = proxy.renewLockKey(key, value);
                log.debug("redis key={}，结束续约，result={}", key, result);
                if (!result) {
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            log.error("RedisLockKeyRenew renew ex:", e);
        }
    }

    /**
     * 将redis锁的k-v放入缓存中，以便续约
     *
     * @param key   redis key
     * @param value redis value
     */
    public static void putLockKey(String key, String value) {
        REDIS_LOCK_KEY_MAP.put(key, value);
    }


    /**
     * 删除LockKey
     *
     * @param key redis key
     */
    public static void removeLockKey(String key) {
        REDIS_LOCK_KEY_MAP.remove(key);
    }
}
