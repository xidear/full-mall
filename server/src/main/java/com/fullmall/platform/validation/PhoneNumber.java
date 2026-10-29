package com.fullmall.platform.validation;

/**
 * 手机号/联系电话的唯一后端规则。
 *
 * <p>前端 `useRegionPolicy`、会员 OTP、地址、结算都必须与这里保持一致；
 * 数据库只保证非空/唯一，不做格式校验（格式属于业务规则）。
 */
public final class PhoneNumber {

    // 透传 ValidationRules.PHONE_PATTERN 不复制正则：抄一份的后果是改了 schema 漏改这里，前后端判定不一致。
    /** 可带国际区号；6–20 位数字。与 storefront/mobile 的 phonePattern 一致。 */
    public static final String PATTERN = ValidationRules.PHONE_PATTERN;

    private PhoneNumber() {
    }

    /**
     * 校验手机号格式：先 trim 再全串匹配，中间带空格或横线（{@code 138 0013 8000}）判为非法，{@code null} 为 false。
     * 只判格式，不判号码是否真实存在（那要调短信服务商，不能阻塞下单）。
     */
    public static boolean isValid(String value) {
        return value != null && value.trim().matches(PATTERN);
    }

    /**
     * 归一化：只 trim。刻意不删横线/空格、不补国家码——改写用户输入会让同一号码以多种形式落库，
     * 变成两个会员账号或无法合并的联系人。
     */
    public static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
