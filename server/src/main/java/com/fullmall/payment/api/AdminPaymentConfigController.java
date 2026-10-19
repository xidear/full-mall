package com.fullmall.payment.api;

import com.fullmall.payment.app.PaymentConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 支付渠道配置（PAY-002）。全部要求 ADMIN；返回掩码后的标识。 */
@RestController
@RequestMapping("/admin/v1/payment-configs")
public class AdminPaymentConfigController {

    private final PaymentConfigService service;

    public AdminPaymentConfigController(PaymentConfigService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, UUID> create(@RequestBody PaymentConfigService.ConfigInput input,
                                    Authentication authentication) {
        return Map.of("id", service.create(input, authentication.getName()));
    }

    @PutMapping("/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> update(@PathVariable UUID configId,
                                       @RequestBody PaymentConfigService.ConfigInput input,
                                       Authentication authentication) {
        return Map.of("version", service.update(configId, input, authentication.getName()));
    }

    @PostMapping("/{configId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable UUID configId, Authentication authentication) {
        service.deactivate(configId, authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PaymentConfigService.ConfigView> list() {
        return service.list();
    }
}
