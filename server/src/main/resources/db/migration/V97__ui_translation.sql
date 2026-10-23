-- 多语言文案（H9）：按 locale + key 存储，缺失时回退 zh-CN
CREATE TABLE ui_translation (
    locale     VARCHAR(8) NOT NULL,
    text_key   VARCHAR(128) NOT NULL,
    text_value VARCHAR(500) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (locale, text_key)
);

INSERT INTO ui_translation (locale, text_key, text_value) VALUES
('zh-CN', 'nav.home', '首页'),
('zh-CN', 'nav.products', '全部商品'),
('zh-CN', 'nav.cart', '购物车'),
('zh-CN', 'nav.orders', '我的订单'),
('zh-CN', 'product.addToCart', '加入购物车'),
('zh-CN', 'product.outOfStock', '暂时缺货'),
('zh-CN', 'checkout.pay', '去支付'),
('zh-CN', 'review.submit', '提交评价'),
('zh-CN', 'review.pending', '评价审核中，通过后展示'),
('zh-CN', 'common.loading', '加载中'),
('en', 'nav.home', 'Home'),
('en', 'nav.products', 'Products'),
('en', 'nav.cart', 'Cart'),
('en', 'nav.orders', 'Orders'),
('en', 'product.addToCart', 'Add to cart'),
('en', 'product.outOfStock', 'Out of stock'),
('en', 'checkout.pay', 'Checkout'),
('en', 'review.submit', 'Submit review'),
('en', 'review.pending', 'Your review is under review'),
('en', 'common.loading', 'Loading')
ON CONFLICT (locale, text_key) DO NOTHING;
