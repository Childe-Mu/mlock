package moon.mlock.utils;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * 通过Spring上下文获取bean工具类
 * <p>
 * 文档见 ApplicationContextAware详解.md
 *
 * @author moon
 */
public class SpringUtils implements ApplicationContextAware {

    /**
     * Spring应用上下文环境
     */
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringUtils.initSpringContext(applicationContext);
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
        return BeanFactoryUtils.beansOfTypeIncludingAncestors(SpringUtils.getApplicationContext(), type);
    }

    /**
     * 初始化ApplicationContext
     *
     * @param applicationContext 上下文
     */
    public static void initSpringContext(ApplicationContext applicationContext) {
        SpringUtils.applicationContext = applicationContext;
    }
}
