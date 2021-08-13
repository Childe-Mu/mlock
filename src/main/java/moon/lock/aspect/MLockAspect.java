package moon.lock.aspect;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Map;

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
    private static LocalVariableTableParameterNameDiscoverer localVariableTableParameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    /**
     * 缓存参数名称
     */
    private static Map<String, String[]> mLockAspectParameterNamesCache = Maps.newConcurrentMap();

    /**
     * 方法与参数缓存
     */
    private static Map<String, String> mLockMethodParamCache = Maps.newConcurrentMap();

    /**
     * 分布式锁切入点
     */
    @Pointcut("@annotation(moon.lock.annotation.MLock)")
    public void mLockAspect() {
    }

    /**
     * 分布式锁环绕逻辑
     *
     * @return
     * @throws Throwable
     */
    @Around("mLockAspect()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        return null;
    }
}
