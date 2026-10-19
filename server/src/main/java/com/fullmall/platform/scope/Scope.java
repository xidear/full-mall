package com.fullmall.platform.scope;

/**
 * 行级数据范围（见 TECHNICAL_ARCHITECTURE_V2 §2.3）。
 *
 * <p>只有两种：全量（ADMIN/OPERATOR）与按归属人。供应商/门店维度在接入时扩展 {@link Kind}，
 * 不做字段级权限矩阵。
 */
public record Scope(Kind kind, String ownerKey) {

    public enum Kind {
        /** 全量可见（平台运营/管理员）。 */
        ALL,
        /** 仅可见 ownerKey 相同的行。 */
        OWNER
    }

    public static Scope all() {
        return new Scope(Kind.ALL, null);
    }

    public static Scope owner(String ownerKey) {
        return new Scope(Kind.OWNER, ownerKey);
    }

    public boolean isAll() {
        return kind == Kind.ALL;
    }

    /**
     * @param rowOwnerKey 数据行的归属人；为 null 时只有 ALL 范围可见。
     */
    public boolean allows(String rowOwnerKey) {
        if (isAll()) {
            return true;
        }
        return ownerKey != null && ownerKey.equals(rowOwnerKey);
    }
}