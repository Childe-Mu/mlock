package moon.mlock.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
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

    private LocalUtils() {
    }

    /**
     * 获取本机ip地址
     * 此方法为重量级的方法，不要频繁调用
     */
    public static String getLocalIp() {
        if (LOCAL_IP != null) {
            return LOCAL_IP;
        }
        try {
            //根据网卡取本机配置的IP
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            String ip = null;
            // 标签在Java中不常用，许多开发人员不了解它们是如何工作的。此外，它们的使用使控制流更难遵循，从而降低了代码的可读性。
            // 网上搬运的代码，第一次知道java原来还有标签这个功能，看来确实不常用
            // TODO 把这个修改一下，换掉标签
            a:
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    InetAddress ipObj = ips.nextElement();
                    if (ipObj.isSiteLocalAddress()) {
                        ip = ipObj.getHostAddress();
                        break a;
                    }
                }
            }
            return ip;
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }


    public static synchronized String getNextBatchID() {
        assert LOCAL_IP != null;
        return LOCAL_IP.replace(".", "") + getDateStringMillisecond(new Date()) + getRandNum();
    }


    //生成最大 五 的随机数 ,位数不够补零
    public static final int MAX_RANDOM_NUM = 99999;

    public static String getRandNum() {
        DecimalFormat df = new DecimalFormat("00000");
        return df.format(random.nextInt(MAX_RANDOM_NUM + 1));
    }

    public static String getDateStringMillisecond(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("MMddhhmmssSSS");
        return formatter.format(date);
    }

//    public static void main(String[] args) {
//        System.out.println(getRandNum());
//    }
}
