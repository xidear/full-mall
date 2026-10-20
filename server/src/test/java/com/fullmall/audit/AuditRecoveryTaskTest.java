package com.fullmall.audit;

import com.fullmall.platform.audit.AuditOutboxRecoveryTask;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 恢复任务默认关闭（避免结构变更期刷失败），且编码稳定。 */
class AuditRecoveryTaskTest {

    @Test
    void taskIsRegisteredButDisabledByDefault() {
        var task = new AuditOutboxRecoveryTask(null);

        assertThat(task.code()).isEqualTo("audit-outbox-recovery");
        assertThat(task.name()).isNotBlank();
        assertThat(task.defaultCron()).isNotBlank();
        assertThat(task.enabledByDefault()).isFalse();
    }
}
