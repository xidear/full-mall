package com.fullmall.platform.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 契约测试：Java 常量必须与 docs/validation.schema.json 完全一致。
 *
 * <p>前端常量由 `npm run generate:validation` 从同一文件生成；
 * 因此修改规则时不会出现 Java/TS 两边漂移。
 */
class ValidationRulesContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void javaRulesMatchSharedSchema() throws Exception {
        // 默认按仓库结构找到 docs；容器/CI 可用 -Dvalidation.schema.path 覆盖。
        Path schemaPath = Path.of(System.getProperty("validation.schema.path",
                Path.of("..", "docs", "validation.schema.json").toString()));
        assertThat(Files.exists(schemaPath))
                .as("契约文件必须存在：%s", schemaPath.toAbsolutePath())
                .isTrue();
        JsonNode defs = MAPPER.readTree(Files.readString(schemaPath)).path("$defs");

        assertThat(ValidationRules.PHONE_PATTERN).isEqualTo(defs.path("phone").path("pattern").asText());
        assertThat(ValidationRules.PASSWORD_MIN_LENGTH).isEqualTo(defs.path("password").path("minLength").asInt());
        assertThat(ValidationRules.STAFF_USERNAME_PATTERN).isEqualTo(defs.path("staffUsername").path("pattern").asText());
        assertThat(ValidationRules.STAFF_USERNAME_MIN_LENGTH).isEqualTo(defs.path("staffUsername").path("minLength").asInt());
        assertThat(ValidationRules.STAFF_USERNAME_MAX_LENGTH).isEqualTo(defs.path("staffUsername").path("maxLength").asInt());
        assertThat(ValidationRules.STAFF_DISPLAY_NAME_MAX_LENGTH)
                .isEqualTo(defs.path("staffDisplayName").path("maxLength").asInt());
        assertThat(ValidationRules.PIN_MIN_LENGTH).isEqualTo(defs.path("pin").path("minLength").asInt());
        assertThat(ValidationRules.PIN_MAX_LENGTH).isEqualTo(defs.path("pin").path("maxLength").asInt());
        assertThat(ValidationRules.QUANTITY_MIN).isEqualTo(defs.path("quantity").path("minimum").asInt());
        assertThat(ValidationRules.QUANTITY_MAX).isEqualTo(defs.path("quantity").path("maximum").asInt());
        assertThat(ValidationRules.PRICE_MIN).isEqualTo(defs.path("priceAmount").path("minimum").asInt());
        assertThat(ValidationRules.CONTACT_NAME_MAX).isEqualTo(defs.path("contactName").path("maxLength").asInt());
        assertThat(ValidationRules.SKU_CODE_PATTERN).isEqualTo(defs.path("skuCode").path("pattern").asText());
        assertThat(ValidationRules.COUPON_CODE_PATTERN).isEqualTo(defs.path("couponCode").path("pattern").asText());

        assertThat(ValidationRules.REVIEW_MEDIA_MAX).isEqualTo(defs.path("reviewMedia").path("maximum").asInt());

        // 收货地址（ADR-0013/0014）：国家码/区划码与长度上限的唯一来源。
        JsonNode address = defs.path("address").path("properties");
        assertThat(address.isMissingNode()).as("schema 必须包含 $defs.address").isFalse();
        assertThat(ValidationRules.ADDRESS_COUNTRY_PATTERN)
                .isEqualTo(address.path("countryCode").path("pattern").asText());
        assertThat(ValidationRules.ADDRESS_REGION_CODE_PATTERN)
                .isEqualTo(address.path("regionCode").path("pattern").asText());
        assertThat(ValidationRules.ADDRESS_REGION_NAME_MAX_LENGTH)
                .isEqualTo(address.path("province").path("maxLength").asInt());
        assertThat(ValidationRules.ADDRESS_DETAIL_MIN_LENGTH)
                .isEqualTo(address.path("detail").path("minLength").asInt());
        assertThat(ValidationRules.ADDRESS_DETAIL_MAX_LENGTH)
                .isEqualTo(address.path("detail").path("maxLength").asInt());
        assertThat(ValidationRules.ADDRESS_POSTAL_CODE_MAX_LENGTH)
                .isEqualTo(address.path("postalCode").path("maxLength").asInt());

        // 素材上传：类型白名单与体积上限。这组常量同时被 MediaUploadValidator（权威校验）与
        // SystemSettingService（后台可配上限的边界）引用，漂移会导致"后台能填、上传必失败"。
        JsonNode upload = defs.path("assetUpload");
        assertThat(upload.isMissingNode()).as("schema 必须包含 $defs.assetUpload").isFalse();
        JsonNode uploadProps = upload.path("properties");

        assertThat(ValidationRules.UPLOAD_IMAGE_TYPES)
                .isEqualTo(textList(uploadProps.path("imageTypes").path("items").path("enum")));
        assertThat(ValidationRules.UPLOAD_VIDEO_TYPES)
                .isEqualTo(textList(uploadProps.path("videoTypes").path("items").path("enum")));

        assertBytesRule(uploadProps, "adminImageMaxBytes", ValidationRules.UPLOAD_ADMIN_IMAGE_DEFAULT_BYTES,
                ValidationRules.UPLOAD_ADMIN_IMAGE_MAX_CONFIGURABLE_BYTES);
        assertBytesRule(uploadProps, "adminVideoMaxBytes", ValidationRules.UPLOAD_ADMIN_VIDEO_DEFAULT_BYTES,
                ValidationRules.UPLOAD_ADMIN_VIDEO_MAX_CONFIGURABLE_BYTES);
        assertBytesRule(uploadProps, "memberImageMaxBytes", ValidationRules.UPLOAD_MEMBER_IMAGE_DEFAULT_BYTES,
                ValidationRules.UPLOAD_MEMBER_IMAGE_MAX_CONFIGURABLE_BYTES);
        assertBytesRule(uploadProps, "memberVideoMaxBytes", ValidationRules.UPLOAD_MEMBER_VIDEO_DEFAULT_BYTES,
                ValidationRules.UPLOAD_MEMBER_VIDEO_MAX_CONFIGURABLE_BYTES);

        assertThat(uploadProps.path("multipartCeilingBytes").path("default").asLong())
                .isEqualTo(ValidationRules.UPLOAD_MULTIPART_CEILING_BYTES);
    }

    /** schema 的 enum 数组转成字符串列表，用于与 Java 白名单逐项比对（顺序也算契约）。 */
    private static java.util.List<String> textList(JsonNode array) {
        java.util.List<String> values = new java.util.ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }

    /** 每个体积规则都同时校验默认值、可配最大值与共用下限。 */
    private static void assertBytesRule(JsonNode props, String name, long expectedDefault, long expectedMax) {
        JsonNode rule = props.path(name);
        assertThat(rule.path("default").asLong()).as("%s.default", name).isEqualTo(expectedDefault);
        assertThat(rule.path("maximum").asLong()).as("%s.maximum", name).isEqualTo(expectedMax);
        assertThat(rule.path("minimum").asLong()).as("%s.minimum", name)
                .isEqualTo(ValidationRules.UPLOAD_MIN_CONFIGURABLE_BYTES);
    }
}
