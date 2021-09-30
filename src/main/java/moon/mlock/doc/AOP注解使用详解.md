# AOP注解使用详解

AOP为Aspect Oriented Programming的缩写，意为：面向切面编程，通过预编译方式和运行期动态代理实现程序功能的统一维护的一种技术.AOP是OOP的延续，是软件开发中的一个热点，也是Spring框架中的一个重要内容，是函数式编程的一种衍生范型。利用AOP可以对业务逻辑的各个部分进行隔离，从而使得业务逻辑各部分之间的耦合度降低，提高程序的可重用性，同时提高了开发的效率。

在spring AOP中业务逻辑仅仅只关注业务本身，将日志记录，性能统计，安全控制，事务处理，异常处理等代码从业务逻辑代码中划分出来，通过对这些行为的分离，我们希望可以将它们独立到非指导业务逻辑的方法中，进而改变这些行为的时候不影响业务逻辑的代码。

相关注解介绍：

@Aspect:作用是把当前类标识为一个切面供容器读取
@Pointcut：Pointcut是植入Advice的触发条件。每个Pointcut的定义包括2部分，一是表达式，二是方法签名。方法签名必须是 public及void型。可以将Pointcut中的方法看作是一个被Advice引用的助记符，因为表达式不直观，因此我们可以通过方法签名的方式为 此表达式命名。因此Pointcut中的方法只需要方法签名，而不需要在方法体内编写实际代码。
@Around：环绕增强，相当于MethodInterceptor
@AfterReturning：后置增强，相当于AfterReturningAdvice，方法正常退出时执行
@Before：标识一个前置增强方法，相当于BeforeAdvice的功能，相似功能的还有
@AfterThrowing：异常抛出增强，相当于ThrowsAdvice
@After: final增强，不管是抛出异常或者正常退出都会执行
使用pointcut代码：

package com.aspectj.test.advice;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class AdviceTest {
@Around("execution(* com.abc.service.*.many*(..))")
public Object process(ProceedingJoinPoint point) throws Throwable {
System.out.println("@Around：执行目标方法之前...");
//访问目标方法的参数：
Object[] args = point.getArgs();
if (args != null && args.length > 0 && args[0].getClass() == String.class) {
args[0] = "改变后的参数1";
}
//用改变后的参数执行目标方法
Object returnValue = point.proceed(args);
System.out.println("@Around：执行目标方法之后...");
System.out.println("@Around：被织入的目标对象为：" + point.getTarget());
return "原返回值：" + returnValue + "，这是返回结果的后缀";
}

    @Before("execution(* com.abc.service.*.many*(..))")
    public void permissionCheck(JoinPoint point) {
        System.out.println("@Before：模拟权限检查...");
        System.out.println("@Before：目标方法为：" + 
                point.getSignature().getDeclaringTypeName() + 
                "." + point.getSignature().getName());
        System.out.println("@Before：参数为：" + Arrays.toString(point.getArgs()));
        System.out.println("@Before：被织入的目标对象为：" + point.getTarget());
    }
    
    @AfterReturning(pointcut="execution(* com.abc.service.*.many*(..))", 
        returning="returnValue")
    public void log(JoinPoint point, Object returnValue) {
        System.out.println("@AfterReturning：模拟日志记录功能...");
        System.out.println("@AfterReturning：目标方法为：" + 
                point.getSignature().getDeclaringTypeName() + 
                "." + point.getSignature().getName());
        System.out.println("@AfterReturning：参数为：" + 
                Arrays.toString(point.getArgs()));
        System.out.println("@AfterReturning：返回值为：" + returnValue);
        System.out.println("@AfterReturning：被织入的目标对象为：" + point.getTarget());
        
    }
    
    @After("execution(* com.abc.service.*.many*(..))")
    public void releaseResource(JoinPoint point) {
        System.out.println("@After：模拟释放资源...");
        System.out.println("@After：目标方法为：" + 
                point.getSignature().getDeclaringTypeName() + 
                "." + point.getSignature().getName());
        System.out.println("@After：参数为：" + Arrays.toString(point.getArgs()));
        System.out.println("@After：被织入的目标对象为：" + point.getTarget());
    }
}
使用annotation代码：

//注解实体类
package com.trip.demo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface SMSAndMailSender {
/*短信模板String格式化串*/
String value() default "";

    String smsContent() default "";
 
    String mailContent() default "";
    /*是否激活发送功能*/
    boolean isActive() default true;
    /*主题*/
    String subject() default "";
}



//切面类
@Aspect
@Component("smsAndMailSenderMonitor")
public class SMSAndMailSenderMonitor {

    private Logger logger = LoggerFactory.getLogger(SMSAndMailSenderMonitor.class);
 
 
    /**
     * 在所有标记了@SMSAndMailSender的方法中切入
     * @param joinPoint
     * @param result
     */
    @AfterReturning(value="@annotation(com.trip.demo.SMSAndMailSender)", returning="result")//有注解标记的方法，执行该后置返回
    public void afterReturning(JoinPoint joinPoint , Object result//注解标注的方法返回值) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method method = ms.getMethod();
        boolean active = method.getAnnotation(SMSAndMailSender.class).isActive();
        if (!active) {
            return;
        }
        String smsContent = method.getAnnotation(SMSAndMailSender.class).smsContent();
        String mailContent = method.getAnnotation(SMSAndMailSender.class).mailContent();
        String subject = method.getAnnotation(SMSAndMailSender.class).subject();
       
    }
 
    
 
    /**
     * 在抛出异常时使用
     * @param joinPoint
     * @param ex
     */
    @AfterThrowing(value="@annotation(com.trip.order.monitor.SMSAndMailSender)",throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex//注解标注的方法抛出的异常) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method method = ms.getMethod();
        String subject = method.getAnnotation(SMSAndMailSender.class).subject();
        
    }

}


//实体类中使用该注解标注方法
@Service("testService ")
public class TestService {


    @Override
    @SMSAndMailSender(smsContent = "MODEL_SUBMIT_SMS", mailContent =     
    "MODEL_SUPPLIER_EMAIL", subject = "MODEL_SUBJECT_EMAIL")
    public String test(String param) {
        return "success";
    }
｝


