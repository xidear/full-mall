package com.fullmall.platform.asset;

/**
 * 素材存储端口（图片/视频等）。
 *
 * <p>业务只依赖本接口；具体实现（七牛/COS/本地）在各自包内，可整体删除。
 */
public interface AssetStorage {

    /** 存储实现标识（qiniu / cos / local），用于健康检查与日志。 */
    String provider();

    /** 上传并返回可访问 URL。key 由调用方生成（如 `product/{id}/{uuid}.jpg`）。 */
    String put(String key, byte[] content, String contentType);

    /** 删除；不存在时返回 false，不抛错（幂等）。 */
    boolean delete(String key);
}
