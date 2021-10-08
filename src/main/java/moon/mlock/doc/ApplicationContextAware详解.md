# ApplicationContextAware详解

## 一、进入正题

Aware本义就是"自动的"，顾名思义spring给我们自动做了些事情。spring有很多以Aware结尾的类，有EnvironmentAware、ApplicationContextAware、MessageSourceAware等。

这里我主要讲一下ApplicationContextAware。

如下文引用，ApplicationContextAware的文档可以阅读 [Spring Core Technologies 1.4.6 Method Injection](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-factory-method-injection) 、[Spring Core Technologies 1.6.2. `ApplicationContextAware` and `BeanNameAware`](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-factory-aware)、[方法注入](https://spring.io/blog/2004/08/06/method-injection)

概括一下，就是：

> 在大多数应用程序场景中，容器中的大多数bean都是单例的。当一个单例bean需要与另一个单例bean协作，或者一个非单例bean需要与另一个非单例bean协作时，通常通过将一个bean定义为另一个bean的属性来处理依赖关系。当bean的生命周期不同时，就会出现问题。假设单例bean A需要使用非单例(原型)bean B，可能是在A的每个方法调用上。容器只创建单例bean A一次，因此只有一次机会设置属性。容器不能在每次需要bean A时都向bean A提供一个新的bean B实例。
>
> 一个解决方案是`放弃一些控制反转`。您可以通过实现ApplicationContextAware接口，以及在bean A每次需要bean B实例时对容器进行getBean(“B”)调用，从而使bean A `aware(自动获取到)` 容器。



## 二、使用

```java
@Service("gatewayService")
public class GatewayServiceImpl implements IGatewayService，ApplicationContextAware {

    Map<ServiceBeanEnum，IGatewayBo> chargeHandlerMap=new HashMap<ServiceBeanEnum，IGatewayBo>();

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }
}
```

在我们需要使用ApplicationContext的服务中实现ApplicationContextAware接口，系统启动时就可以自动给我们的服务注入applicationContext对象，我们就可以获取到ApplicationContext里的所有信息了。

## 三、原理分析

我们都知道spring的入口方法就在AbstractApplicationContext的refresh()方法，我们先去看看refresh().prepareBeanFactory()方法。

```java
protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // Tell the internal bean factory to use the context's class loader etc.
        beanFactory.setBeanClassLoader(getClassLoader());
        beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
        beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this， getEnvironment()));

        // 添加ApplicationContextAware的处理器
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
        beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
        beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
        beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
        beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
                ...
    }
```

也就是说，spring在启动的时候给我们添加了ApplicationContextAwareProcessor这样一个processor，进去看看它的实现:

```tsx
@Override
public Object postProcessBeforeInitialization(final Object bean， String beanName) throws BeansException {
    AccessControlContext acc = null;

    if (System.getSecurityManager() != null &&
        (bean instanceof EnvironmentAware || bean instanceof EmbeddedValueResolverAware ||
         bean instanceof ResourceLoaderAware || bean instanceof ApplicationEventPublisherAware ||
         bean instanceof MessageSourceAware || bean instanceof ApplicationContextAware)) {
        acc = this.applicationContext.getBeanFactory().getAccessControlContext();
    }

    if (acc != null) {
        AccessController.doPrivileged((PrivilegedAction)() -> {
            //核心方法，调用aware接口方法
            invokeAwareInterfaces(bean);
            return null;
        }， acc);
    } else {
        invokeAwareInterfaces(bean);
    }

    return bean;
}

//实现
private void invokeAwareInterfaces(Object bean) {
    if (bean instanceof Aware) {
        if (bean instanceof EnvironmentAware) {
            ((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());
        }
        if (bean instanceof EmbeddedValueResolverAware) {
            ((EmbeddedValueResolverAware) bean).setEmbeddedValueResolver(this.embeddedValueResolver);
        }
        if (bean instanceof ResourceLoaderAware) {
            ((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
        }
        if (bean instanceof ApplicationEventPublisherAware) {
            ((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);
        }
        if (bean instanceof MessageSourceAware) {
            ((MessageSourceAware) bean).setMessageSource(this.applicationContext);
        }
        //针对实现了ApplicationContextAware的接口，spring都将调用其setApplicationContext，将applicationContext注入到当前bean对象。
        if (bean instanceof ApplicationContextAware) {
            ((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
        }
    }
}
```

那ApplicationContextAwareProcessor又是什么时候调用的呢？我们接着往下看，原来refresh()方法中还有个beanFactory.preInstantiateSingletons()方法，里面有这样一段代码:

```dart
拿到所有的beanNames，然后依次判断是否需要加载，如果是，则调用getBean(beanName)方法实例化出来。
// Trigger initialization of all non-lazy singleton beans...
    for (String beanName : beanNames) {
        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            if (isFactoryBean(beanName)) {
                final FactoryBean<?> factory = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
                boolean isEagerInit;
                if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                    isEagerInit = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            return ((SmartFactoryBean<?>) factory).isEagerInit();
                        }
                    }， getAccessControlContext());
                }
                else {
                    isEagerInit = (factory instanceof SmartFactoryBean &&
                                   ((SmartFactoryBean<?>) factory).isEagerInit());
                }
                if (isEagerInit) {
                    getBean(beanName);
                }
            }
            else {
                getBean(beanName);
            }
        }
    }
```

依次查看getBean() ->doGetBean()->createBean()->doCreateBean()方法:

```jsx
// Initialize the bean instance.
Object exposedObject = bean;
try {
    populateBean(beanName， mbd， instanceWrapper);
    if (exposedObject != null) {
        exposedObject = initializeBean(beanName， exposedObject， mbd);
    }
} catch (Throwable ex) {
    if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
        throw (BeanCreationException) ex;
    } else {
        throw new BeanCreationException(
            mbd.getResourceDescription()， beanName， "Initialization of bean failed"， ex);
    }
}
```

查看一下initializeBean方法:

```java
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
    if (System.getSecurityManager() != null) {
        AccessController.doPrivileged(() -> {
            this.invokeAwareMethods(beanName, bean);
            return null;
        }, this.getAccessControlContext());
    } else {
        //调用setBeanName() 、setBeanClassLoaderAware、setBeanFactoryAware方法
        this.invokeAwareMethods(beanName, bean);
    }

    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = this.applyBeanPostProcessorsBeforeInitialization(bean, beanName);
    }

    try {
        //调用afterPropertiesSet()方法
        this.invokeInitMethods(beanName, wrappedBean, mbd);
    } catch (Throwable var6) {
        throw new BeanCreationException(mbd != null ? mbd.getResourceDescription() : null, beanName, "Invocation of init method failed", var6);
    }
	//这里才是ApplicationContextProcessor的postProcessAfterInitialization()执行入口:
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = this.applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}
```

原来AbstractAutowireCapableBeanFactory中的inititalBean()方法就是BeanPostProcessor的调用处。但是像BeanNameAware、BeanFactoryAware不同，是通过initialBean()中的invokeAwareMethods直接调用实现的

## 四、样例

```java
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * 通过Spring上下文获取bean工具类
 *
 * @author moon
 */
public class SpringContextUtils implements ApplicationContextAware {

    /**
     * Spring应用上下文环境
     */
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringContextUtils.initSpringContext(applicationContext);
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return (T) applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public static boolean isSingleton(String name) {
        return applicationContext.isSingleton(name);
    }

    /**
     * 根据class对象返回IOC容器中其对象和其子类的对象。
     * 未找到则返回空MAP。
     * KEY为BEAN ID或者NAME，VALUE为BEAN实例
     *
     * @param type 需要找的bean类型的CLASS对象
     * @return bean映射
     */
    public static <T> Map<String, T> getBeansByType(Class<T> type) {
        return BeanFactoryUtils.beansOfTypeIncludingAncestors(SpringContextUtils.getApplicationContext(), type);
    }

    /**
     * 初始化ApplicationContext
     *
     * @param applicationContext 上下文
     */
    public static void initSpringContext(ApplicationContext applicationContext) {
        SpringContextUtils.applicationContext = applicationContext;
    }

    /**
     * 获取业务线（业务线配置在配置文件中）
     *
     * @return 业务线
     */
    public static String getProjectBusinessLine() {
        if (applicationContext == null) {
            throw new RuntimeException("spring初始化失败");
        }
        return applicationContext.getEnvironment().getProperty("***.application.businessLine");
    }

    /**
     * 获取项目名称（项目名称配置在配置文件中）
     *
     * @return 项目名称
     */
    public static String getProjectName() {
        if (applicationContext == null) {
            throw new RuntimeException("spring初始化失败");
        }
        return applicationContext.getEnvironment().getProperty("***.application.name");
    }
}

```



> 作者：jerrik
> 链接：spring(1)-ApplicationContextAware详解