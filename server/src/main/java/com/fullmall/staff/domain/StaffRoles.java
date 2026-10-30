package com.fullmall.staff.domain;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 员工角色：**唯一来源**。
 *
 * <p>此前角色字符串散落在权限矩阵、鉴权过滤器、门店范围判断、前端标签等 6 处，
 * 改一个角色要改 6 个地方，漏一处就出现"能登录但权限集为空"的账号。新增角色只改这里。
 */
public final class StaffRoles {

    public static final String ADMIN = "ADMIN";
    public static final String OPERATOR = "OPERATOR";
    public static final String STORE = "STORE";
    public static final String CS = "CS";
    public static final String SUPPLIER = "SUPPLIER";

    /** 后台账号可以使用的角色（SUPPLIER 是供应商端身份，不出现在后台员工管理里）。 */
    public static final List<String> BACKOFFICE = List.of(ADMIN, OPERATOR, STORE, CS);

    /** 可以在权限矩阵里配置的角色：ADMIN 恒有全部权限，不参与配置。 */
    public static final List<String> EDITABLE_PERMISSION = List.of(OPERATOR, STORE, CS);

    private static final Set<String> ALL_KNOWN = Set.of(ADMIN, OPERATOR, STORE, CS, SUPPLIER);

    private StaffRoles() {
    }

    /** 运营后台（/admin/**）可用角色。 */
    public static java.util.Set<String> adminSide() {
        return Set.of(ADMIN, OPERATOR);
    }

    /** 门店侧（/store/**）：店长/店员也要能进。 */
    public static java.util.Set<String> storeSide() {
        return Set.of(ADMIN, OPERATOR, STORE);
    }

    /** 客服侧（/support/**）。 */
    public static java.util.Set<String> supportSide() {
        return Set.of(ADMIN, OPERATOR, CS);
    }

    /** 不受门店数据范围限制的角色（其余角色按 staff_store 过滤）。 */
    public static java.util.Set<String> unrestrictedStoreSide() {
        return Set.of(ADMIN, OPERATOR);
    }

    public static boolean isBackoffice(String role) {
        return role != null && BACKOFFICE.contains(normalize(role));
    }

    public static boolean isKnown(String role) {
        return role != null && ALL_KNOWN.contains(normalize(role));
    }

    public static String normalize(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
