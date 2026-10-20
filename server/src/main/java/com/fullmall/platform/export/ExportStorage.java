package com.fullmall.platform.export;

/**
 * 导出文件存储抽象。
 *
 * <p>为什么需要抽象：本地目录在多实例部署时互相读不到；上线要换对象存储（S3/SeaweedFS/OSS）。
 * 任务表只存 **storage key**，不存绝对路径，切换实现不影响历史任务。
 */
public interface ExportStorage {

    /** 写入并返回 key（相对标识，不含主机名与协议）。 */
    String store(String key, byte[] content);

    byte[] read(String key);

    boolean exists(String key);

    /**
     * 删除早于保留期的导出文件。
     *
     * <p>默认实现返回 0（对象存储应由生命周期规则处理）；本地磁盘实现会真正删除。
     * 返回值必须真实反映删除数量，不得静默返回 0 冒充成功。
     */
    default int deleteOlderThan(int days) {
        return 0;
    }
}
