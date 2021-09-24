package moon.mlock.aspect;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import moon.mlock.annotation.Idempotent;
import moon.mlock.common.exception.IdempotentException;
import moon.mlock.proxy.RedisLockProxy;
import moon.mlock.utils.AspectUtils;
import org.aspectj.lang.ProceedingJoinPoint;
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
 * 分布式幂等AOP切入点
 *
 * @author moon
 */
@Slf4j
@Aspect
@Order(value = Integer.MIN_VALUE)
public class IdempotentAspect {
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
    private final Map<String, String[]> idempotentAspectParamNamesCache = Maps.newConcurrentMap();

    /**
     * 方法与参数缓存
     * <p>
     * key:方法全量名称字符串，value:方法名和参数
     * <p>
     * 例如，对于方法{@link MLockAspect#doAround(org.aspectj.lang.ProceedingJoinPoint)}
     * <p>
     * key = public java.lang.Object moon.mlock.aspect.MLockAspect.doAround(org.aspectj.lang.ProceedingJoinPoint)
     * <p>
     * value = doAround(org.aspectj.lang.ProceedingJoinPoint)
     */
    private final Map<String, String> idempotentMethodParamsCache = Maps.newConcurrentMap();

    @Autowired
    private RedisLockProxy proxy;

    /**
     * 分布式幂等切入点
     */
    @Pointcut("@annotation(moon.mlock.annotation.Idempotent)")
    public void idempotentAspect() {
        // do nothing
    }

    /**
     * 分布式幂等环绕逻辑
     *
     * @param joinPoint 切面的切入点信息
     * @return 结果
     * @throws Throwable 异常
     */
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String key = null;
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();

            Idempotent idempotent = getIdempotent(method);
            Assert.notNull(idempotent, "获取@Idempotent注解失败！");

            key = getIdempotentKey(joinPoint, idempotent);
            String domain = idempotent.domain();
            long ttl = idempotent.ttl();
            boolean re = proxy.tryRedisIdempotent(key, ttl);
            log.info("tryRedisIdempotent domain={} key={} result={} methodName={}", domain, key, re, method.getName());
            if (re) {
                return joinPoint.proceed();
            } else if (idempotent.throwEx()) {
                // 业务异常，抛出异常提示信息
                Class<? extends RuntimeException> ex = idempotent.ex();
                Constructor<? extends RuntimeException> constructor = ex.getConstructor(String.class);
                throw constructor.newInstance(idempotent.exMsg());
            }
            return null;
        } catch (IdempotentException e) {
            // 业务异常，释放幂等锁
            proxy.unlock(key);
            log.error("idempotentAspect Business ex, key={}", key, e);
            throw e;
        } catch (Exception e) {
            // 异常释放幂等锁
            proxy.unlock(key);
            log.error("idempotentAspect ex, key={}", key, e);
            throw e;
        }
    }

    /**
     * 拼接Key
     *
     * @param joinPoint  切面的切入点信息
     * @param idempotent Idempotent注解信息
     * @return key
     */
    private String getIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String[] keys = idempotent.keys();
        String[] keyValues = executeTemplate(keys, joinPoint);
        String key = String.join("_", keyValues);
        return idempotent.domain() + "_" + key;
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
        String[] paramNames = idempotentAspectParamNamesCache.computeIfAbsent(methodLongName, function);
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
        String methodNameAndParam = idempotentMethodParamsCache.computeIfAbsent(methodLongName, fun);
        Method[] methods = joinPoint.getTarget().getClass().getMethods();
        for (Method method : methods) {
            String targetMethodAndParam = idempotentMethodParamsCache.computeIfAbsent(method.toString(), fun);
            if (methodNameAndParam.equals(targetMethodAndParam)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 获取Idempotent注解
     *
     * @param method 切面方法
     * @return Idempotent注解
     */
    private Idempotent getIdempotent(Method method) {
        try {
            return method.getAnnotation(Idempotent.class);
        } catch (Exception e) {
            log.error("getIdempotent ex", e);
            return null;
        }
    }

}
