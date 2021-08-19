package moon.mlock.common.enums;

/**
 * 公共枚举方法接口
 *
 * @author moon
 */
public interface EnumValue {
    /**
     * 获取枚举的index
     *
     * @return 枚举的index
     */
    Integer getIndex();

    /**
     * 获取枚举的name
     *
     * @return 枚举的name
     */
    String getName();

    /**
     * 根据枚举index获取枚举
     *
     * @param index 枚举index
     * @return 枚举
     */
    EnumValue getEnumByIndex(Integer index);
}
