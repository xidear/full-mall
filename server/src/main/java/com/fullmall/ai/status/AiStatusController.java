package com.fullmall.ai.status;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 能力状态（后台 AI 客服页据此决定展示什么）。
 *
 * <p>为什么需要它：前端原来调 {@code /admin/v1/ai/status}，但**这个端点从未实现**，
 * 于是永远 404、永远显示"当前是社区版构建，不包含 AI 模块"——把"端点缺失"错报成"版本没有该功能"。
 *
 * <p>版本与能力是两个维度，必须分开表达：
 * <ul>
 *   <li>{@code edition}：构建形态（internal / community）。开源版不附带模型权重，但代码路径应存在；</li>
 *   <li>{@code enabled}：现在**能不能用**——取决于有没有可用供应商；没配就提示去「第三方服务」配置。</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/v1/ai")
public class AiStatusController {

    public record AiStatusView(String edition, boolean enabled, int providerCount, String reason) {
    }

    private final JdbcTemplate jdbc;
    private final String edition;

    public AiStatusController(JdbcTemplate jdbc, @Value("${app.edition:internal}") String edition) {
        this.jdbc = jdbc;
        this.edition = edition;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AiStatusView status() {
        List<String> providers = jdbc.queryForList(
                "SELECT code FROM ai_provider WHERE enabled", String.class);
        boolean enabled = !providers.isEmpty();
        String reason = enabled ? null
                : "未配置可用的 AI 供应商：请到「第三方服务」登记模型供应商（endpoint/apiKey/model）后再试";
        return new AiStatusView(edition, enabled, providers.size(), reason);
    }
}
