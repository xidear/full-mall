package com.fullmall.payment.reconcile;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 对账差异查询（财务用）。 */
@RestController
@RequestMapping("/admin/v1/payments/reconciliation")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class PaymentReconciliationController {

    private final PaymentReconciliationService reconciliation;

    public PaymentReconciliationController(PaymentReconciliationService reconciliation) {
        this.reconciliation = reconciliation;
    }

    @GetMapping("/differences")
    public List<Map<String, Object>> differences(@RequestParam String billDate) {
        return reconciliation.differences(LocalDate.parse(billDate));
    }
}
