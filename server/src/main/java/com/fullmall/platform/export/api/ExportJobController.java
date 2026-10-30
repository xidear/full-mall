package com.fullmall.platform.export.api;

import com.fullmall.platform.export.ExportJobService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** 异步导出：创建任务 + 查询状态（前端轮询）。 */
@RestController
@RequestMapping("/admin/v1/export-jobs")
public class ExportJobController {

    private final ExportJobService service;
    private final com.fullmall.platform.export.ExportStorage storage;

    public ExportJobController(ExportJobService service,
                               com.fullmall.platform.export.ExportStorage storage) {
        this.service = service;
        this.storage = storage;
    }

    public record CreateExport(String resource, String projection, Map<String, Object> params) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, UUID> create(@RequestBody CreateExport body, Authentication authentication) {
        // 创建即校验存储：否则任务会 PENDING→FAILED，用户白等一轮才发现没配存储
        if (storage instanceof com.fullmall.platform.export.LocalExportStorage local) {
            local.assertConfigured();
        }
        UUID id = service.create(authentication.getName(), body.resource(), body.projection(),
                body.params(), null);
        return Map.of("jobId", id);
    }

    /** 下载：鉴权（只有发起人）+ 过期检查；文件不存在直接 410，不返回空内容。 */
    @GetMapping("/{jobId}/download")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public org.springframework.http.ResponseEntity<byte[]> download(@PathVariable UUID jobId,
                                                                    Authentication authentication) {
        var job = service.findForRequester(jobId, authentication.getName());
        if (!"SUCCEEDED".equals(job.status()) || job.filePath() == null) {
            throw new com.fullmall.platform.error.ApiException(
                    org.springframework.http.HttpStatus.CONFLICT, "EXPORT_NOT_READY",
                    "导出尚未完成，当前状态=" + job.status());
        }
        if (!storage.exists(job.filePath())) {
            throw new com.fullmall.platform.error.ApiException(
                    org.springframework.http.HttpStatus.GONE, "EXPORT_FILE_MISSING",
                    "导出文件已不可用，请重新导出");
        }
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=export-" + jobId + ".csv")
                .header("Content-Type", "text/csv;charset=UTF-8")
                .body(storage.read(job.filePath()));
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ExportJobService.ExportJob status(@PathVariable UUID jobId, Authentication authentication) {
        return service.findForRequester(jobId, authentication.getName());
    }
}
