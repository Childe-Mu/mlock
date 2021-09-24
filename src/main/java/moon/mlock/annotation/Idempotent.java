package moon.mlock.annotation;

import moon.mlock.common.exception.IdempotentException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式幂等注解
 *
 * @author moon
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 分布式幂等保留最长时间，单位s
     * <p>
     * 非必须，默认5分钟
     *
     * @return 分布式幂等过期时间
     */
    long ttl() default 300;

    /**
     * 业务领域
     * <p>
     * 非必须，表示一种业务场景
     *
     * @return 业务领域
     */
    String domain() default "idempotent";

    /**
     * key的组成数组
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
    Class<? extends IdempotentException> ex() default IdempotentException.class;

    /**
     * 抛出Exception的message
     * <p>
     * 非必须，默认 已处理，请不要重复处理！
     *
     * @return 异常提示信息
     */
    String exMsg() default "已处理，请不要重复处理！";
}
