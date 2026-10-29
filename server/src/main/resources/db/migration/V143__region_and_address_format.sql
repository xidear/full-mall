-- 批次 C（ADR-0013）：区划字典 + 国别地址格式
--
-- 为什么要有两张表而不是一张：
--   region       = 某国的"区划实例"（省/市/区），只有我们真的抓了数据才有；允许为空（手填兜底）。
--   address_format = 某国的"地址形状"（必填哪些字段、什么顺序、邮编正则、行政区叫什么）——
--                   这个**必须有**，否则不知道该渲染哪些输入框，也就无法判断"这个国家有没有省市区"。
-- 为什么 level 不用 PROVINCE/CITY/DISTRICT：全球层级深度 1–4 且叫法不同（state/prefecture/emirate/county），
-- 固定三级已被 libaddressinput 的 US / Guernsey / BR 三种形状证伪（ADR-0013 §8）。

CREATE TABLE IF NOT EXISTS region (
    code           VARCHAR(16) PRIMARY KEY,
    parent_code    VARCHAR(16) REFERENCES region (code),
    -- COUNTRY=国家 / ADMIN=一级行政区(S) / LOCALITY=市(C) / DEPENDENT=区或邮编前缀区(D)
    level          VARCHAR(16) NOT NULL CHECK (level IN ('COUNTRY', 'ADMIN', 'LOCALITY', 'DEPENDENT')),
    depth          SMALLINT     NOT NULL CHECK (depth BETWEEN 1 AND 4),
    country_code   CHAR(2)      NOT NULL,
    names          JSONB        NOT NULL,
    pinyin         VARCHAR(128),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    source_id      VARCHAR(32)  NOT NULL,
    source_version VARCHAR(32)  NOT NULL,
    -- 根节点必须是国家；非根节点必须有父节点
    CONSTRAINT region_root_check CHECK ((level = 'COUNTRY') = (parent_code IS NULL))
);

CREATE INDEX IF NOT EXISTS region_lookup_idx
    ON region (country_code, parent_code, level) WHERE active;

-- 区划会变（撤县设区、新设市）：刷新时置 active=FALSE，禁止删除——历史订单/地址簿必须还能解释已存的 code
COMMENT ON COLUMN region.active IS 'FALSE=已撤销/并入其他区划，仍可被历史数据引用；列表与级联不再返回';

CREATE TABLE IF NOT EXISTS address_format (
    country_code    CHAR(2) PRIMARY KEY REFERENCES region (code),
    -- libaddressinput 的字段缩写串：N 收件人 / O 公司 / A 地址行 / D 依赖 locality / C 市 / S 州省 / Z 邮编 / X 分拣码
    required        VARCHAR(16),
    fmt             VARCHAR(64)  NOT NULL,
    -- 邮编正则；GB 的正则很长（含 BFPO 军邮分支），列宽按实际最大值留余量
    zip             VARCHAR(512),
    zipex           VARCHAR(128),
    -- 该级本地叫法（state/prefecture/emirate/county...），直接用作表单 label，不再一律写"省"
    state_name_type VARCHAR(32),
    -- 该国是否有一级行政区的封闭枚举（有才做下拉）
    has_admin_list  BOOLEAN NOT NULL,
    -- require 里没有 S 表示该级不要求填（如根西岛根本没有行政区级）
    admin_optional  BOOLEAN NOT NULL,
    upper           VARCHAR(16),
    source_id       VARCHAR(32) NOT NULL,
    source_version  VARCHAR(32) NOT NULL
);
