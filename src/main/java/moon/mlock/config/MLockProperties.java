package moon.mlock.config;


import moon.mlock.common.enums.LockTypeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MLock配置()
 *
 * @author moon
 */
@SuppressWarnings("all")
@ConfigurationProperties
public class MLockProperties {

    /**
     * 项目名称
     */
    @Value("${moon.application.name}")
    private String applicationName;

    /**
     * MLock-锁类型
     */
    @Value("${${moon.application.name}.mLock.type:redis}")
    private Integer mLockType;

    /**
     * redis集群名称
     */
    @Value("${component.redis.cluster-name:moonCluster}")
    private String redisGroupName;

    /**
     * 获取MLock锁类型
     *
     * @return
     */
    public LockTypeEnum getMLockType() {
        return LockTypeEnum.getEnumByIndex(mLockType);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getRedisGroupName() {
        return redisGroupName;
    }

    public void setRedisGroupName(String redisGroupName) {
        this.redisGroupName = redisGroupName;
    }
}
