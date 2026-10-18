package com.fullmall.staff.api;

import com.fullmall.staff.app.StaffAuthService;
import com.fullmall.staff.domain.StaffUser;
import com.fullmall.staff.web.StaffAuthFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/auth")
public class StaffAuthController {

    private final StaffAuthService authService;

    public StaffAuthController(StaffAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password(),
                request.captchaId(), request.captchaAnswer());
    }

    @GetMapping("/me")
    public StaffUser me(@RequestAttribute(StaffAuthFilter.STAFF_USER_ATTRIBUTE) StaffUser user) {
        return user;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(token(authorization));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestAttribute(StaffAuthFilter.STAFF_USER_ATTRIBUTE) StaffUser user,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user, request.oldPassword(), request.newPassword());
    }

    private String token(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}