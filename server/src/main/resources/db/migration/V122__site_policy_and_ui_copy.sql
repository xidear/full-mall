-- 多地区 / 多模板前台所需的站点策略与文案。
-- 关键点：地区（market）与版权声明必须是配置，而不是写死在页面里。
INSERT INTO system_setting (setting_key, setting_value) VALUES
('default_market', 'CN'),
('site_tagline', '全品类商城'),
('branding_attribution', 'SHOW')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO ui_translation (locale, text_key, text_value) VALUES
('zh-CN', 'footer.note.prod', '价格含税 · 门店自提 · 七天无理由'),
('en', 'footer.note.prod', 'Tax included · Store pickup · 7-day returns'),
('zh-CN', 'checkout.login.title', '登录后继续结算'),
('en', 'checkout.login.title', 'Sign in to continue'),
('zh-CN', 'checkout.login.desc', '登录或注册后将回到本页，购物车和填写内容都会保留。'),
('en', 'checkout.login.desc', 'Sign in or register and you will return here. Your cart and entries are kept.'),
('zh-CN', 'checkout.account.auto', '未注册的手机号验证后将自动创建账号，并同时同意《服务条款》与《隐私政策》。'),
('en', 'checkout.account.auto', 'A new account is created automatically after phone verification, and you agree to the Terms and Privacy Policy.'),
('zh-CN', 'footer.attribution', 'Powered by Full Mall'),
('en', 'footer.attribution', 'Powered by Full Mall')
ON CONFLICT (locale, text_key) DO NOTHING;
