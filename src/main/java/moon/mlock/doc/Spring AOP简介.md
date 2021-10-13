# Spring AOP简介



## 1. 定义

AOP是Aspect Oriented Programming，即面向切面编程。

那什么是AOP？

我们先回顾一下OOP：Object Oriented Programming，OOP作为面向对象编程的模式，获得了巨大的成功，OOP的主要功能是数据封装、继承和多态。

而AOP是一种新的编程方式，它和OOP不同，OOP把系统看作多个对象的交互，AOP把系统分解为不同的关注点，或者称之为切面（Aspect）。

AOP在程序开发中主要用来解决一些系统层面上的问题，比如日志，事务，权限等待，Struts2的拦截器设计就是基于AOP的思想，是个比较经典的例子。

## 2.AOP原理

AOP需要解决的问题是，如何把切面织入到核心逻辑中，如何对调用方法进行拦截，并在拦截前后进行安全检查、日志、事务等处理，就相当于完成了所有业务功能。

在Java平台上，对于AOP的织入，有3种方式：

1. 编译期：在编译时，由编译器把切面调用编译进字节码，这种方式需要定义新的关键字并扩展编译器，AspectJ就扩展了Java编译器，使用关键字aspect来实现织入；
2. 类加载器：在目标类被装载到JVM时，通过一个特殊的类加载器，对目标类的字节码重新“增强”；
3. 运行期：目标对象和切面都是普通Java类，通过JVM的动态代理功能或者第三方库实现运行期动态织入。

最简单的方式是第三种，Spring的AOP实现就是基于JVM的动态代理。由于JVM的动态代理要求必须实现接口，如果一个普通类没有业务接口，就需要通过[CGLIB](https://github.com/cglib/cglib)或者[Javassist](https://www.javassist.org/)这些第三方库实现。

AOP技术看上去比较神秘，但实际上，它本质就是一个动态代理，让我们把一些常用功能如权限检查、日志、事务等，从每个业务方法中剥离出来。

需要特别指出的是，AOP对于解决特定问题，例如事务管理非常有用，这是因为分散在各处的事务代码几乎是完全相同的，并且它们需要的参数（JDBC的Connection）也是固定的。另一些特定问题，如日志，就不那么容易实现，因为日志虽然简单，但打印日志的时候，经常需要捕获局部变量，如果使用AOP实现日志，我们只能输出固定格式的日志，因此，使用AOP时，必须适合特定的场景。

## 3. 语法

#### 3.1 基本概念

- **Aspect**：切面，即一个横跨多个核心逻辑的功能，或者称之为系统关注点；
- **Joinpoin**t：连接点，即定义在应用程序流程的何处插入切面的执行；
- **Pointcut**：切入点，即一组连接点的集合；
- **Advice**：增强，指特定连接点上执行的动作；
- **Introduction**：引介，指为一个已有的Java对象动态地增加新的接口；
- **Weaving**：织入，指将切面整合到程序的执行流程中；
- **Interceptor**：拦截器，是一种实现增强的方式；
- **Target Object**：目标对象，即真正执行业务的核心逻辑对象；
- **AOP Proxy**：AOP代理，是客户端持有的增强后的对象引用，Spring中的AOP代理可以使JDK动态代理，也可以是CGLIB代理，前者基于接口，后者基于子类

#### 3.2 通知方法

- **前置通知（@Before）**
	- 在目标方法被调用之前做增强处理,@Before只需要指定切入点表达式即可 
- **后置通知（@After）**
	- 在目标方法完成之后做增强，无论目标方法时候成功完成。@After可以指定一个切入点表达式
- **返回通知 （@AfterReturning）**
	- 在目标方法正常完成后做增强,@AfterReturning除了指定切入点表达式后，还可以指定一个返回值形参名returning,代表目标方法的返回值
- **异常通知 （@AfterThrowing）**
	- 主要用来处理程序中未处理的异常,@AfterThrowing除了指定切入点表达式后，还可以指定一个throwing的返回值形参名,可以通过该形参名来访问目标方法中所抛出的异常对象
- **环绕通知 （@Around）**
	- 环绕通知,在目标方法完成前后做增强处理,环绕通知是最重要的通知类型,像事务,日志等都是环绕通知,注意编程中核心是一个ProceedingJoinPoint

#### 3.3  启用方式

1. xml配置方式，在applicationContext.xml中配置下面一句:
    ```xml
    <aop:aspectj-autoproxy />
    ```
2. 注解方式，在启动类上加上`@EnableAspectJAutoProxy`
	``` java
	@EnableAspectJAutoProxy
	@SpringBootApplication
    public class Application {
        public static void main(String[] args) {
            //do something
        }
    }
	```

## 4.实例

#### 4.1 定义目标类

``` java
public class MathCalculator {
    public int div(int x, int y) {
        System.out.println(x / y);
        return x / y;
    }
}
```

#### 4.2 定义切面类,并指定通知方法

```java
@Aspect
public class LogAspects {
    @Pointcut("execution(int com.test.tornesol.util.spring.spring_aop.MathCalculator.div(int,int))")
    public void pointCut() { }

    @Before("com.test.tornesol.util.spring.spring_aop.LogAspects.pointCut()")
    public void logStart(JoinPoint joinPoint) {
        System.out.println(joinPoint.getSignature().getName() + " 除法运行,参数是：" + Arrays.asList(joinPoint.getArgs()));
    }

    @After("com.test.tornesol.util.spring.spring_aop.LogAspects.pointCut()")
    public void logEnd() {
        System.out.println("除法结束");
    }


    @AfterReturning(value = "com.test.tornesol.util.spring.spring_aop.LogAspects.pointCut())", returning = "result")
    public void logReturn2(JoinPoint joinPoint, Object result) {
        System.out.println(joinPoint.getSignature().getName() + "除法返回" + result);
    }

    @AfterThrowing(value = "com.test.tornesol.util.spring.spring_aop.LogAspects.pointCut()", throwing = "exception")
    public void logException(Exception exception) {
        System.out.println("除法异常");
    }
}
```

> **Notes**： 注意给切面类添加@Aspect注解

#### 4.3 添加Configuration类，注入目标类和切面类，并开启AOP代理模式

```java
@Configuration
@EnableAspectJAutoProxy//开启基于注解的AOP模式
public class MainConfig {
    @Bean
    public MathCalculator mathCalculator() {
        return new MathCalculator();
    }
    @Bean
    public LogAspects logAspects() {
        return new LogAspects();
    }
}
```

#### 4.4 测试 输出

```java
public class AopDemo {
    static public void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig.class);
        context.getBean(MathCalculator.class).div(4, 2);
    }
}
```

```css
div 除法运行,参数是：[4, 2]
2
除法结束
div除法返回2
```

## 5. 参考文档

1. [使用AOP - 廖雪峰的官方网站 (liaoxuefeng.com)](https://www.liaoxuefeng.com/wiki/1252599548343744/1266265125480448)，廖雪峰的文档写的还不错，可以用来快速了解上手AOP。
2. [Aspect Oriented Programming With Spring](https://spring.getdocs.org/en-US/spring-framework-docs/docs/spring-core/aop/aop.html)，Spring的官方文档最权威详细