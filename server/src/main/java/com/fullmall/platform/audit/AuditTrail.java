package com.fullmall.platform.audit;

import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 审计网关：所有 JaVers 调用都必须经过本类。
 *
 * <p>原因见 {@link AuditConfig#javersSqlRepository}：JaVers 7.11.9 不关闭连接，
 * 我们固定复用一条连接，因此审计写入必须串行，避免多线程共用同一连接。
 *
 * <p>约束：审计提交与业务事务不在同一事务内（JaVers 用自己的连接）。
 * 业务回滚时审计可能多一条记录，属于可接受的“多记不可漏记”策略；
 * 需要强一致时在业务侧用 outbox，后续阶段处理。
 */
@Component
public class AuditTrail {

    private final Javers javers;

    public AuditTrail(Javers javers) {
        this.javers = javers;
    }

    public synchronized void commit(String author, Object entity) {
        javers.commit(author, entity);
    }

    public synchronized List<CdoSnapshot> history(UUID entityId, Class<?> type) {
        return javers.findSnapshots(QueryBuilder.byInstanceId(entityId, type).build());
    }
}