package com.fullmall.staff.app;

import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.validation.ValidationRules;
import com.fullmall.staff.domain.StaffAccount;
import com.fullmall.staff.domain.StaffRoles;
import com.fullmall.staff.domain.StaffUser;
import com.fullmall.staff.infra.StaffAuthRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 员工账号维护（后台管理侧，与 {@link StaffAuthService} 的"我自己登录/改自己"分开）。
 *
 * <p>几条硬约束：
 * <ul>
 *   <li>停用立刻删会话（{@code deleteSessionsForStaff}）：不删的话已登录的人还能用到 token 过期；</li>
 *   <li>不允许停用最后一个生效管理员——否则没人能再进后台；</li>
 *   <li>重置密码同样删会话：管理员重置后必须强制重新登录，否则旧会话仍按旧凭据有效；</li>
 *   <li>只改档案（姓名/角色）；密码、PIN、门店各有专门入口，避免一次请求混着改敏感项。</li>
 * </ul>
 */
@Service
public class StaffAdminService {

    // 规则只从 ValidationRules 取（镜像 docs/validation.schema.json），本类不写死长度/正则
    private static final int USERNAME_MAX = ValidationRules.STAFF_USERNAME_MAX_LENGTH;
    private static final int DISPLAY_NAME_MAX = ValidationRules.STAFF_DISPLAY_NAME_MAX_LENGTH;
    private static final java.util.regex.Pattern USERNAME_PATTERN =
            java.util.regex.Pattern.compile(ValidationRules.STAFF_USERNAME_PATTERN);

    private final StaffAuthRepository repository;
    private final PasswordHasher passwordHasher;
    private final AuditEventService auditEvents;

    public StaffAdminService(StaffAuthRepository repository, PasswordHasher passwordHasher,
                             AuditEventService auditEvents) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.auditEvents = auditEvents;
    }

    @Transactional
    public StaffUser create(String username, String password, String displayName, String role, String actor) {
        String name = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(name).matches()) {
            throw ApiException.badRequest("STAFF_USERNAME_INVALID",
                    "账号只能是字母数字与 . _ -，长度 " + ValidationRules.STAFF_USERNAME_MIN_LENGTH
                            + "-" + USERNAME_MAX + "，实际=" + (username == null ? "null" : username));
        }
        if (password == null || password.length() < ValidationRules.PASSWORD_MIN_LENGTH) {
            throw ApiException.badRequest("STAFF_PASSWORD_TOO_SHORT",
                    "密码至少 " + ValidationRules.PASSWORD_MIN_LENGTH + " 位");
        }
        String normalizedRole = StaffRoles.normalize(role);
        if (!StaffRoles.isBackoffice(normalizedRole)) {
            throw ApiException.badRequest("STAFF_ROLE_UNSUPPORTED",
                    "不支持的角色：" + role + "（可选：" + String.join("/", StaffRoles.BACKOFFICE) + "）");
        }
        if (repository.usernameExists(name)) {
            throw ApiException.badRequest("STAFF_USERNAME_TAKEN", "账号已存在：" + name);
        }
        String display = displayName == null || displayName.isBlank() ? name : displayName.trim();
        if (display.length() > DISPLAY_NAME_MAX) {
            throw ApiException.badRequest("STAFF_DISPLAY_NAME_TOO_LONG",
                    "显示名最多 " + DISPLAY_NAME_MAX + " 个字符");
        }
        StaffUser created = repository.insert(name, passwordHasher.hash(password), display, normalizedRole);
        auditEvents.record(new AuditEventService.AuditEvent("STAFF_CREATED", actor, "staff_user",
                created.id().toString(), "username=" + name + ", role=" + normalizedRole, null));
        return created;
    }

    @Transactional
    public StaffUser update(UUID staffId, String displayName, String role, String actor) {
        StaffAccount existing = requireStaff(staffId);
        String normalizedRole = StaffRoles.normalize(role);
        if (!StaffRoles.isBackoffice(normalizedRole)) {
            throw ApiException.badRequest("STAFF_ROLE_UNSUPPORTED",
                    "不支持的角色：" + role + "（可选：" + String.join("/", StaffRoles.BACKOFFICE) + "）");
        }
        String display = displayName == null || displayName.isBlank()
                ? existing.user().displayName() : displayName.trim();
        if (display.length() > DISPLAY_NAME_MAX) {
            throw ApiException.badRequest("STAFF_DISPLAY_NAME_TOO_LONG", "显示名最多 " + DISPLAY_NAME_MAX + " 个字符");
        }
        // 把唯一的管理员改成别的角色 = 后台没人能进，必须拦住
        if (StaffRoles.ADMIN.equals(existing.user().role()) && !StaffRoles.ADMIN.equals(normalizedRole)
                && repository.countActiveAdmins() <= 1) {
            throw ApiException.badRequest("STAFF_LAST_ADMIN",
                    "这是最后一个生效的管理员，不能改成其它角色；请先另建一个管理员");
        }
        StaffUser updated = repository.updateProfile(staffId, display, normalizedRole);
        auditEvents.record(new AuditEventService.AuditEvent("STAFF_UPDATED", actor, "staff_user",
                staffId.toString(), "role " + existing.user().role() + " -> " + normalizedRole, null));
        return updated;
    }

    @Transactional
    public StaffUser setStatus(UUID staffId, boolean active, String actor) {
        StaffAccount existing = requireStaff(staffId);
        if (!active && StaffRoles.ADMIN.equals(existing.user().role()) && repository.countActiveAdmins() <= 1) {
            throw ApiException.badRequest("STAFF_LAST_ADMIN", "这是最后一个生效的管理员，不能停用");
        }
        StaffUser updated = repository.setStatus(staffId, active ? "ACTIVE" : "DISABLED");
        if (!active) {
            // 停用必须立刻踢掉已有会话：否则被停用的人还能拿旧 token 继续用
            repository.deleteSessionsForStaff(staffId);
        }
        auditEvents.record(new AuditEventService.AuditEvent("STAFF_STATUS_CHANGED", actor, "staff_user",
                staffId.toString(), active ? "ACTIVE" : "DISABLED", null));
        return updated;
    }

    @Transactional
    public void resetPassword(UUID staffId, String password, String actor) {
        requireStaff(staffId);
        if (password == null || password.length() < ValidationRules.PASSWORD_MIN_LENGTH) {
            throw ApiException.badRequest("STAFF_PASSWORD_TOO_SHORT",
                    "密码至少 " + ValidationRules.PASSWORD_MIN_LENGTH + " 位");
        }
        repository.updatePassword(staffId, passwordHasher.hash(password));
        repository.deleteSessionsForStaff(staffId);
        auditEvents.record(new AuditEventService.AuditEvent("STAFF_PASSWORD_RESET", actor, "staff_user",
                staffId.toString(), "强制重新登录", null));
    }

    private StaffAccount requireStaff(UUID staffId) {
        StaffAccount existing = repository.findAccountById(staffId);
        if (existing == null) {
            throw ApiException.notFound("STAFF_NOT_FOUND", "员工不存在：" + staffId);
        }
        return existing;
    }
}
