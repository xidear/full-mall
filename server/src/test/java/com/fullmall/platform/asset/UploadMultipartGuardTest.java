package com.fullmall.platform.asset;

import com.fullmall.platform.validation.ValidationRules;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 守住"解析层天花板 ≠ 业务天花板"这个坑：两者不一致必须启动即失败。 */
class UploadMultipartGuardTest {

    @Test
    void matchingCeilingIsAccepted() {
        assertThatCode(() -> new UploadMultipartGuard(
                DataSize.ofBytes(ValidationRules.UPLOAD_MULTIPART_CEILING_BYTES)))
                .doesNotThrowAnyException();
    }

    @Test
    void springDefaultOneMegabyteFailsFast() {
        assertThatThrownBy(() -> new UploadMultipartGuard(DataSize.ofMegabytes(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UPLOAD_MULTIPART_CEILING_BYTES");
    }
}
