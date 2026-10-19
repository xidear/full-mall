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

    public ExportJobController(ExportJobService service) {
        this.service = service;
    }

    public record CreateExport(String resource, String projection, Map<String, Object> params) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, UUID> create(@RequestBody CreateExport body, Authentication authentication) {
        UUID id = service.create(authentication.getName(), body.resource(), body.projection(),
                body.params(), null);
        return Map.of("jobId", id);
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ExportJobService.ExportJob status(@PathVariable UUID jobId, Authentication authentication) {
        return service.findForRequester(jobId, authentication.getName());
    }
}
