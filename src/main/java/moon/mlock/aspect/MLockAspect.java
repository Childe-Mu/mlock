package moon.mlock.aspect;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import moon.mlock.annotation.MLock;
import moon.mlock.common.enums.LockTypeEnum;
import moon.mlock.common.exception.GetLockException;
import moon.mlock.lock.Lock;
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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分布式锁AOP切入点.
 *
 * @author moon
 */
@Aspect
@Order(value = Integer.MIN_VALUE)
@Slf4j
public class MLockAspect {

    /**
     * Spring EL表达式解析器
     */
    private static ExpressionParser parser = new SpelExpressionParser();

    /**
     * 获取方法参数
     */
    private static LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    /**
     * 缓存参数名称
     * <p>
     * key:方法全量名称字符串，value:方法的参数名称列表
     */
    private static Map<String, String[]> mLockAspectParamNamesCache = Maps.newConcurrentMap();

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
    private static Map<String, String> mLockMethodParamsCache = Maps.newConcurrentMap();

    /**
     * 分布式锁切入点
     */
    @Pointcut("@annotation(moon.mlock.annotation.MLock)")
    public void mLockAspect() {
    }

    /**
     * 分布式锁环绕逻辑
     *
     * @param joinPoint 切面的切入点信息
     * @throws Throwable
     */
    @Around("mLockAspect()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String lockKey = null;
        Lock lock = null;
        try {
            // 切入点处的签名
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();

            MLock mLock = getMLock(method);
            Assert.notNull(mLock, "获取mLock注解失败！");

            lockKey = getLocalKey(joinPoint, mLock);
            String domain = mLock.domain();
            LockTypeEnum lockTypeEnum = mLock.lockType();
            long waitTime = mLock.waitTime();
            lock = MLockFactory.getLock(lockTypeEnum, domain, lockKey);

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
            log.error("MLockAspect GetLockException, lockKey={}", lockKey, e);
            throw e;
        } catch (RuntimeException e) {
            log.error("MLockAspect BusinessRuntimeException, lockKey={}", lockKey, e);
            throw e;
        } catch (Exception e) {
            log.error("MLockAspect Exception, lockKey={}", lockKey, e);
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
     * @param mLock     mLock注解信息
     * @return local key
     */
    private String getLocalKey(ProceedingJoinPoint joinPoint, MLock mLock) {
        String[] keys = mLock.keys();
        Object[] keyValues = executeTemplate(keys, joinPoint);
        String key = Arrays.stream(keyValues).map(String::valueOf).collect(Collectors.joining("_"));
        return mLock.domain() + "_" + key;
    }

    /**
     * 执行表达式模板并返回结果
     *
     * @param template  需要执行的表达式魔板
     * @param joinPoint 切面的切入点信息
     * @return 表达式执行结果集
     */
    private Object[] executeTemplate(String[] template, ProceedingJoinPoint joinPoint) {
        // 获取方法全量签名
        String methodLongName = joinPoint.getSignature().toLongString();
        // 参数名称数组
        String[] paramNames;
        if (mLockAspectParamNamesCache.containsKey(methodLongName)) {
            paramNames = mLockAspectParamNamesCache.get(methodLongName);
        } else {
            Method method = getMethod(joinPoint);
            paramNames = discoverer.getParameterNames(method);
            //缓存参数名称
            mLockAspectParamNamesCache.put(methodLongName, paramNames);
        }

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

        Object[] result = new Object[template.length];

        for (int i = 0; i < template.length; i++) {
            Expression expression = parser.parseExpression(template[i]);
            Object value = expression.getValue(context, Object.class);
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
        String methodNameAndParams;
        // 方法名称参数已缓存，则直接从缓存获取，反之则解析名称参数，并加入缓存
        if (mLockMethodParamsCache.containsKey(methodLongName)) {
            methodNameAndParams = mLockMethodParamsCache.get(methodLongName);
        } else {
            methodNameAndParams = AspectUtils.getMethodNameAndParams(methodLongName);
            mLockMethodParamsCache.put(methodLongName, methodNameAndParams);
        }
        // 获取切点所在类的全部方法列表
        Method[] methods = joinPoint.getTarget().getClass().getMethods();
        Method method = null;
        for (Method m : methods) {
            String targetMethodLongName = m.toString();
            String targetMethodAndParam;
            if (mLockMethodParamsCache.containsKey(targetMethodLongName)) {
                targetMethodAndParam = mLockMethodParamsCache.get(targetMethodLongName);
            } else {
                targetMethodAndParam = AspectUtils.getMethodNameAndParams(targetMethodLongName);
                mLockMethodParamsCache.put(targetMethodLongName, targetMethodAndParam);
            }
            if (StringUtils.equals(methodNameAndParams, targetMethodAndParam)) {
                method = m;
                break;
            }
        }
        return method;
    }

    /**
     * 获取MLock注解
     *
     * @param method 切面方法
     * @return MLock注解
     */
    private MLock getMLock(Method method) {
        try {
            return method.getAnnotation(MLock.class);
        } catch (Exception e) {
            log.error("getMLock from method ex:", e);
            return null;
        }
    }
}
