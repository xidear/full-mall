-- 富文本详情的"译文"：与标题/描述同一套状态机（草稿→审核→发布）
ALTER TABLE product_translation ADD COLUMN detail_html TEXT;
