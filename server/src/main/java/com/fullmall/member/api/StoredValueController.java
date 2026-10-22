package com.fullmall.member.api;

import com.fullmall.member.app.StoredValueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 储值充值（门店/后台代客充值，现金或扫码收款后录入）。 */
@RestController
@RequestMapping("/admin/v1/members/{memberId}/wallet")
public class StoredValueController {

    private final StoredValueService storedValue;

    public StoredValueController(StoredValueService storedValue) {
        this.storedValue = storedValue;
    }

    public record TopUp(long amount, String businessKey, String note) {
    }

    @PostMapping("/topup")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public StoredValueService.Result topUp(@PathVariable UUID memberId, @RequestBody TopUp body) {
        return storedValue.topUp(memberId, body.amount(), body.businessKey(), body.note());
    }
}
