package moon.mlock.annotation;

import moon.mlock.common.enums.LockTypeEnum;
import moon.mlock.common.exception.LockException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式检查锁注解
 * <p>
 * 检查锁的存在，不加锁，若已加锁，跳过执行或抛出指定异常
 * <p>
 * Lock和CheckLock的异同：Lock会等待执行，CheckLock只要检查到锁已经被持有就退出
 *
 * @author moon
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckLock {

    /**
     * 锁类型
     * <p>
     * 非必须，默认 redis
     *
     * @return 锁类型
     */
    LockTypeEnum lockType() default LockTypeEnum.LOCK_REDIS;

    /**
     * 业务领域
     * <p>
     * 相当于redis key的前缀
     * <p>
     * 必须，默认 m_lock
     *
     * @return 业务领域
     */
    String domain() default "m_lock";

    /**
     * key组成要素
     * <p>
     * 拼接在domain后面，即 domain_key1_key2..._keyn，共同组成redis key
     * <p>
     * 必须
     *
     * @return key数组
     */
    String[] keys() default {};

    /**
     * 失败时是否抛出异常
     * <p>
     * 非必须，默认 true
     *
     * @return 失败时是否抛出异常，true：抛出异常，false：不抛出异常
     */
    boolean throwEx() default true;

    /**
     * 失败时，抛出的异常类
     * <p>
     * 非必须，默认 LockException
     *
     * @return 抛出的异常类
     */
    Class<? extends LockException> ex() default LockException.class;

    /**
     * 抛出Exception的message
     * <p>
     * 非必须，默认 其他操作正在处理中，请稍后再试！
     *
     * @return 异常提示信息
     */
    String exMsg() default "其他操作正在处理中，请稍后再试！";
}
