package moon.mlock.aspect;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import moon.mlock.annotation.CheckLock;
import moon.mlock.common.enums.LockTypeEnum;
import moon.mlock.common.exception.GetLockException;
import moon.mlock.common.exception.LockException;
import moon.mlock.factory.LockFactory;
import moon.mlock.lock.ILock;
import moon.mlock.proxy.RedisLockProxy;
import moon.mlock.utils.AspectUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * 分布式检查锁注解AOP切入点
 *
 * @author moon
 */
@Slf4j
@Aspect
@Order(value = Integer.MIN_VALUE)
public class CheckLockAspect {
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
    private final Map<String, String[]> checkLockAspectParamNamesCache = Maps.newConcurrentMap();

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
    private final Map<String, String> checkLockMethodParamsCache = Maps.newConcurrentMap();

    @Autowired
    private RedisLockProxy proxy;

    /**
     * 分布式检查锁切入点
     */
    @Pointcut("@annotation(moon.mlock.annotation.CheckLock)")
    public void checkLockAspect() {
        // do nothing
    }


    /**
     * 分布式检查锁环绕逻辑
     *
     * @param joinPoint 切面的切入点信息
     * @return 结果
     * @throws Throwable 异常
     */
    @Around("checkLockAspect()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String lockKey = null;
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();

            CheckLock checkLock = getCheckLock(method);
            Assert.notNull(checkLock, "获取@CheckLock注解失败！");

            lockKey = getLockKey(joinPoint, checkLock);
            String domain = checkLock.domain();
            LockTypeEnum lockType = checkLock.lockType();
            ILock lock = LockFactory.getLock(lockType, domain, lockKey);

            //检查锁
            boolean check = lock.checkLock();

            String lockName = lock.getClass().getSimpleName();
            log.info("checkLock domain={} lockKey={} lockName={} methodName={} check={}", domain, lockKey, lockName, method.getName(), check);
            if (check) {
                return joinPoint.proceed();
            } else if (checkLock.throwEx()) {
                Class<? extends LockException> exceptionClass = checkLock.ex();
                Constructor<? extends LockException> constructor = exceptionClass.getConstructor(String.class);
                throw constructor.newInstance(checkLock.exMsg());
            }
            return null;
        } catch (GetLockException e) {
            log.error("CheckLockAspect GetLockException, lockKey={}", lockKey, e);
            throw e;
        } catch (LockException e) {
            log.error("CheckLockAspect Business Exception, lockKey={}", lockKey, e);
            throw e;
        } catch (Exception e) {
            log.error("CheckLockAspect Exception, lockKey={}", lockKey, e);
            throw e;
        }
    }

    /**
     * 拼接Key
     *
     * @param joinPoint 切面的切入点信息
     * @param checkLock CheckLock注解信息
     * @return key
     */
    private String getLockKey(ProceedingJoinPoint joinPoint, CheckLock checkLock) {
        String[] keys = checkLock.keys();
        String[] keyValues = executeTemplate(keys, joinPoint);
        String key = String.join("_", keyValues);
        return checkLock.domain() + "_" + key;
    }
    /**
     * 执行表达式模板
     *
     * @param template  需要执行的表达式模板
     * @param joinPoint 切面的切入点信息
     * @return 表达式执行结果集
     */
    private String[] executeTemplate(String[] template, ProceedingJoinPoint joinPoint) {
        String methodLongName = joinPoint.getSignature().toLongString();
        Function<String, String[]> function = o -> discoverer.getParameterNames(getMethod(joinPoint));
        String[] paramNames = checkLockAspectParamNamesCache.computeIfAbsent(methodLongName, function);
        int len = paramNames.length;
        StandardEvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        if (args.length == len) {
            for (int i = 0; i < len; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        String[] result = new String[len];
        for (int i = 0; i < len; i++) {
            Expression expression = parser.parseExpression(template[i]);
            String value = expression.getValue(context, String.class);
            result[i] = value;
        }
        return result;
    }

    /**
     * 获取当前执行的方法（方法名+参数）
     *
     * @param joinPoint 切面的切入点信息
     * @return 当前执行的方法（方法名+参数）
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        String methodLongName = joinPoint.getSignature().toLongString();
        UnaryOperator<String> fun = AspectUtils::getMethodNameAndParams;
        String methodNameAndParam = checkLockMethodParamsCache.computeIfAbsent(methodLongName, fun);
        Method[] methods = joinPoint.getTarget().getClass().getMethods();
        for (Method method : methods) {
            String targetMethodAndParam = checkLockMethodParamsCache.computeIfAbsent(method.toString(), fun);
            if (methodNameAndParam.equals(targetMethodAndParam)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 获取CheckLock注解
     *
     * @param method 切面方法
     * @return CheckLock注解
     */
    private CheckLock getCheckLock(Method method) {
        try {
            return method.getAnnotation(CheckLock.class);
        } catch (Exception e) {
            log.error("getDCheckLock Exception", e);
            return null;
        }
    }
}
