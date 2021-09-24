package moon.mlock.utils;

import java.util.UUID;

/**
 * UUIDUtils
 * @author moon
 */
public class UUIDUtils {
    /**
     * 获取一个UUID
     *
     * @return UUID
     */
    public static String getUuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
