-- 订单与会员建立稳定关联（不再依赖手机号）
ALTER TABLE orders ADD COLUMN member_id UUID REFERENCES member(id);
CREATE INDEX orders_member_idx ON orders (member_id, created_at DESC);

-- 回填：仅当手机号在会员表中唯一匹配时才绑定，避免错绑
UPDATE orders o
   SET member_id = m.id
  FROM member m
 WHERE o.member_id IS NULL
   AND o.contact_phone = m.phone
   AND (SELECT COUNT(*) FROM member m2 WHERE m2.phone = m.phone) = 1;
