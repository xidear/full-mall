-- 清理 V8 迁移误插入的演示会员与优惠券（P0 已知污染）。
-- 迁移不应携带业务演示数据：prod 也会执行 V8，从而得到可直接使用的演示会员/优惠券。
-- 演示数据已迁移到 db/demo/seed.sql（仅 dev/demo profile 执行）。
-- 处理策略：未被引用则删除；被引用则显式失败，由运维决定如何处理（不静默跳过）。
DO $$
DECLARE
    member_refs BIGINT;
    coupon_refs BIGINT;
BEGIN
    SELECT COUNT(*) INTO member_refs
      FROM (
            SELECT member_id FROM pos_sale
             WHERE member_id IN ('a1000000-0000-0000-0000-000000000001',
                                 'a1000000-0000-0000-0000-000000000002')
            UNION ALL
            SELECT member_id FROM points_ledger
             WHERE member_id IN ('a1000000-0000-0000-0000-000000000001',
                                 'a1000000-0000-0000-0000-000000000002')
            UNION ALL
            SELECT member_id FROM member_card_ledger
             WHERE member_id IN ('a1000000-0000-0000-0000-000000000001',
                                 'a1000000-0000-0000-0000-000000000002')
            UNION ALL
            SELECT member_id FROM growth_ledger
             WHERE member_id IN ('a1000000-0000-0000-0000-000000000001',
                                 'a1000000-0000-0000-0000-000000000002')
           ) refs;

    SELECT COUNT(*) INTO coupon_refs
      FROM coupon_redemption
     WHERE coupon_id IN ('c1000000-0000-0000-0000-000000000001',
                         'c1000000-0000-0000-0000-000000000002');

    IF member_refs > 0 OR coupon_refs > 0 THEN
        RAISE EXCEPTION
            'V23 无法清理 V8 演示数据：演示会员被引用 % 次、演示优惠券被引用 % 次。请人工确认后再执行，不要忽略本错误。',
            member_refs, coupon_refs;
    END IF;

    DELETE FROM coupon
     WHERE id IN ('c1000000-0000-0000-0000-000000000001',
                  'c1000000-0000-0000-0000-000000000002');
    DELETE FROM member
     WHERE id IN ('a1000000-0000-0000-0000-000000000001',
                  'a1000000-0000-0000-0000-000000000002');
END $$;