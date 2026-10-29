-- 评价晒图/晒视频：顾客上传的媒体 URL（不入素材库，不污染运营的商品素材）。
-- 影响表：product_review（现有行取默认空数组，无需回填）。数据量级：与评价条数同阶。
-- 可回滚：DROP COLUMN media_urls（前端只是少显示几张图）。
-- 上线顺序：先上本迁移，再上允许 mediaUrls 的后端与前台（旧版本客户端不受影响）。
ALTER TABLE product_review ADD COLUMN media_urls text[] NOT NULL DEFAULT '{}';

-- 条数上限是最后防线：接口层按 ValidationRules.REVIEW_MEDIA_MAX 校验，
-- 这里防止绕过接口的写入（批量导入、手工 SQL）把列表页拖垮。
ALTER TABLE product_review
    ADD CONSTRAINT product_review_media_urls_check CHECK (cardinality(media_urls) <= 9);
