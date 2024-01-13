package org.dromara.common.core.constant;

/**
 * 正则表达式
 *
 * @author Feng
 */
public interface RegexConstants {

    /**
     * 中文字符正则表达式
     */
    public static final String CHINESE_REGEX = "[\\u4e00-\\u9fa5]+";

    /**
     * 姓名（2-4个中文字符正则）
     */
    public static final String NAME_REGEX = "^[\u4e00-\u9fa5]{2,4}$";

    /**
     * 匹配中国大陆手机号码
     */
    public static final String PHONE_NUMBER_REGEX = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";

    /**
     * 座机号码
     */
    public static final String LANDLINE_REGEX = "^(0\\d{2,3})-?(\\d{7,8})$";

    /**
     * 电子邮箱
     */
    public static final String EMAIL_REGEX = "^[\\w+&*-]+(?:\\.[\\w+&*-]+)*@(?:[\\w+&*-]+\\.)+[a-zA-Z]{2,7}$";

    /**
     * 身份证号码（普通校验）
     */
    public static final String ID_CARD_REGEX_GENERAL = "(^\\d{15}$)|(^\\d{18}$)|(^\\d{17}(\\d|X|x)$)";

    /**
     * 身份证号码（精准校验 - 18位）
     */
    public static final String ID_CARD_REGEX_ACCURATE_18 = "^[1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";

    /**
     * 身份证号码（15位）
     */
    public static final String ID_CARD_REGEX_ACCURATE_15 = "^[1-9]\\d{5}\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{2}[0-9Xx]$";

    /**
     * 身份证号码（后6位）
     */
    public static final String ID_CARD_REGEX_LAST_6 = "^(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";

    /**
     * QQ号码
     */
    public static final String QQ_NUMBER_REGEX = "^[1-9][0-9]\\d{4,9}$";

    /**
     * 邮政编码
     */
    public static final String POSTAL_CODE_REGEX = "^[1-9]\\d{5}$";

    /**
     * 注册账号
     */
    public static final String ACCOUNT_REGEX = "^[a-zA-Z][a-zA-Z0-9_]{4,15}$";

    /**
     * 密码：包含至少8个字符，包括大写字母、小写字母、数字和特殊字符
     */
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    /**
     * 通用状态（0表示正常，1表示停用）
     */
    public static final String STATUS_REGEX = "^[01]$";

    /**
     * 字典类型必须以字母开头，且只能为（小写字母，数字，下滑线）
     */
    public static final String DICTIONARY_TYPE_REGEX = "^[a-z][a-z0-9_]*$";

}
