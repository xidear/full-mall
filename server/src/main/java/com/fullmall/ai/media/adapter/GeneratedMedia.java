package com.fullmall.ai.media.adapter;

/**
 * 生成结果：二进制内容或远端 URL 二选一。
 *
 * <p>视频通常给 URL（体积大），图片尽量直接拿字节落到我们自己的存储。
 */
public record GeneratedMedia(byte[] content, String sourceUrl, String contentType) {

    public static GeneratedMedia bytes(byte[] content, String contentType) {
        return new GeneratedMedia(content, null, contentType);
    }

    public static GeneratedMedia url(String url, String contentType) {
        return new GeneratedMedia(null, url, contentType);
    }

    public boolean hasBytes() {
        return content != null && content.length > 0;
    }
}
