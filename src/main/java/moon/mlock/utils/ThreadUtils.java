package moon.mlock.utils;

/**
 * 线程工具类
 *
 * @author moon
 */
public class ThreadUtils {
    private ThreadUtils() {

    }

    /**
     * 获取当前线程名称
     *
     * @return 当前线程名称
     */
    public static String getThreadName() {
        return Thread.currentThread().getName();
    }
}
