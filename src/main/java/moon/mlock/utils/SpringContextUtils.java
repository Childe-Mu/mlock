package moon.mlock.utils;

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
    private transient static ApplicationContext applicationContext;

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

    public static <T> T getBean(Class<T> clz) {
        return applicationContext.getBean(clz);
    }

    /**
     * 是否是共享单例
     *
     * @param name 要查询的 bean 的名称
     * @return 此 bean 是否对应于单例实例
     */
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
     * 初始化applicaitonContext
     */
    public static void initSpringContext(ApplicationContext applicationContext) {
        SpringContextUtils.applicationContext = applicationContext;
    }

    public static String getProjectBusiLine() {
        if (applicationContext == null) {
            throw new RuntimeException("spring初始化失败");
        }
        return applicationContext.getEnvironment().getProperty("application.businessLine");
    }

    public static String getProjectName() {
        if (applicationContext == null) {
            throw new RuntimeException("spring初始化失败");
        }
        return applicationContext.getEnvironment().getProperty("application.name");
    }
}
