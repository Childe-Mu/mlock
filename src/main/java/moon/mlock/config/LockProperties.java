package moon.mlock.config;


import moon.mlock.common.enums.LockTypeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lock配置()
 *
 * @author moon
 */
@SuppressWarnings("all")
@ConfigurationProperties
public class LockProperties {

    /**
     * 项目名称
     */
    @Value("${moon.application.name}")
    private String applicationName;

    /**
     * ILock-锁类型
     */
    @Value("${${moon.application.name}.mLock.type:redis}")
    private Integer mLockType;

    /**
     * redis集群名称
     */
    @Value("${component.redis.cluster-name:moonCluster}")
    private String redisGroupName;

    /**
     * 获取Lock锁类型
     *
     * @return
     */
    public LockTypeEnum getLockType() {
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
