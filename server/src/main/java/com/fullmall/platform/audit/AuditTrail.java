package com.fullmall.platform.audit;

import jakarta.annotation.PreDestroy;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 审计网关：所有 JaVers 调用都必须经过本类。
 *
 * <p>为什么异步：JaVers 7.11.9 的 SQL 仓储不关闭连接，我们固定复用一条连接（见 {@code AuditConfig}），
 * 因此写入必须串行。**串行不等于阻塞业务线程**——这里用一个有界队列 + 单消费者线程：
 * <ul>
 *   <li>业务线程只做入队，不等待 JaVers；</li>
 *   <li>单消费者按顺序用同一条连接写入，保持串行假设；</li>
 *   <li>队列满时**明确抛错**（不丢审计、也不无限堆积）；</li>
 *   <li>应用关闭时 {@link #shutdown()} 排空队列，避免丢失。</li>
 * </ul>
 *
 * <p>后续可换成数据库 outbox（跨进程可靠投递），当前实现解决的是"业务线程被审计阻塞"。
 */
@Component
public class AuditTrail {

    private static final Logger log = LoggerFactory.getLogger(AuditTrail.class);
    private static final int QUEUE_CAPACITY = 10_000;

    private final Javers javers;
    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final Thread worker;
    private final AtomicLong written = new AtomicLong();
    /** 已入队但尚未写完的任务数（含正在执行的那条）。 */
    private final java.util.concurrent.atomic.AtomicInteger pending =
            new java.util.concurrent.atomic.AtomicInteger();
    private final AtomicLong failed = new AtomicLong();
    private volatile boolean running = true;

    public AuditTrail(Javers javers) {
        this.javers = javers;
        this.worker = new Thread(this::consume, "audit-writer");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public void commit(String author, Object entity) {
        enqueue(() -> javers.commit(author, entity));
    }

    public List<CdoSnapshot> history(UUID entityId, Class<?> type) {
        flush();
        return javers.findSnapshots(QueryBuilder.byInstanceId(entityId, type).build());
    }

    /** 阻塞直到队列排空（测试与关闭时使用）。 */
    public void flush() {
        while (!queue.isEmpty() || pending.get() > 0) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待审计队列清空时被中断", error);
            }
        }
        if (failed.get() > 0) {
            throw new IllegalStateException("审计写入失败 " + failed.get() + " 条，请检查审计存储");
        }
    }

    public long written() {
        return written.get();
    }

    private void enqueue(Runnable task) {
        if (!running) {
            throw new IllegalStateException("审计写入器已关闭，拒绝静默丢弃审计");
        }
        pending.incrementAndGet();
        if (!queue.offer(task)) {
            pending.decrementAndGet();
            throw new IllegalStateException(
                    "审计队列已满（容量 " + QUEUE_CAPACITY + "），请检查审计写入是否阻塞");
        }
    }

    private void consume() {
        while (running || !queue.isEmpty()) {
            try {
                Runnable task = queue.poll(200, TimeUnit.MILLISECONDS);
                if (task == null) {
                    continue;
                }
                try {
                    task.run();
                    written.incrementAndGet();
                } catch (RuntimeException error) {
                    failed.incrementAndGet();
                    log.error("审计写入失败（已计数，flush 时会报错）", error);
                } finally {
                    pending.decrementAndGet();
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        try {
            worker.join(5000);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
        if (!queue.isEmpty()) {
            log.error("审计队列在关闭时仍有 {} 条未写入", queue.size());
        }
    }
}
