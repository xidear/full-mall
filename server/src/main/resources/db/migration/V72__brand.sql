-- 品牌表：product.brand_id 一直存在但没有主表，品牌范围（促销/券）与品牌筛选都无法使用
CREATE TABLE brand (
    id         UUID PRIMARY KEY,
    code       VARCHAR(64) NOT NULL UNIQUE,
    name       VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 演示品牌（幂等）：让范围选择器与筛选有可用数据
INSERT INTO brand (id, code, name, sort_order) VALUES
    ('bb000000-0000-0000-0000-000000000001', 'BRAND-DEMO-1', '演示品牌甲', 10),
    ('bb000000-0000-0000-0000-000000000002', 'BRAND-DEMO-2', '演示品牌乙', 20)
ON CONFLICT (id) DO NOTHING;
