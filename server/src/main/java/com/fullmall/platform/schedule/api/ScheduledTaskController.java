package com.fullmall.platform.schedule.api;

import com.fullmall.platform.schedule.ScheduledTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图形化定时任务配置：列表（含下次执行时间）、改 cron / 启停、立即执行。
 *
 * <p>权限由 `StaffAuthFilter` 判定（`/admin/v1/**` 未知路径回落 `permission.manage`，仅 ADMIN）。
 */
@RestController
@RequestMapping("/admin/v1/scheduled-tasks")
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
        com.fullmall.platform.schedule.ScheduledTaskService.class)
public class ScheduledTaskController {

    private final ScheduledTaskService tasks;

    public ScheduledTaskController(ScheduledTaskService tasks) {
        this.tasks = tasks;
    }

    public record UpdateTask(String cron, boolean enabled) {
    }

    @GetMapping
    public List<ScheduledTaskService.TaskView> list() {
        return tasks.list();
    }

    @PutMapping("/{code}")
    public ScheduledTaskService.TaskView update(@PathVariable String code, @RequestBody UpdateTask body) {
        return tasks.update(code, body.cron(), body.enabled());
    }

    @PostMapping("/{code}/run")
    public ScheduledTaskService.TaskView runNow(@PathVariable String code) {
        return tasks.runNow(code);
    }
}
