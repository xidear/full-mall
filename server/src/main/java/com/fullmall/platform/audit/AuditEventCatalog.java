package com.fullmall.platform.audit;

import java.util.List;
import java.util.Map;

/**
 * 审计事件码 → 中文标签（唯一来源）。
 *
 * <p>为什么必须有这张表：审计表里存的是 `EXPORT_JOB_CREATED` 这类内部码，直接摆到界面上
 * 运营根本看不懂；更糟的是筛选框让用户"手填事件类型"，等于要求运营背下这些常量。
 *
 * <p>新增审计事件时：在这里补一行标签即可（漏了也不会丢数据——接口会把库里有、表里没有的码
 * 以"未知事件"列出，前端仍可选、可读）。
 */
public final class AuditEventCatalog {

    public record Entry(String code, String label, String group) {
    }

    private record Def(String code, String label, String group) {
    }

    private static final List<Def> DEFS = List.of(
            new Def("LOGIN_SUCCESS", "登录成功", "认证"),
            new Def("STAFF_CREATED", "新建员工", "员工与权限"),
            new Def("STAFF_UPDATED", "修改员工", "员工与权限"),
            new Def("STAFF_STATUS_CHANGED", "启用/停用员工", "员工与权限"),
            new Def("STAFF_PASSWORD_RESET", "重置员工密码", "员工与权限"),
            new Def("PERMISSION_CHANGED", "修改角色权限", "员工与权限"),
            new Def("PAYMENT_SUCCEEDED", "支付成功", "订单与支付"),
            new Def("PAYMENT_REFUNDED", "支付退款", "订单与支付"),
            new Def("ORDER_CANCELLED_BY_SYSTEM", "系统取消订单", "订单与支付"),
            new Def("ORDER_REFUND_REQUESTED", "顾客申请退款", "订单与支付"),
            new Def("POS_REFUND", "收银台退款", "订单与支付"),
            new Def("COUPON_REDEEMED", "优惠券核销", "营销"),
            new Def("STOCK_ADJUSTED", "库存调整", "库存与仓储"),
            new Def("STOCK_ADJUST_APPROVED", "库存调整审批", "库存与仓储"),
            new Def("STOCK_TRANSFER_SHIPPED", "调拨出库", "库存与仓储"),
            new Def("SETTLEMENT_GENERATED", "生成结算单", "结算"),
            new Def("EXPORT_JOB_CREATED", "创建导出任务", "导出"),
            new Def("FEATURE_FLAG_CHANGED", "修改功能开关", "系统"),
            new Def("TRANSLATION_DRAFT_SAVED", "保存译文草稿", "翻译"),
            new Def("TRANSLATION_REVIEWED", "审核译文", "翻译"),
            new Def("TRANSLATION_PUBLISHED", "发布译文", "翻译"),
            new Def("TRANSLATION_OUTDATED", "译文已过期", "翻译"),
            new Def("TRANSLATION_AUTO_TRANSLATED", "自动翻译（标题/描述）", "翻译"),
            new Def("TRANSLATION_DETAIL_TRANSLATED", "自动翻译（富文本详情）", "翻译"));

    private static final Map<String, Def> BY_CODE = DEFS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Def::code, def -> def));

    private AuditEventCatalog() {
    }

    public static String label(String code) {
        Def def = BY_CODE.get(code);
        return def == null ? code : def.label();
    }

    /** 已知事件（供前端下拉）：按分组、再按码排序。 */
    public static List<Entry> entries() {
        return DEFS.stream()
                .sorted(java.util.Comparator.comparing(Def::group).thenComparing(Def::code))
                .map(def -> new Entry(def.code(), def.label(), def.group()))
                .toList();
    }

    /** 库里出现过但目录里没有的码：给一个可读标签，避免前端显示空白。 */
    public static Entry unknown(String code) {
        return new Entry(code, code, "未登记");
    }
}
