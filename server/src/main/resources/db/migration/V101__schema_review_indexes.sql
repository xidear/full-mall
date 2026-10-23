-- 数据库设计整改（第 1 批）：外键列索引 + 复合索引 + 去重索引
-- 依据：pg_constraint 扫描出 45 个外键列没有索引（FK 检查与级联会全表扫描）

-- 1) theme_config 上 (channel, market_code) 同时有唯一索引与普通索引，删普通索引
DROP INDEX IF EXISTS idx_theme_config_channel;

CREATE INDEX IF NOT EXISTS fk_after_sale_line_order_line_id_idx ON after_sale_line (order_line_id);
CREATE INDEX IF NOT EXISTS fk_article_category_id_idx ON article (category_id);
CREATE INDEX IF NOT EXISTS fk_bargain_help_helper_member_id_idx ON bargain_help (helper_member_id);
CREATE INDEX IF NOT EXISTS fk_cart_item_sku_id_idx ON cart_item (sku_id);
CREATE INDEX IF NOT EXISTS fk_category_parent_id_idx ON category (parent_id);
CREATE INDEX IF NOT EXISTS fk_coupon_redemption_coupon_id_idx ON coupon_redemption (coupon_id);
CREATE INDEX IF NOT EXISTS fk_goods_receipt_supplier_id_idx ON goods_receipt (supplier_id);
CREATE INDEX IF NOT EXISTS fk_goods_receipt_line_sku_id_idx ON goods_receipt_line (sku_id);
CREATE INDEX IF NOT EXISTS fk_group_buy_promotion_id_idx ON group_buy (promotion_id);
CREATE INDEX IF NOT EXISTS fk_group_buy_sku_id_idx ON group_buy (sku_id);
CREATE INDEX IF NOT EXISTS fk_legal_document_market_code_idx ON legal_document (market_code);
CREATE INDEX IF NOT EXISTS fk_member_browse_history_sku_id_idx ON member_browse_history (sku_id);
CREATE INDEX IF NOT EXISTS fk_member_favorite_sku_id_idx ON member_favorite (sku_id);
CREATE INDEX IF NOT EXISTS fk_orders_store_id_idx ON orders (store_id);
CREATE INDEX IF NOT EXISTS fk_payment_config_version_id_idx ON payment (config_version_id);
CREATE INDEX IF NOT EXISTS fk_payment_event_payment_id_idx ON payment_event (payment_id);
CREATE INDEX IF NOT EXISTS fk_pick_task_fulfillment_order_id_idx ON pick_task (fulfillment_order_id);
CREATE INDEX IF NOT EXISTS fk_pick_task_line_order_line_id_idx ON pick_task_line (order_line_id);
CREATE INDEX IF NOT EXISTS fk_pick_task_line_suggested_batch_id_idx ON pick_task_line (suggested_batch_id);
CREATE INDEX IF NOT EXISTS fk_pickup_fulfillment_order_id_idx ON pickup (fulfillment_order_id);
CREATE INDEX IF NOT EXISTS fk_pickup_store_id_idx ON pickup (store_id);
CREATE INDEX IF NOT EXISTS fk_pos_refund_allocation_batch_id_idx ON pos_refund_allocation (batch_id);
CREATE INDEX IF NOT EXISTS fk_pos_refund_tender_tender_id_idx ON pos_refund_tender (tender_id);
CREATE INDEX IF NOT EXISTS fk_pos_sale_coupon_id_idx ON pos_sale (coupon_id);
CREATE INDEX IF NOT EXISTS fk_pos_sale_member_id_idx ON pos_sale (member_id);
CREATE INDEX IF NOT EXISTS fk_pos_sale_allocation_batch_id_idx ON pos_sale_allocation (batch_id);
CREATE INDEX IF NOT EXISTS fk_product_category_id_idx ON product (category_id);
CREATE INDEX IF NOT EXISTS fk_product_review_member_id_idx ON product_review (member_id);
CREATE INDEX IF NOT EXISTS fk_purchase_order_supplier_id_idx ON purchase_order (supplier_id);
CREATE INDEX IF NOT EXISTS fk_purchase_order_line_sku_id_idx ON purchase_order_line (sku_id);
CREATE INDEX IF NOT EXISTS fk_reservation_batch_id_idx ON reservation (batch_id);
CREATE INDEX IF NOT EXISTS fk_role_permission_permission_code_idx ON role_permission (permission_code);
CREATE INDEX IF NOT EXISTS fk_sku_option_value_option_id_idx ON sku_option_value (option_id);
CREATE INDEX IF NOT EXISTS fk_sku_serial_order_line_id_idx ON sku_serial (order_line_id);
CREATE INDEX IF NOT EXISTS fk_staff_store_store_id_idx ON staff_store (store_id);
CREATE INDEX IF NOT EXISTS fk_stock_transfer_allocation_source_batch_id_idx ON stock_transfer_allocation (source_batch_id);
CREATE INDEX IF NOT EXISTS fk_stock_transfer_line_sku_id_idx ON stock_transfer_line (sku_id);
CREATE INDEX IF NOT EXISTS fk_stocktake_line_batch_id_idx ON stocktake_line (batch_id);
CREATE INDEX IF NOT EXISTS fk_stocktake_line_sku_id_idx ON stocktake_line (sku_id);
CREATE INDEX IF NOT EXISTS fk_store_market_code_idx ON store (market_code);
CREATE INDEX IF NOT EXISTS fk_supplier_return_order_id_idx ON supplier_return (order_id);
CREATE INDEX IF NOT EXISTS fk_supplier_sku_sku_id_idx ON supplier_sku (sku_id);
CREATE INDEX IF NOT EXISTS fk_supplier_stock_feed_sku_id_idx ON supplier_stock_feed (sku_id);
CREATE INDEX IF NOT EXISTS fk_warehouse_market_code_idx ON warehouse (market_code);
CREATE INDEX IF NOT EXISTS fk_webhook_delivery_webhook_id_idx ON webhook_delivery (webhook_id);

-- 2) 高频查询的复合索引（订单深页 keyset / 评价审核 / 审计倒序）
CREATE INDEX IF NOT EXISTS orders_status_created_id_idx
    ON orders (status, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS product_review_status_created_idx
    ON product_review (status, created_at DESC);
CREATE INDEX IF NOT EXISTS audit_log_occurred_idx
    ON audit_log (occurred_at DESC);

-- 3) 券发放与核销的常用查询
CREATE INDEX IF NOT EXISTS member_coupon_member_status_idx
    ON member_coupon (member_id, status, expires_at);
CREATE INDEX IF NOT EXISTS coupon_redemption_coupon_created_idx
    ON coupon_redemption (coupon_id, created_at DESC);
