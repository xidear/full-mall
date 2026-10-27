package com.fullmall.ops.api;

import com.fullmall.ops.app.OpsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/v1/ops")
public class OpsController {

    private final OpsService service;

    public OpsController(OpsService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return service.overview();
    }

    @GetMapping("/containers")
    public Map<String, Object> containers() {
        return service.containers();
    }

    @GetMapping("/security")
    public Map<String, Object> security() {
        return service.security();
    }

    @GetMapping("/ssl")
    public ResponseEntity<Map<String, Object>> ssl() {
        return upstream(() -> service.ssl());
    }

    @PostMapping("/ssl/renew")
    public ResponseEntity<Map<String, Object>> renew(@RequestBody(required = false) Map<String, Object> payload) {
        return upstream(() -> service.renewSsl(payload));
    }

    @PostMapping("/containers/{host}/{id}/{action}")
    public ResponseEntity<Map<String, Object>> containerAction(@PathVariable String host,
                                                               @PathVariable String id,
                                                               @PathVariable String action) {
        return upstream(() -> service.containerAction(host, id, action));
    }

    private ResponseEntity<Map<String, Object>> upstream(java.util.function.Supplier<Map<String, Object>> call) {
        try {
            return ResponseEntity.ok(call.get());
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", String.valueOf(error.getMessage())));
        } catch (RuntimeException error) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("ok", false, "error", String.valueOf(error.getMessage())));
        }
    }
}
