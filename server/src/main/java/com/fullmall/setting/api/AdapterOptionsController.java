package com.fullmall.setting.api;

import com.fullmall.platform.secret.IntegrationCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 适配器候选：每个能力下**哪些服务商现在真的能用**。
 *
 * <p>解决两个具体问题：
 * <ol>
 *   <li>设置页原本把候选写死，连"待接入"的服务商也列出来，选了必然报错；</li>
 *   <li>存储把「本地磁盘」和「Mock」混成一个选项——本地是真实存储（生产也能用），
 *       Mock 只是演示（不落盘、不访问外部服务），两者必须分开。</li>
 * </ol>
 *
 * <p>判定口径：内置实现（本地磁盘）恒可用；Mock 恒可用但标注为演示；其余服务商只有在
 * 「第三方服务」里配过该能力+服务商（且密钥已配置）时才算可用。未配置的仍然返回，
 * 但 available=false，前端置灰并说明原因——而不是让运营选到之后在运行期炸。
 */
@RestController
@RequestMapping("/admin/v1/settings/adapters")
public class AdapterOptionsController {

    public record Option(String code, String label, String kind, boolean available, String note) {
    }

    /** 设置页用到的四个能力；与 system_setting 的 *_provider 键一一对应。 */
    private static final List<String> CAPABILITIES = List.of("storage", "sms", "payment", "logistics");

    private final JdbcTemplate jdbc;
    /** 环境变量侧已配好的存储凭据（历史部署走 env，不能因为页面上没配就判成不可用）。 */
    private final Set<String> envConfigured;

    public AdapterOptionsController(JdbcTemplate jdbc,
                                    @org.springframework.beans.factory.annotation.Value("${app.asset.qiniu.access-key:}") String qiniuKey,
                                    @org.springframework.beans.factory.annotation.Value("${app.asset.qiniu.secret-key:}") String qiniuSecret,
                                    @org.springframework.beans.factory.annotation.Value("${app.asset.qiniu.bucket:}") String qiniuBucket,
                                    @org.springframework.beans.factory.annotation.Value("${app.asset.cos.secret-id:}") String cosId,
                                    @org.springframework.beans.factory.annotation.Value("${app.asset.cos.secret-key:}") String cosSecret,
                                    @org.springframework.beans.factory.annotation.Value("${app.asset.cos.bucket:}") String cosBucket) {
        this.jdbc = jdbc;
        Set<String> configured = new java.util.HashSet<>();
        if (!qiniuKey.isBlank() && !qiniuSecret.isBlank() && !qiniuBucket.isBlank()) {
            configured.add("storage:qiniu");
        }
        if (!cosId.isBlank() && !cosSecret.isBlank() && !cosBucket.isBlank()) {
            configured.add("storage:cos");
        }
        this.envConfigured = Set.copyOf(configured);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, List<Option>> options() {
        Set<String> configured = configuredKeys();
        configured.addAll(envConfigured);
        Map<String, List<Option>> result = new LinkedHashMap<>();
        for (String capability : CAPABILITIES) {
            List<Option> options = new ArrayList<>();
            // 存储没有 mock：ConfigurableAssetStorage 里 "mock" 只是 local 的历史别名，
            // 把两者并成一个选项会让运营以为"本地=演示"，生产环境不敢选本地磁盘。
            if (!"storage".equals(capability)) {
                options.add(new Option("mock", "Mock（演示用）", "MOCK", true,
                        "不落盘、不访问外部服务，仅用于演示；生产请选真实实现"));
            }
            for (IntegrationCatalog.Provider provider : IntegrationCatalog.providers(capability)) {
                boolean available = configured.contains(capability + ":" + provider.code());
                // 本地存储在清单里没有凭据可言，属于内置实现
                boolean builtin = "storage".equals(capability) && "local".equals(provider.code());
                options.add(new Option(provider.code(), provider.label(),
                        builtin ? "BUILTIN" : "CONFIGURED",
                        builtin || available,
                        builtin ? "随应用运行，无需凭据；文件落在 app.asset.local.directory"
                                : available ? "已在「第三方服务」配置" : "尚未在「第三方服务」配置凭据"));
            }
            result.put(capability, options);
        }
        return result;
    }

    private Set<String> configuredKeys() {
        Set<String> keys = new java.util.HashSet<>();
        jdbc.query("""
                SELECT capability, provider FROM integration_config
                 WHERE config_enc IS NOT NULL
                """, rs -> {
            keys.add(rs.getString("capability") + ":" + rs.getString("provider"));
        });
        return keys;
    }
}
