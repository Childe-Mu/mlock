package moon.mlock.template;

import moon.mlock.common.enums.LockTypeEnum;

import java.util.concurrent.TimeUnit;

/**
 * Lock 模板类
 *
 * @author moon
 */
public interface ILockTemplate<T> {
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
    T execute(LockTypeEnum lockType, String domain, String key, long timeout, TimeUnit unit, ILockCallback<T> callback);

}
