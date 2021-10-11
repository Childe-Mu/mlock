package moon.mlock.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

/**
 * 本地资源工具类
 *
 * @author moon
 */
@Slf4j
public class LocalUtils {
    /**
     * 本地机器ip处理后的字符串
     */
    public static final String LOCAL_IP = getLocalIp();

    /**
     * 随机
     */
    public static final Random random = new Random();

    /**
     * 生成最大五位的随机数,位数不够补零
     */
    public static final int BOUND = (int) 1e6;

    private LocalUtils() {
    }

    /**
     * 获取本机ip地址
     * 此方法为重量级的方法，不要频繁调用
     */
    public static String getLocalIp() {
        if (StringUtils.isNotEmpty(LOCAL_IP)) {
            return LOCAL_IP;
        }
        try {
            // 根据网卡取本机配置的IP
            // 可以使用 getNetworkInterfaces()+getInetAddresses() 获取该节点的所有 IP 地址
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    InetAddress ip = ips.nextElement();
                    if (ip.isSiteLocalAddress()) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取本机ip地址，异常：", e);
        }
        return null;
    }

    /**
     * 获取下一个id
     *
     * @return 下一个id
     */
    public static synchronized String getNextBatchId() {
        assert LOCAL_IP != null;
        return LOCAL_IP.replace(".", "") + getCurrentTimeStr() + getRandNum();
    }


    /**
     * 获取五位随机值
     *
     * @return 随机值
     */
    public static String getRandNum() {
        return String.format("%05d", random.nextInt(BOUND));
    }

    /**
     * 获取当前时间戳字符串（格式：MMddhhmmssSSS）
     *
     * @return 当前时间戳字符串
     */
    public static String getCurrentTimeStr() {
        return new SimpleDateFormat("MMddhhmmssSSS").format(new Date());
    }
}
