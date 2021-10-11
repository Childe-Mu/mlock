package moon.mlock.template.impl;

import lombok.extern.slf4j.Slf4j;
import moon.mlock.common.enums.LockTypeEnum;
import moon.mlock.factory.LockFactory;
import moon.mlock.lock.ILock;
import moon.mlock.template.ILockCallback;
import moon.mlock.template.ILockTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Lock模板类
 *
 * @author moon
 */
@Slf4j
public class LockTemplate<T> implements ILockTemplate<T> {
    /**
     * 执行方法
     *
     * @param lockType 默认锁类型
     * @param domain   业务类型，自定义
     * @param key      锁Key
     * @param timeout  超时时间
     * @param unit     超时时间单位
     * @param callback 回调函数
     * @return 执行结果
     */
    @Override
    public T execute(LockTypeEnum lockType, String domain, String key, long timeout, TimeUnit unit, ILockCallback<T> callback) {
        try (ILock lock = LockFactory.getLock(lockType, domain, key)) {
            if (lock.tryLock(timeout, unit)) {
                return callback.success();
            } else {
                return callback.fail();
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return callback.ex(e);
        }
    }
}
