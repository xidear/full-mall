package com.fullmall.platform.meta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台界面用到的枚举字典（码 → 中文），单一来源。
 *
 * <p>为什么要有：接口和数据库里存的是 `PRODUCT`/`IMAGE`/`SUCCEEDED` 这类码，
 * 直接渲染到表格里运营看不懂；而各页面各写一份中文映射，又会随枚举扩展而走样。
 * 集中在这里维护，前端只按 group + code 取标签（见 web/apps/admin/src/composables/useEnums.ts）。
 *
 * <p>未登记的码不隐藏：前端拿不到标签时回退显示原始码，
 * 便于发现"后端加了枚举值但没登记"这种情况，而不是变成空白。
 */
public final class EnumCatalog {

    public record Option(String code, String label) {
    }

    private static final Map<String, List<Option>> GROUPS = new LinkedHashMap<>();

    static {
        GROUPS.put("ASSET_PURPOSE", List.of(
                new Option("PRODUCT", "商品图"),
                new Option("SPEC", "规格图"),
                new Option("VIDEO", "视频/封面"),
                new Option("AI_GENERATED", "AI 生成"),
                new Option("AI", "AI 生成（早期数据）"),
                new Option("IMPORT", "批量导入"),
                new Option("E2E", "自动化测试")));
        GROUPS.put("ASSET_KIND", List.of(
                new Option("IMAGE", "图片"),
                new Option("VIDEO", "视频"),
                new Option("TEXT", "文本"),
                new Option("FILE", "文件")));
        GROUPS.put("ASSET_STATUS", List.of(
                new Option("ACTIVE", "可用"),
                new Option("ONLINE", "已上线"),
                new Option("ARCHIVED", "已归档"),
                new Option("DELETED", "已删除")));
        GROUPS.put("AI_PROTOCOL", List.of(
                new Option("openai-chat", "OpenAI 兼容对话（/chat/completions）"),
                new Option("openai-embedding", "OpenAI 兼容向量（/embeddings）")));
        GROUPS.put("AI_JOB_STATUS", List.of(
                new Option("QUEUED", "排队中"),
                new Option("PENDING", "待处理"),
                new Option("RUNNING", "生成中"),
                new Option("PROCESSING", "处理中"),
                new Option("SUCCEEDED", "已完成"),
                new Option("FAILED", "失败"),
                new Option("CANCELLED", "已取消")));
        GROUPS.put("EXPORT_STATUS", List.of(
                new Option("PENDING", "排队中"),
                new Option("PROCESSING", "处理中"),
                new Option("SUCCEEDED", "已完成"),
                new Option("FAILED", "失败")));
        GROUPS.put("PROMOTION_STATUS", List.of(
                new Option("DRAFT", "草稿"),
                new Option("ACTIVE", "进行中"),
                new Option("PAUSED", "已暂停"),
                new Option("ENDED", "已结束")));
        GROUPS.put("PROMOTION_CHANNEL", List.of(
                new Option("ONLINE", "线上"),
                new Option("POS", "收银台"),
                new Option("PICKUP", "自提"),
                new Option("OFFLINE", "线下")));
        GROUPS.put("STAFF_STATUS", List.of(
                new Option("ACTIVE", "启用"),
                new Option("DISABLED", "停用")));
        GROUPS.put("SUPPLIER_STATUS", List.of(
                new Option("ACTIVE", "合作中"),
                new Option("PENDING", "待审核"),
                new Option("DISABLED", "已停用")));
        GROUPS.put("NOTIFICATION_STATUS", List.of(
                new Option("SENT", "已发送"),
                new Option("PENDING", "待发送"),
                new Option("FAILED", "发送失败")));
        GROUPS.put("MEDIA_PURPOSE", List.of(
                new Option("MAIN", "商品主图"),
                new Option("SPEC", "规格图"),
                new Option("DETAIL", "详情图"),
                new Option("VIDEO", "主图视频"),
                new Option("COPY", "详情文案")));
        GROUPS.put("MEDIA_ATTACH_ROLE", List.of(
                new Option("MAIN", "主图"),
                new Option("GALLERY", "图集"),
                new Option("DETAIL", "详情图"),
                new Option("VIDEO", "视频"),
                new Option("SPEC", "规格图")));
        GROUPS.put("COUPON_STATUS", List.of(
                new Option("DRAFT", "草稿"),
                new Option("ACTIVE", "发放中"),
                new Option("DISABLED", "已停用"),
                new Option("ENDED", "已结束")));
        GROUPS.put("COUPON_TYPE", List.of(
                new Option("AMOUNT", "满减券"),
                new Option("PERCENT", "折扣券")));
        GROUPS.put("PROMOTION_TYPE", List.of(
                new Option("FLASH_SALE", "秒杀"),
                new Option("BUY_GIFT", "买赠"),
                new Option("GROUP_BUY", "拼团"),
                new Option("FULL_REDUCTION", "满减"),
                new Option("FULL_DISCOUNT", "满折")));
        GROUPS.put("PROMOTION_SCOPE", List.of(
                new Option("ALL", "全场"),
                new Option("SKU", "指定商品"),
                new Option("CATEGORY", "指定类目"),
                new Option("BRAND", "指定品牌")));
        // 只列真的能发出去的通道：APP 推送还没实现，放进来等于给运营一个必失败的选项。
        GROUPS.put("NOTIFICATION_CHANNEL", List.of(
                new Option("IN_APP", "站内通知"),
                new Option("SMS", "短信"),
                new Option("EMAIL", "邮件"),
                new Option("WECHAT", "微信")));
        GROUPS.put("RISK_ALERT_SEVERITY", List.of(
                new Option("FATAL", "致命（立即告警）"),
                new Option("HIGH", "严重（即时告警）"),
                new Option("MEDIUM", "中等（仅记录）"),
                new Option("LOW", "轻微（仅记录）")));
    }

    private EnumCatalog() {
    }

    public static Map<String, List<Option>> all() {
        return GROUPS;
    }
}
