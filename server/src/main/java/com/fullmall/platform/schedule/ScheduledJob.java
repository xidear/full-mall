package com.fullmall.platform.schedule;

/**
 * 可被图形化配置的定时任务。
 *
 * <p>实现类只需声明 `code` 与默认 cron，并实现 `run`；**cron 与启停状态存在数据库**，
 * 后台可以随时修改，不需要重启应用，也不需要重新打包。
 *
 * <p>约定：`run()` 内部不做"吞异常"处理——失败必须抛出，由调度器记录 `last_status=FAILED`
 * 与 `last_error`，这样运维在界面上能直接看到失败原因。
 */
public interface ScheduledJob {

    /** 任务编码（稳定标识，落库主键）。 */
    String code();

    /** 展示名称。 */
    String name();

    /** 首次注册时使用的默认 cron（之后以数据库为准）。 */
    String defaultCron();

    /** 是否默认启用（之后以数据库为准）。 */
    boolean enabledByDefault();

    void run() throws Exception;
}
