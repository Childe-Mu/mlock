package moon.mlock.utils;

/**
 * 切面工具类
 *
 * @author moon
 */
public class AspectUtils {

    /**
     * 获取方法名称和参数字符串
     * <p>
     * eg: methodLongName = public static java.lang.String moon.mlock.utils.getMethodNameAndParams(java.lang.String)
     * <p>
     * 返回 getMethodNameAndParams(java.lang.String)
     *
     * @param methodLongName 方法全量签名，例如  public static java.lang.String moon.mlock.utils.getMethodNameAndParams(java.lang.String)
     * @return 方法名称和参数字符串
     */
    public static String getMethodNameAndParams(String methodLongName) {
        // 左右括号
        int left = methodLongName.indexOf("(");
        int right = methodLongName.indexOf(")");
        // 方法前面最后一个 . 位置
        int lastDotBeforeMethod = 0;
        for (int i = left; i >= 0; i--) {
            if (methodLongName.charAt(i) == '.') {
                lastDotBeforeMethod = i;
                break;
            }
        }
        return methodLongName.substring(lastDotBeforeMethod + 1, right + 1);
    }
}
