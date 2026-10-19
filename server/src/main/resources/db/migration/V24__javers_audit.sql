-- JaVers 审计表（成熟审计组件，schema 仍由 Flyway 管理）
-- 结构取自 JaVers 7.11.9 在 PostgreSQL 上的实际建表结果（javers.ensureSchema() 导出）。
-- JaVers 使用显式序列名（jv_*_pk_seq），不能用 BIGSERIAL 的自动命名
CREATE SEQUENCE jv_global_id_pk_seq;
CREATE SEQUENCE jv_commit_pk_seq;
CREATE SEQUENCE jv_snapshot_pk_seq;

CREATE TABLE jv_global_id (
    global_id_pk BIGINT PRIMARY KEY DEFAULT nextval('jv_global_id_pk_seq'),
    local_id     VARCHAR(191),
    fragment     VARCHAR(200),
    type_name    VARCHAR(200),
    owner_id_fk  BIGINT
);
CREATE INDEX jv_global_id_local_id_idx ON jv_global_id (local_id);
CREATE INDEX jv_global_id_owner_id_fk_idx ON jv_global_id (owner_id_fk);

CREATE TABLE jv_commit (
    commit_pk           BIGINT PRIMARY KEY DEFAULT nextval('jv_commit_pk_seq'),
    author              VARCHAR(200),
    commit_date         TIMESTAMP,
    commit_date_instant VARCHAR(30),
    commit_id           NUMERIC
);
CREATE INDEX jv_commit_commit_id_idx ON jv_commit (commit_id);

CREATE TABLE jv_commit_property (
    commit_fk      BIGINT NOT NULL,
    property_name  VARCHAR(191) NOT NULL,
    property_value VARCHAR(600),
    PRIMARY KEY (commit_fk, property_name)
);
CREATE INDEX jv_commit_property_commit_fk_idx ON jv_commit_property (commit_fk);
CREATE INDEX jv_commit_property_property_name_property_value_idx
    ON jv_commit_property (property_name, property_value);

CREATE TABLE jv_snapshot (
    snapshot_pk        BIGINT PRIMARY KEY DEFAULT nextval('jv_snapshot_pk_seq'),
    type               VARCHAR(200),
    version            BIGINT,
    state              TEXT,
    changed_properties TEXT,
    managed_type       VARCHAR(200),
    global_id_fk       BIGINT,
    commit_fk          BIGINT
);
CREATE INDEX jv_snapshot_commit_fk_idx ON jv_snapshot (commit_fk);
CREATE INDEX jv_snapshot_global_id_fk_idx ON jv_snapshot (global_id_fk);