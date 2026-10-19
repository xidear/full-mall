package com.fullmall.staff.api;

import com.fullmall.staff.api.StaffPinRequest.ManagerAuth;
import com.fullmall.staff.app.StaffAuthService;
import com.fullmall.staff.domain.StaffUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/store/v1/pos/auth")
public class ManagerAuthController {

    private final StaffAuthService authService;

    public ManagerAuthController(StaffAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/manager")
    public StaffUser manager(@Valid @RequestBody ManagerAuth request) {
        StaffUser user = authService.verifyPin(request.username(), request.pin());
        if (user == null || (!"ADMIN".equals(user.role()) && !"OPERATOR".equals(user.role())
                && !"STORE".equals(user.role()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "店长授权失败：账号或 PIN 不正确");
        }
        return user;
    }
}
