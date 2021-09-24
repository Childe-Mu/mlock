package moon.mlock.factory;

import moon.mlock.common.enums.LockTypeEnum;
import moon.mlock.common.exception.GetLockException;
import moon.mlock.config.LockProperties;
import moon.mlock.lock.ILock;
import moon.mlock.lock.impl.NoLock;
import moon.mlock.lock.impl.ReentrantRedisLock;

import java.util.Objects;

/**
 * Lock工厂类
 *
 * @author moon
 */
public class LockFactory {

    /**
     * Lock配置中心配置数据，后期考虑可以从公共数据源的配置中心获取锁配置，配置中心的配置优先级可高于入参
     */
    private static LockProperties lockProperties;

    private LockFactory(LockProperties lockProperties) {
        LockFactory.lockProperties = lockProperties;
    }

    /**
     * 根据lockType 获取锁，可根据配置中心进行锁降级
     * 优先级  无锁 > 强制redis锁 > redis锁
     *
     * @param lockType 锁类型
     * @param domain   业务领域
     * @param lockKey  local key
     * @return 获取锁
     */
    public static ILock getLock(LockTypeEnum lockType, String domain, String lockKey) throws GetLockException {
        ILock lock;
        switch (lockType) {
            case LOCK_NOTHING:
                lock = new NoLock();
                break;
            case LOCK_REDIS:
            case LOCK_REDIS_FORCE:
                lock = new ReentrantRedisLock(domain, lockKey);
                break;
            default:
                lock = null;
        }
        if (Objects.isNull(lock)) {
            throw new GetLockException("DLockFactory getLock 获取锁失败");
        }
        return lock;
    }
}
