package com.fullmall.catalog.app;

import com.fullmall.catalog.infra.SkuMatrixTemplateRepository;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 规格矩阵模板（ADR-006）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>模板 = 维度 + 取值 + 禁用组合，**不含价格**（未绑定商品时价格没有意义）；</li>
 *   <li>商品可以引用模板生成 SKU，模板本身可被多个商品复用；</li>
 *   <li>维度/取值统一走"标签 + 勾选"，因此这里只接受结构化数据，不接受逗号分隔字符串。</li>
 * </ul>
 */
@Service
public class SkuMatrixTemplateService {

    private static final int MAX_DIMENSIONS = 4;
    private static final int MAX_COMBINATIONS = 500;

    private final SkuMatrixTemplateRepository repository;
    private final SkuMatrixMultiService matrix;
    private final JdbcTemplate jdbc;

    public SkuMatrixTemplateService(SkuMatrixTemplateRepository repository,
                                   SkuMatrixMultiService matrix,
                                   JdbcTemplate jdbc) {
        this.repository = repository;
        this.matrix = matrix;
        this.jdbc = jdbc;
    }

    public record TemplateInput(String code, String name, String note,
                               LinkedHashMap<String, List<String>> dimensions,
                               List<Map<String, String>> disabled) {
    }

    public record TemplateView(UUID id, String code, String name, String note,
                              LinkedHashMap<String, List<String>> dimensions,
                              List<Map<String, String>> disabled,
                              int combinationCount, int enabledCombinationCount,
                              int usedByProducts) {
    }

    public record ApplyResult(UUID templateId, int created, int skipped, int disabled,
                             List<String> codes) {
    }

    public List<TemplateView> list() {
        return repository.findAll().stream().map(this::view).toList();
    }

    public TemplateView get(UUID id) {
        return view(require(id));
    }

    @Transactional
    public TemplateView create(TemplateInput input) {
        Normalized normalized = normalize(input, null);
        UUID id = UUID.randomUUID();
        repository.insert(id, normalized.code(), normalized.name(), normalized.note(),
                normalized.dimensions(), normalized.disabled());
        return get(id);
    }

    @Transactional
    public TemplateView update(UUID id, TemplateInput input) {
        require(id);
        Normalized normalized = normalize(input, id);
        repository.update(id, normalized.code(), normalized.name(), normalized.note(),
                normalized.dimensions(), normalized.disabled());
        return get(id);
    }

    /** 删除：仍被商品引用时拒绝（不允许把商品悄悄变成"没有矩阵"）。 */
    @Transactional
    public void delete(UUID id) {
        require(id);
        int used = repository.productCount(id);
        if (used > 0) {
            throw ApiException.conflict("SKU_MATRIX_TEMPLATE_IN_USE",
                    "模板仍被 " + used + " 个商品使用，请先解绑后再删除");
        }
        repository.delete(id);
    }

    /**
     * 用模板给商品生成 SKU：价格由调用方在"应用模板"时输入（模板本身不存价格）。
     */
    @Transactional
    public ApplyResult applyToProduct(UUID productId, UUID templateId, String prefix,
                                     long priceAmount, String currency) {
        SkuMatrixTemplateRepository.Row template = require(templateId);
        Integer productExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product WHERE id = ?", Integer.class, productId);
        if (productExists == null || productExists == 0) {
            throw ApiException.notFound("PRODUCT_NOT_FOUND", "商品不存在：" + productId);
        }
        SkuMatrixMultiService.Result generated = matrix.generate(productId, prefix,
                template.dimensions(), template.disabled(), priceAmount, currency);
        repository.bindProduct(productId, templateId);
        return new ApplyResult(templateId, generated.created(), generated.skipped(),
                generated.disabled(), generated.codes());
    }

    public void unbindProduct(UUID productId) {
        jdbc.update("UPDATE product SET sku_matrix_template_id = NULL WHERE id = ?", productId);
    }

    private TemplateView view(SkuMatrixTemplateRepository.Row row) {
        int total = 1;
        for (List<String> values : row.dimensions().values()) {
            total *= values.size();
        }
        int disabled = row.disabled() == null ? 0 : row.disabled().size();
        return new TemplateView(row.id(), row.code(), row.name(), row.note(),
                row.dimensions(), row.disabled(), total, Math.max(total - disabled, 0),
                repository.productCount(row.id()));
    }

    private SkuMatrixTemplateRepository.Row require(UUID id) {
        return repository.find(id).orElseThrow(() ->
                ApiException.notFound("SKU_MATRIX_TEMPLATE_NOT_FOUND", "规格矩阵模板不存在：" + id));
    }

    private record Normalized(String code, String name, String note,
                              LinkedHashMap<String, List<String>> dimensions,
                              List<Map<String, String>> disabled) {
    }

    private Normalized normalize(TemplateInput input, UUID excludeId) {
        if (input == null) {
            throw ApiException.badRequest("SKU_MATRIX_TEMPLATE_REQUIRED", "模板内容不能为空");
        }
        String code = input.code() == null ? "" : input.code().trim().toUpperCase(Locale.ROOT);
        String name = input.name() == null ? "" : input.name().trim();
        if (code.isBlank()) {
            throw ApiException.badRequest("SKU_MATRIX_CODE_REQUIRED", "模板编码不能为空");
        }
        if (!code.matches("^[A-Z0-9][A-Z0-9_-]{1,63}$")) {
            throw ApiException.badRequest("SKU_MATRIX_CODE_INVALID",
                    "模板编码只能包含大写字母、数字、下划线与连字符：" + code);
        }
        if (name.isBlank()) {
            throw ApiException.badRequest("SKU_MATRIX_NAME_REQUIRED", "模板名称不能为空");
        }
        if (repository.existsByCode(code, excludeId)) {
            throw ApiException.conflict("SKU_MATRIX_CODE_DUPLICATED", "模板编码已存在：" + code);
        }

        LinkedHashMap<String, List<String>> dimensions = input.dimensions();
        if (dimensions == null || dimensions.isEmpty()) {
            throw ApiException.badRequest("SKU_MATRIX_DIMENSIONS_REQUIRED", "至少需要一个规格维度");
        }
        if (dimensions.size() > MAX_DIMENSIONS) {
            throw ApiException.badRequest("SKU_MATRIX_TOO_MANY_DIMENSIONS",
                    "规格维度最多 " + MAX_DIMENSIONS + " 个，当前 " + dimensions.size());
        }
        LinkedHashMap<String, List<String>> cleaned = new LinkedHashMap<>();
        long total = 1;
        for (Map.Entry<String, List<String>> entry : dimensions.entrySet()) {
            String dimensionName = entry.getKey() == null ? "" : entry.getKey().trim();
            if (dimensionName.isBlank()) {
                throw ApiException.badRequest("SKU_MATRIX_DIMENSION_NAME_REQUIRED", "规格维度名称不能为空");
            }
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                throw ApiException.badRequest("SKU_MATRIX_VALUES_REQUIRED",
                        "规格维度「" + dimensionName + "」至少需要一个取值");
            }
            List<String> unique = new ArrayList<>();
            for (String value : values) {
                String trimmed = value == null ? "" : value.trim();
                if (trimmed.isBlank()) {
                    throw ApiException.badRequest("SKU_MATRIX_VALUE_EMPTY",
                            "规格维度「" + dimensionName + "」存在空取值");
                }
                if (!unique.contains(trimmed)) {
                    unique.add(trimmed);
                }
            }
            cleaned.put(dimensionName, unique);
            total *= unique.size();
        }
        if (total > MAX_COMBINATIONS) {
            throw ApiException.badRequest("SKU_MATRIX_TOO_MANY_COMBINATIONS",
                    "组合数 " + total + " 超过上限 " + MAX_COMBINATIONS);
        }

        List<Map<String, String>> disabled = input.disabled() == null ? List.of() : input.disabled();
        List<Map<String, String>> normalizedDisabled = new ArrayList<>();
        for (Map<String, String> combination : disabled) {
            if (combination == null || combination.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String> entry : combination.entrySet()) {
                List<String> values = cleaned.get(entry.getKey());
                if (values == null) {
                    throw ApiException.badRequest("SKU_MATRIX_DISABLED_UNKNOWN_DIMENSION",
                            "禁用组合引用了不存在的维度：" + entry.getKey());
                }
                if (entry.getValue() == null || !values.contains(entry.getValue().trim())) {
                    throw ApiException.badRequest("SKU_MATRIX_DISABLED_UNKNOWN_VALUE",
                            "禁用组合引用了维度「" + entry.getKey() + "」不存在的取值：" + entry.getValue());
                }
            }
            LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
            for (String dimensionName : cleaned.keySet()) {
                String value = combination.get(dimensionName);
                if (value != null) {
                    normalized.put(dimensionName, value.trim());
                }
            }
            normalizedDisabled.add(normalized);
        }
        return new Normalized(code, name, input.note(), cleaned,
                SkuMatrixTemplateRepository.distinct(normalizedDisabled));
    }
}
