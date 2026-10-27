-- 页脚营业执照/经营编号文案调整（V119 已应用后新增，避免修改历史迁移导致 Flyway checksum 不一致）
UPDATE ui_translation SET text_value = '营业执照 / 经营编号', updated_at = now()
WHERE locale = 'zh-CN' AND text_key = 'footer.license';
