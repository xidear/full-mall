package com.fullmall.platform.asset;

import com.fullmall.platform.validation.ValidationRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * 启动自检：Spring multipart 单文件上限必须等于 {@link ValidationRules#UPLOAD_MULTIPART_CEILING_BYTES}。
 *
 * <p>为什么需要这个自检：解析层天花板（application.yml）与业务侧可配上限（系统设置）是两份配置，
 * 一旦天花板被人改小，后台仍能填 10MB，但任何 1MB 以上的视频都会在进 Controller 之前被拒，
 * 报错里只有"文件过大"，看不出是配置问题。这里在启动时就失败，报出两边的真实数字。
 */
@Component
public class UploadMultipartGuard {

    public UploadMultipartGuard(
            @Value("${spring.servlet.multipart.max-file-size:1MB}") DataSize maxFileSize) {
        long actual = maxFileSize.toBytes();
        long expected = ValidationRules.UPLOAD_MULTIPART_CEILING_BYTES;
        if (actual != expected) {
            throw new IllegalStateException("spring.servlet.multipart.max-file-size=" + actual
                    + " 字节，与 ValidationRules.UPLOAD_MULTIPART_CEILING_BYTES=" + expected
                    + " 不一致；两者必须相等，否则后台能配的上传上限会被解析层提前拒绝");
        }
    }
}
