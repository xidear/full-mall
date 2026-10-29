-- 分类名称与站点副标题的英文文案。分类表本身只有 name，不强制改表结构；
-- 前台按 category.<CODE> 取译名，缺失时回退数据库里的 name。
INSERT INTO ui_translation (locale, text_key, text_value) VALUES
('zh-CN', 'category.BEAUTY', '美妆个护'),
('en', 'category.BEAUTY', 'Beauty & care'),
('zh-CN', 'category.DIGITAL', '数码家电'),
('en', 'category.DIGITAL', 'Electronics'),
('zh-CN', 'category.FRESH', '生鲜食品'),
('en', 'category.FRESH', 'Fresh food'),
('zh-CN', 'category.HOME', '家居日用'),
('en', 'category.HOME', 'Home & living'),
('zh-CN', 'category.INDUSTRIAL', '工业品'),
('en', 'category.INDUSTRIAL', 'Industrial'),
('zh-CN', 'category.SPORTS', '运动户外'),
('en', 'category.SPORTS', 'Sports & outdoors'),
('zh-CN', 'site.tagline', '全品类商城'),
('en', 'site.tagline', 'All-category mall')
ON CONFLICT (locale, text_key) DO NOTHING;
