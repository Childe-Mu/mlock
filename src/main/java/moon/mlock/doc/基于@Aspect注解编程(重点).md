# 基于@Aspect注解编程(重点)

### 1、说明

Spring 使用了和AspectJ 一样的注解并使用AspectJ来做切入点解析和匹配。但是，AOP在运行时仍旧是纯的Spring AOP，并不依赖于AspectJ的编译器或者织入器（weaver）(编译器与织入器暂时不要管)

### 2、启用@AspectJ支持

1. 说明

   为了在Spring中使用@AspectJ切面，你首先必须启用Spring对@AspectJ切面配置的支持，并确保开启自动代理。自动代理是指Spring会判断一个bean是否使用了一个或多个切面通知，并据此自动生成相应的代理以拦截其方法调用，并且确保通知在需要时执行

2. 新建spring-aspect.xml配置文件

   

   ```jsx
   <?xml version="1.0" encoding="UTF-8"?
   <beans xmlns="http://www.springframework.org/schema/beans"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xmlns:aop="http://www.springframework.org/schema/aop"
          xmlns:context="http://www.springframework.org/schema/context"
          xsi:schemaLocation="http://www.springframework.org/schema/beans
          http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd"
       <context:component-scan base-package="com.wener.example.aop.aspect"/
    <!-- 有了这个Spring就能够自动扫描被@Aspect标注的切面了 -->
       <!-- 开启自动代理 -->    
       <aop:aspectj-autoproxy/>
   </beans>
   ```

### 2、声明一个切面

1. 说明

   在代码中定义一个类任意在类上使用@Aspect注解

2. 示例代码

   

   ```java
   import org.aspectj.lang.annotation.Aspect;
   @Aspect
   public class LogAspect {
   }
   ```

### 3、声明一个切入点

1. 说明

   切入点决定了连接点关注的内容，使得我们可以控制通知什么时候执行。Spring AOP只支持Spring bean的方法执行连接点。所以你可以把切入点看做是Spring bean上方法执行的匹配。一个切入点声明有两个部分：

   - **包含名字和任意参数的签名：**一个切入点签名通过一个普通的方法定义来提供，并且切入点表达式使用`@Pointcut`注解来表示（作为切入点签名的方法必须返回`void` 类型)
   - **切入点表达式：**切入点表达式决定了我们关注哪些方法的执行,详细表达式语法后面在说。

2. 语法格式

   

   ```kotlin
   @Pointcut(value="", argNames = "")
   ```

3. 参数说明

   - value

     指定切入点表达式

   - argNames

     指定命名切入点方法参数列表参数名字，可以有多个用“，”分隔，这些参数将传递给通知方法同名的参数

4. 示例代码

   

   ```java
   @Aspect
   public class LogAspect {
       // 也可以在通知上定义,当需要复用切入点的时候
       @Pointcut("execution(* com.wener.example.aop.aspect.*.*(..))")  
       // 返回值 必须是void类型
       public void log() {
       }
   }
   ```

5. 备注

   切入点的定义是非必要的,也可以直接在通知上使用切入点表达式

### 4、声明通知

#### 4.1、说明

通知是跟一个切入点表达式关联起来的，并且在切入点匹配的方法执行之前或者之后或者前后运行。 切入点表达式可能是指向已命名的切入点的简单引用或者是一个已经声明过的切入点表达式，通知的类型就是我们前面提到过的类型

#### 4.2、前置通知

1. 说明

   在关注点执行前运行的方法，切面里使用 `@Before` 注解声明前置通知

2. 语法格式

   

   ```java
   @Before(value = "", argNames = "")
   ```

3. 参数说明

   - **value :**指定切入点表达式或切入点名字；
   - **argNames:** 用来接收AspectJ表达式中的参数,并指定通知方法中的参数

4. 示例代码

   

   ```java
   import org.springframework.stereotype.Component;
   
   import org.aspectj.lang.annotation.Aspect;
   import org.aspectj.lang.annotation.Pointcut;
   import org.aspectj.lang.annotation.Before;
   
   @Aspect
   @Component
   public class LogAspect {
       /**
        * @Pointcut() 切入点表达式
        */
       @Pointcut("execution(* com.wener.example.aop.aspect.*.*(..))")
       public void logPointcut() {
   
       }
       /**
        * @Before 前置通知
        * value：指定切入点表达式或命名切入点；
        * argNames：与Schema方式配置中的同义；
        */
       @Before("logPointcut()")
       public void before() {
           System.out.println("前置通知");
       }
   }
   ```

#### 4.3、后置通知(最终通知)

1. 说明

   不论一个方法是如何结束的，最终通知都会运行。使用`@After` 注解来声明。最终通知必须准备处理正常返回和异常返回两种情况。通常用它来释放资源。相当于异常处理里finally的代码

2. 语法格式

   

   ```java
   @After(value = "", argNames = "")
   ```

3. 参数

   - **value :**指定切入点表达式或切入点名字；
   - **argNames: **用来接收AspectJ表达式中的参数,并指定通知方法中的参数

4. 示例代码

   

   ```java
   import org.springframework.stereotype.Component;
   
   import org.aspectj.lang.annotation.Aspect;
   import org.aspectj.lang.annotation.Pointcut;
   import org.aspectj.lang.annotation.After;
   import org.aspectj.lang.annotation.Before;
   
   @Aspect
   @Component
   public class LogAspect {
       /**
        * @Pointcut() 切入点表达式
        */
       @Pointcut("execution(* com.wener.example.aop.aspect.*.*(..))")
       public void logPointcut() {
   
       }
       /**
        * @After 后置通知 
        */
       @After(value = "logPointcut()")
       public void after() {
           System.out.println("后置通知");
       }
   }
   ```

#### 4.4、返回通知

1. 说明

   返回后通知通常在一个匹配的方法返回的时候执行。使用 `@AfterReturning` 注解来声明

2. 语法格式

   

   ```java
   @AfterReturning(value="",pointcut="",returning="",argNames="")
   ```

3. 参数说明

   - **value**：指定切入点表达式或切入点名字；
   - **pointcut**：指定切入点表达式或命名切入点，如果指定了将覆盖value属性的，pointcut具有高优先级；
   - **returning**：如果你想获取方法的返回值可以使用该参数,在通知方法中定义参数就可以了
   - **argNames**：用来接收AspectJ表达式中的参数,并指定通知方法中的参数

4. 示例代码

   

   ```java
   import org.springframework.stereotype.Component;
   
   import org.aspectj.lang.annotation.Aspect;
   import org.aspectj.lang.annotation.Pointcut;
   import org.aspectj.lang.annotation.After;
   import org.aspectj.lang.annotation.Before;
   import org.aspectj.lang.annotation.AfterThrowing;
   import org.aspectj.lang.annotation.AfterReturning;
   import org.aspectj.lang.annotation.Around;
   import org.aspectj.lang.ProceedingJoinPoint;
   
   @Aspect
   @Component
   public class LogAspect {
       /**
        * @Pointcut() 切入点表达式
        */
       @Pointcut("execution(* com.wener.example.aop.aspect.*.*(..))")
       public void logPointcut() {
   
       }
    /**
     * 不获取方法的返回值
        */
       @AfterReturning(value = "logPointcut()")
       public void AfterReturning1() {
           System.out.println("异常通知");
       }
       /**
        * 获取方法的返回值
        * returning的赋值的名字,必须跟通知方法中参数的名字保持一致
        */
       @AfterReturning(value = "logPointcut()", returning = "val")
       public Object afterReturning(Object val) {
           System.out.println("返回后通知");
           return val;
       }
   
   }
   ```

#### 4.5、异常通知

1. 说明

   抛出异常通知在一个方法抛出异常后执行。使用`@AfterThrowing`注解来声明

2. 语法格式

   

   ```kotlin
   @AfterThrowing(value="",pointcut="",throwing="",argNames="")
   ```

3. 参数说明

   - value：指定切入点表达式或命名切入点；
   - pointcut：指定切入点表达式或命名切入点，如果指定了将覆盖value属性的，pointcut具有高优先级；
   - throwing：异常类型；并且在通知方法中定义异常参数；
   - argNames：用来接收AspectJ表达式中的参数,并指定通知方法中的参数；

4. 示例代码

   

   ```java
   import org.springframework.stereotype.Component;
   
   import org.aspectj.lang.annotation.Aspect;
   import org.aspectj.lang.annotation.Pointcut;
   import org.aspectj.lang.annotation.After;
   import org.aspectj.lang.annotation.Before;
   import org.aspectj.lang.annotation.AfterThrowing;
   import org.aspectj.lang.annotation.AfterReturning;
   import org.aspectj.lang.annotation.Around;
   import org.aspectj.lang.ProceedingJoinPoint;
   
   @Aspect
   @Component
   public class LogAspect {
       /**
        * @Pointcut() 切入点表达式
        */
       @Pointcut("execution(* com.wener.example.aop.aspect.*.*(..))")
       public void logPointcut() {
        
       }
       /**
        * @AfterThrowing 异常通知 
        *   value：指定切入点表达式或命名切入点；
        *   throwing：异常类型。
        */
       @AfterThrowing("logPointcut()")
       public void afterThrowing() {
           System.out.println("异常通知");
       }
       /**
        * 如果想要限制通知只在某种特定的异常被抛出的时候匹配，同时还想知道异常的一些信息。 
        * 那我们就需要使用throwing属性声明响应
        */
    @AfterThrowing(value = "logPointcut()", throwing = "exception")
       public void afterThrowing(Exception exception) {
           System.out.println("异常通知");
       }
   }
   ```

#### 4.6、环绕通知

1. 说明

   环绕通知在一个方法执行之前和之后执行。它使得通知有机会 在一个方法执行之前和执行之后运行。而且它可以决定这个方法在什么时候执行，如何执行，甚至是否执行。 环绕通知经常在某线程安全的环境下，你需要在一个方法执行之前和之后共享某种状态的时候使用。 请尽量使用最简单的满足你需求的通知。（比如如果简单的前置通知也可以适用的情况下不要使用环绕通知）。

   - 使用`@Around`注解；
   - 环绕通知需要携带ProceedingJoinPoint类型的参数；
   - 且环绕通知必须有返回值，返回值即为有目标方法的返回值。

2. 语法格式

   

   ```java
   @Around(value = "", argNames = "")
   ```

3. 参数

   - **value :**指定切入点表达式或切入点名字；
   - **argNames: **用来接收AspectJ表达式中的参数,并指定通知方法中的参数

4. 示例代码

   

   ```java
   import org.springframework.stereotype.Component;
   
   import org.aspectj.lang.annotation.Aspect;
   import org.aspectj.lang.annotation.Pointcut;
   import org.aspectj.lang.annotation.After;
   import org.aspectj.lang.annotation.Before;
   import org.aspectj.lang.annotation.AfterThrowing;
   import org.aspectj.lang.annotation.AfterReturning;
   import org.aspectj.lang.annotation.Around;
   import org.aspectj.lang.ProceedingJoinPoint;
   
   @Aspect
   @Component
   public class LogAspect {
       /**
        * @Pointcut() 切入点表达式
        */
       @Pointcut("execution(* com.wener.example.aop.aspect.*.*(..))")
       public void logPointcut() {
   
       }
       /**
        * @Around 环绕通知
        * 比如 缓存切面，如果缓存中有值，就返回该值，否则调用proceed()方法
        * value：指定切入点表达式或命名切入点；
        * 注意 第一个参数必须是 ProceedingJoinPoint对象 具体这个类的更多详细使用看附录:
        */
       @Around(value = "logPointcut()")
       public Object around(ProceedingJoinPoint pjp) throws Throwable {
           System.out.println("环绕通知1");
           Object obj = pjp.proceed();
           System.out.println("环绕通知2");
           return obj;
       }
   }
   ```

#### 4.7、通知参数

1. 说明

   若想要在通知方法获取被通知方法的参数共有两种方式：自动获取、手动指定

   - 自动获取参数：通知类型可以通过参数JoinPoint或者 ProceedingJoinPoint 自动获取被通知方法的参数值并调用该方法
   - 手动指定参数：即在配置切面时，需在切面的通知与切面的切点中明确指定参数。

2. 手动指定

   - 在@pointcut中切入表达式中使用args声明匹配的参数,注意使用&&连接args

   - 在@pointcut中切入表达式中使用参数argNames用来接收AspectJ表达式中的参数，

     argNames属性是用于指定在表达式中应用的参数名与Advice方法参数是如何对应的

   - 在通知方法中定义参数

3. 手动获取指定参数

   

   ```java
   import org.aspectj.lang.annotation.*;
   import org.springframework.stereotype.Component;
   @Aspect
   @Component
   public class LogAdviceParamsAspect {
    // 注意参数的个数必须一致,否则匹配不到
       @Before(value = "execution(* com.wener.example.aop.aspect.*.*(..))&& args(id,name)", argNames = "id,name")
       public void testArgs(Object id, Object name) {
           System.out.println(id);
           System.out.println(name);
       }
   }
   ```

4. 混用使用

   **当同时采用自动获取参数与手动指定参数时，自动获取参数必须是第一个参数，即ProceedingJoinPoint 等参数并需是通知方法定义的第一个参数**

   

   ```java
   import org.aopalliance.intercept.Joinpoint;
   import org.aspectj.lang.annotation.*;
   import org.springframework.stereotype.Component;
   
   @Aspect
   @Component
   public class LogAdviceParamsAspect {
     // args、argNames的参数名与testArgs()方法中参数名 保持一致
       @Before(value = "execution(* com.wener.example.aop.aspect.*.*(..))&& args(id,name)", argNames = "id,name")
       public void testArgs(Object id, Object name) {
           System.out.println(id);
           System.out.println(name);
       }
    // 也可以不用argNames
       @Before(value = "execution(* com.wener.example.aop.aspect.*.*(..))&& args(id,name)")
       public void testArgs(Object id, Object name) {
           System.out.println(id);
           System.out.println(name);
       }
       
       @Around(value = "execution(* com.wener.example.aop.aspect.*.*(..))&&(args(id,name,..))", argNames = "pjp,id,name")
       public Object testAroundArgs(ProceedingJoinPoint pjp, Object id, Object name) throws Throwable {
           System.out.println("Around之前");
           Object obj = pjp.proceed();
           System.out.println();
           return obj;
       }
   }
   ```

#### 4.8 、引入

1. 说明

   有时候有一组共享公共行为类。在OOP中，它们必须扩展相同的基类或者实现相同的接口。此外，Java的单继承机制仅允许一个类最多扩展一个基类。所以，不能同时从多个实现类中继承行为。

   解决方案：引入是AOP中的一种特殊的通知。它允许为一个接口提供实现类，使对象动态的实现接口。就像对象在运行时扩展了实现类。而且，可以用多个实现类将多个接口同时引入对象。这可以实现与多重继承相同的效果。

2. 在开发中用的不是很多,所以不做过多的分析

### 5、声明代理类

1. 说明

   被代理的对象,跟前面说的一样,代理接口或者类都可以

2. 示例代码

   

   ```java
   public interface AspectDao {
       public void test();
       public void testParams(int id, String name);
       public void testParams(Joinpoint jp, int id, String name);
   }
   
   @Component("aspectDao")
   public class AspectDaoImpl implements AspectDao {
       @Override
       public void test() {
           System.out.println("核心测试方法");
       }
       @Override
       public void testParams(int id, String name) {
           System.out.println("带参数的方法:" + "ID:" + id + "name:" + name);
       }
   }
   ```

### 6、测试

1. 示例代码

   

   ```java
   ApplicationContext context = new ClassPathXmlApplicationContext("spring-aspect.xml");
   AspectDao dao = (AspectDao) context.getBean("aspectDao");
   dao.test();
   dao.testParams(1,"hello");
   ```

### 7、总结

1. 使用@Aspect将POJO声明为切面；
2. 在切面类中使用@Pointcut进行命名切入点声明；
3. 定义通知方法,使用5中注解声明，其中value用于定义切入点表达式或引用命名切入点；
4. 配置文件需要使用`<aop:aspectj-autoproxy/`来开启注解风格的@AspectJ支持；
5. 将切面类和POJO类注册到Spring容器中

## 七、基于xml的AOP编程(掌握)

### 1、说明

如果比较喜欢使用XML格式，Spring2.0也提供了使用新的"aop"命名空间来定义一个切面。 和使用@AspectJ风格完全一样，切入点表达式和通知类型同样得到了支持

| AOP配置元素              | 用途                                                         |
| ------------------------ | ------------------------------------------------------------ |
| `<aop:config`            | 顶层的AOP配置元素,大多数的`<aop:*`必须包含在`<aop:config`元素内 |
| `<aop:aspect`            | 定义一个切面                                                 |
| `<aop:pointcut`          | 定义一个切点                                                 |
| `<aop:advisor`           | 定义AOP通知器                                                |
| `<aop:before`            | 定义AOP前置通知                                              |
| `<aop:around`            | 定义AOP环绕通知                                              |
| `<aop:after-returning`   | 定义AOP返回通知                                              |
| `<aop:after-throwing`    | 定义AOP异常通知                                              |
| `<aop:after`             | 定义AOP后置通知（不管被通知的方法是否执行成功）              |
| `<aop:aspectj-autoproxy` | 启用@Aspect注解的切面                                        |
| `<aop:declare-parents`   | 以透明的方式为被通知的对象引入额外的接口                     |

### 2、引入aop命名空间标签

1. 说明

   在beans元素下 引入aop，声明`<aop-config`，在配置文件中，我们可以声明多个`<aop-config`。

   注意:

   - 所有的切面和通知都必须定义在`<aop:config`元素内部。
   - 一个`<aop:config`可以包含pointcut，advisor和aspect元素 （注意这三个元素必须按照这个顺序进行声明）

2. 示例代码

   

   ```xml
   <beans>
          ...
          xmlns:aop="http://www.springframework.org/schema/aop"
        ...
      <aop:config>
      </aop:config>
   </beans>
   ```

### 3、声明一个切面

1. 说明

   切面使用`<aop:aspect`来声明

2. 示例代码

   

   ```xml
   <aop:config>
     <aop:aspect id="myAspect" ref="myBean">
       ...
     </aop:aspect>
   </aop:config>
   
   <bean id="myBean" class="...">
     ...
   </bean>
   ```

### 4、声明一个切入点

1. 说明

   一个命名切入点可以在`<aop:config`元素中定义，使用`<aop:pointcut`声明，这样多个切面和通知就可以共享该切入点，你也可以在切面中定义

2. 示例代码

   

   ```xml
   <aop:config>
     <aop:pointcut id="servicePointcut" 
           expression="execution（* *.*（..））"/>
   </aop:config>
   ```

   

   ```xml
   <aop:config>
     <aop:aspect id="myAspect" ref="myBean">
         <!--这个切入点只能在该 切面中使用  -->
        <aop:pointcut id="servicePointcut" 
           expression="execution（* *.*（..））"/>
     </aop:aspect>
   </aop:config>
   ```

### 5、声明通知

#### 5.1、说明

和@AspectJ风格一样，基于xml的风格也支持5种通知类型并且两者具有同样的语义

#### 5.2、前置通知

1. 说明

   前置通知在匹配方法执行前运行。在`<aop:aspect>`中使用`<aop:before>` 元素来声明它

2. 示例代码

   

   ```xml
   <aop:config>
     <aop:pointcut id="servicePointcut" 
           expression="execution（* *.*（..））"/>
   
     <aop:aspect id="beforeExample" ref="myBean">
        <aop:before 
             pointcut-ref="servicePointcut" 
             method="doBefore"/>
    </aop:aspect>
   </aop:config
   ```

#### 5.3、后置通知

1. 说明

   后置通知在匹配的方法完全执行后运行。和前置通知一样，在`<aop:aspect>` 里面使用`<aop:after-returning>`声明，通知方法可以得到返回值。使用returning属性来指定传递返回值的参数名。

2. 示例代码

   

   ```xml
   <aop:aspect id="afterReturningExample" ref="myBean">
       <aop:after-returning 
         pointcut-ref="servicePointcut" 
         method="doAfterReturning"/>
       ...
   </aop:aspect>
   ```

   

   ```xml
   <aop:aspect id="afterReturningExample" ref="myBean">
       <aop:after-returning 
         pointcut-ref="servicePointcut" 
         method="doAfterReturning"/>
       ...
   </aop:aspect>
   ```



#### 5.4、异常通知

1. 说明

   异常通知在匹配方法抛出异常退出时执行。在`<aop:aspect`中使用 `<after-throwing`元素来声明，还可以使用throwing属性来指定传递异常的参数名

2. 示例代码

   

   ```xml
   <!-- 无返回值 -->
   <aop:aspect id="afterThrowingExample" ref="myBean">
       <aop:after-throwing
         pointcut-ref="servicePointcut" 
         throwing="exception" 
         method="doAfterThrowing"/>
       ...
   </aop:aspect>
   ```

#### 5.5、最终通知

1. 说明

   最终通知无论如何都会在匹配方法退出后执行。在`<aop:aspect`中使用`<aop:after`元素来声明

2. 示例代码

   

   ```xml
   <aop:aspect id="afterFinallyExample" ref="myBean"
       <aop:after
         pointcut-ref="servicePointcut" 
         method="doAfter"/
       ...
   </aop:aspect
   ```

#### 5.6、环绕通知

1. 说明

   环绕通知在匹配方法运行期的“周围”执行。 它有机会在目标方法的前面和后面执行，并决定什么时候运行，怎么运行，甚至是否运行。环绕通知经常在需要在一个方法执行前后共享状态信息，并且是在线程安全的情况下使用

2. 示例代码

   

   ```xml
   <aop:aspect id="aroundExample" ref="myBean"
       <aop:around
         pointcut-ref="servicePointcut" 
         method="doAround"/
       ...
   </aop:aspect
   ```

## 八、切面的优先级

### 1、说明

在同一个连接点上应用不止一个切面时, 除非明确指定, 否则它们的优先级是不确定的

- 切面的优先级可以通过实现 Ordered 接口或利用 @Order 注解指定.
  实现 Ordered 接口, getOrder() 方法的返回值越小, 优先级越高.
- 若使用 @Order 注解, 序号出现在注解中，值越小优先级越高

### 2、示例代码

1. 基于实现接口(了解)

   

   ```java
   @Aspect
   @Component
   public class LoggingAspect implements Ordered {
       @Override
       public int getOrder() {
           return 2;
       }
   }
   ```

   

   ```java
   @Aspect
   @Component
   public class ValidateAspect implements Ordered {
       @Override
       public int getOrder() {
           return 1;
       }
   }
   ```

2. 基于注解

   

   ```java
   @Order(2)
   @Aspect
   @Component
   public class LoggingAspect {
       
   }
   ```

   

   ```java
   @Order(1)
   @Aspect
   @Component
   public class ValidateAspect {
       
   }
   ```

## 九、简单总结

1. 切面的内容可以复用
2. 避免使用Proxy、CGLIB生成代理，这方面的工作全部框架去实现，开发者可以专注于切面内容本身
3. 代码与代码之间没有耦合，如果拦截的方法有变化修改配置文件即可

## 十、附录

### 1、获取目标对象信息

#### 1.1、JoinPoint 对象

1. 说明

   **JoinPoint**对象**封装了SpringAop中切面方法的信息**,在切面方法中添加**JoinPoint参数**,就可以获取到封装了该方法信息的**JoinPoint对象**

2. 重要方法

   | 方法                      | 说明                                                         |
   | ------------------------- | ------------------------------------------------------------ |
   | Signature getSignature(); | **获取封装了署名信息的对象,在该对象中可以获取到目标方法名,所属类的Class等信息** |
   | Object[] getArgs();       | **获取连接点方法运行时的入参列表**                           |
   | Object getTarget();       | **获取连接点所在的目标对象**                                 |
   | Object getThis();         | **获取代理对象本身**                                         |

#### 1.2、ProceedingJoinPoint

1. 说明

   ProceedingJoinPoint继承JoinPoint子接口，并且只能用于**@Around**的切面方法中

2. 新增方法

   | 方法名                                         | 功能                             |
   | ---------------------------------------------- | -------------------------------- |
   | Object proceed() throws Throwable              | **执行目标方法**                 |
   | Object proceed(Object[] var1) throws Throwable | **传入的新的参数去执行目标方法** |

### 2、示例代码

1. 案例1

   

   ```java
   @Aspect
   @Component
   public class JoinPointerAspect {
       /**
        * 定义一个切入点表达式,用来确定哪些类需要代理
        */
       @Pointcut("execution(* com.wener.example.aop.aspect.*.*(..))") 
       public void declareJoinPointer() {}
       /**
        * 前置方法,在目标方法执行前执行
        * @param joinPoint 封装了代理方法信息的对象,若用不到则可以忽略不写
        */
       @Before("declareJoinPointer()")
       public void beforeMethod(JoinPoint joinPoint){
           System.out.println("目标方法名:" + joinPoint.getSignature().getName());
           System.out.println("目标方法所属类的名:" +        joinPoint.getSignature().getDeclaringType().getSimpleName());
           System.out.println("目标方法声明类型:" + Modifier.toString(joinPoint.getSignature().getModifiers()));
           //获取传入目标方法的参数
           Object[] args = joinPoint.getArgs();
           for (int i = 0; i < args.length; i++) {
               System.out.println("第" + (i+1) + "个参数为:" + args[i]);
           }
           System.out.println("被代理的对象:" + joinPoint.getTarget());
           System.out.println("代理对象自己:" + joinPoint.getThis());
       }
       /**
        * 环绕方法,可自定义目标方法执行的时机
        * @param pjd JoinPoint的子接口,添加了
        *  Object proceed() throws Throwable 执行目标方法
        *  Object proceed(Object[] var1) throws Throwable 传入的新的参数去执行目标方法
        *         
        * @return 此方法需要返回值,返回值视为目标方法的返回值
        */
       @Around("declareJoinPointer()")
       public Object aroundMethod(ProceedingJoinPoint pjd){
           Object result = null;
           try {
               //前置通知
               System.out.println("目标方法执行前...");
               //执行目标方法
               //result = pjd.proeed();
               //用新的参数值执行目标方法
               result = pjd.proceed(new Object[]{"hello","world"});
               //返回通知
               System.out.println("目标方法返回结果后...");
           } catch (Throwable e) {
               //异常通知
               System.out.println("执行目标方法异常后...");
               throw new RuntimeException(e);
           }
           //后置通知
           System.out.println("目标方法执行后...");
           return result;
       }
   }
   ```

2. 执行流程

   - AOP定义了一个切面（Aspect），一个切面包含了切入点，通知，引入，这个切面上定义了许多的切入点(Pointcut)，一旦访问过程中有对象的方法跟切入点匹配那么就会被AOP拦截。
   - 此时该对象就是目标对象（Target Object）而匹配的方法就是连接点（Join Point）。
   - 紧接着AOP会用过JDK动态代理或者CGLIB生成一个目标对象的代理对象(AOP proxy)，这个过程就是织入（Weaving）。
   - 这个时候我们就可以按照我们的需求对连接点进行一些拦截处理。
   - 可以看到，我们可以引入（Introduction）一个新的接口，让代理对象来实现这个接口来，以实现额外的方法和字段。也可以在连接点上进行通知（Advice），通知的类型包括了前置通知，返回后通知，抛出异常后通知，后置通知，环绕通知。
   - 最后也是最骚的是整个过程不会改变代码原有的逻辑