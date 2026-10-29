package com.fullmall.setting.api;

import com.fullmall.setting.app.SetupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 首次配置引导：状态查询与"标记完成"。 */
@RestController
@RequestMapping("/admin/v1/setup")
public class SetupController {

    private final SetupService setup;

    public SetupController(SetupService setup) {
        this.setup = setup;
    }

    @GetMapping("/status")
    public SetupService.SetupStatus status() {
        return setup.status();
    }

    /**
     * 标记首次配置完成（可重复调用：写入 setup_completed=true，覆盖写不产生副作用）。
     *
     * <p>它只影响"首次进入是否弹引导"，不是开关：已完成也能再进设置页改配置，
     * 因此这里不做状态机校验，也不回滚任何已生效的设置。
     */
    @PostMapping("/complete")
    public SetupService.SetupStatus complete() {
        setup.markCompleted();
        return setup.status();
    }

    /** 当前数据模式与规模（首次进入时显示"将安装示例数据 / 保持空白"）。 */
    @GetMapping("/data")
    public SetupService.DataStatus data() {
        return setup.dataStatus();
    }

    public record DataModeRequest(String mode) {
    }

    /** 选择示例数据（DEMO）或完全空白（BLANK）。 */
    /**
     * 补充约束：DEMO 只在 dev/demo profile 下可用，否则返回 409 DEMO_SEEDER_UNAVAILABLE，
     * 而不是假装装好了；BLANK 会 TRUNCATE 业务表，已开始运营（有订单/会员）时服务层会拒绝——
     * 这是不可逆操作，前端必须先让用户确认，别当普通开关用。
     */
    @PostMapping("/data")
    public SetupService.DataStatus applyDataMode(@RequestBody DataModeRequest request) {
        return setup.applyDataMode(request == null ? null : request.mode());
    }
}
