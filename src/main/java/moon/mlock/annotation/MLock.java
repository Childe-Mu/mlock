package moon.mlock.annotation;

import moon.mlock.common.enums.LockTypeEnum;
import moon.mlock.common.exception.MLockException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解
 *
 * @author moon
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MLock {
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
     * key组成
     * <p>
     * 拼接在domain后面，即 domain_key1_key2..._keyn，共同组成redis key
     * <p>
     * 必须
     *
     * @return key数组
     */
    String[] keys() default {};

    /**
     * 锁等待最长时间，单位ms
     * <p>
     * 非必须，默认 0ms，即不尝试重新获取锁
     *
     * @return 锁等待最长时间
     */
    long waitTime() default 0;

    /**
     * 失败时是否抛出异常
     * <p>
     * 非必须，默认 false，即不抛出异常，直接结束
     *
     * @return 失败时是否抛出异常，true：抛出异常，false：不抛出异常
     */
    boolean throwEx() default false;

    /**
     * 失败时，抛出的异常类
     * <p>
     * 非必须，默认 MLockException
     *
     * @return 抛出的异常类
     */
    Class<? extends Exception> ex() default MLockException.class;

    /**
     * 抛出Exception的message
     * <p>
     * 非必须，默认 其他操作正在处理中，请稍后再试！
     *
     * @return message
     */
    String exMsg() default "其他操作正在处理中，请稍后再试！";
}
