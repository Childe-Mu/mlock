package moon.mlock.common.enums;

import java.util.Objects;

/**
 * 锁类型枚举
 *
 * @author moon
 */
public enum LockTypeEnum implements EnumValue {

    /**
     * 无锁
     */
    LOCK_NOTHING(1, "nothing"),

    /**
     * redis锁
     *
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     */
    LOCK_REDIS(2, "redis"),

    /**
     * 强制redis锁
     */
    LOCK_REDIS_FORCE(3, "redis_force"),
    ;

    /**
     * index
     */
    private final Integer index;

    /**
     * name
     */
    private final String name;

    LockTypeEnum(int index, String name) {
        this.index = index;
        this.name = name;
    }

    /**
     * 获取枚举的index
     *
     * @return 枚举的index
     */
    @Override
    public Integer getIndex() {
        return this.index;
    }

    /**
     * 获取枚举的name
     *
     * @return 枚举的name
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 根据index获取锁类型枚举
     *
     * @param index index
     * @return 锁类型枚举
     */
    public static LockTypeEnum getEnumByIndex(Integer index) {
        if (Objects.isNull(index)) {
            return null;
        }
        for (LockTypeEnum lockTypeEnum : LockTypeEnum.values()) {
            if (Objects.equals(index, lockTypeEnum.getIndex())) {
                return lockTypeEnum;
            }
        }
        return null;
    }
}
