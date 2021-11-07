package moon.mlock.aspect;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import moon.mlock.annotation.Lock;
import moon.mlock.common.enums.LockTypeEnum;
import moon.mlock.common.exception.GetLockException;
import moon.mlock.common.exception.LockException;
import moon.mlock.factory.LockFactory;
import moon.mlock.lock.ILock;
import moon.mlock.utils.AspectUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 分布式锁AOP切入点.
 *
 * @author moon
 */
@Aspect
@Order(value = Integer.MIN_VALUE)
@Slf4j
public class LockAspect {

    /**
     * Spring EL表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 获取方法参数
     */
    private final LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    /**
     * 缓存参数名称
     * <p>
     * key:方法全量名称字符串，value:方法的参数名称列表
     */
    private final Map<String, String[]> lockAspectParamNamesCache = Maps.newConcurrentMap();

    /**
     * 方法与参数缓存
     * <p>
     * key:方法全量名称字符串，value:方法名和参数
     * <p>
     * 例如，对于方法{@link LockAspect#doAround(org.aspectj.lang.ProceedingJoinPoint)}
     * <p>
     * key = public java.lang.Object moon.mlock.aspect.LockAspect.doAround(org.aspectj.lang.ProceedingJoinPoint)
     * <p>
     * value = doAround(org.aspectj.lang.ProceedingJoinPoint)
     */
    private final Map<String, String> lockMethodParamsCache = Maps.newConcurrentMap();

    /**
     * 分布式锁切入点
     */
    @Pointcut("@annotation(moon.mlock.annotation.Lock)")
    public void mLockAspect() {
        // do nothing
    }

    /**
     * 分布式锁环绕逻辑
     *
     * @param joinPoint 切面的切入点信息
     * @throws Throwable 异常
     */
    @Around("mLockAspect()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String lockKey = null;
        ILock lock = null;
        try {
            // 切入点处的签名
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();

            Lock mLock = getLock(method);
            Assert.notNull(mLock, "获取@Lock注解失败！");

            lockKey = getLocalKey(joinPoint, mLock);
            String domain = mLock.domain();
            LockTypeEnum lockTypeEnum = mLock.lockType();
            long waitTime = mLock.waitTime();
            lock = LockFactory.getLock(lockTypeEnum, domain, lockKey);

            //加锁
            boolean lockResult = lock.tryLock(waitTime, TimeUnit.MILLISECONDS);
            String lockName = lock.getClass().getSimpleName();
            log.info("domain={} lockKey={} lockName={} lockResult={} methodName={}", domain, lockKey, lockName, lockResult, method.getName());

            if (lockResult) {
                // 继续下一个目标方法调用
                return joinPoint.proceed();
            } else if (mLock.throwEx()) {
                Class<? extends Exception> ex = mLock.ex();
                // 获取入参为string的异常构造函数
                Constructor<? extends Exception> constructor = ex.getConstructor(String.class);
                throw constructor.newInstance(mLock.exMsg());
            } else {
                return null;
            }
        } catch (GetLockException e) {
            log.error("LockAspect GetLockException, lockKey={}", lockKey, e);
            throw e;
        } catch (LockException e) {
            log.error("LockAspect Business Exception, lockKey={}", lockKey, e);
            throw e;
        } catch (Exception e) {
            log.error("LockAspect Exception, lockKey={}", lockKey, e);
            throw e;
        } finally {
            //释放锁
            if (Objects.nonNull(lock)) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取 local key
     *
     * @param joinPoint 切面的切入点信息
     * @param lock     mLock注解信息
     * @return local key
     */
    private String getLocalKey(ProceedingJoinPoint joinPoint, Lock lock) {
        String[] keys = lock.keys();
        String[] keyValues = executeTemplate(keys, joinPoint);
        String key = String.join("_", keyValues);
        return lock.domain() + "_" + key;
    }

    /**
     * 执行表达式模板并返回结果
     *
     * @param template  需要执行的表达式模板
     * @param joinPoint 切面的切入点信息
     * @return 表达式执行结果集
     */
    private String[] executeTemplate(String[] template, ProceedingJoinPoint joinPoint) {
        // 获取方法全量签名
        String methodLongName = joinPoint.getSignature().toLongString();
        // 获取切入点处的方法
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        // 参数名称数组
        Function<String, String[]> function = o -> discoverer.getParameterNames(method);
        String[] paramNames = lockAspectParamNamesCache.computeIfAbsent(methodLongName, function);

        // SpEL 的标准计算上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        // 获取对应的参数
        Object[] args = joinPoint.getArgs();
        if (args.length == paramNames.length) {
            for (int i = 0; i < args.length; i++) {
                // 设置参数名和对应的参数值
                context.setVariable(paramNames[i], args[i]);
            }
        }

        String[] result = new String[template.length];
        for (int i = 0; i < template.length; i++) {
            Expression expression = parser.parseExpression(template[i]);
            String value = expression.getValue(context, String.class);
            result[i] = value;
        }
        return result;
    }

    /**
     * 获取Lock注解
     *
     * @param method 切面方法
     * @return Lock注解
     */
    private Lock getLock(Method method) {
        try {
            return method.getAnnotation(Lock.class);
        } catch (Exception e) {
            log.error("getLock from method ex:", e);
            return null;
        }
    }
}
