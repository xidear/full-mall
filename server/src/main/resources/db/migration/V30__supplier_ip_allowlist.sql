-- 供应商 IP 白名单（SUP-030）：多段 CIDR，逗号分隔；空值表示不限制。
ALTER TABLE supplier_credential ADD COLUMN ip_allowlist VARCHAR(512);