-- 分镜支持多张引导图（多角度参考 / 角色脸部定型）
ALTER TABLE ai_storyboard_shot ADD COLUMN IF NOT EXISTS guide_asset_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
UPDATE ai_storyboard_shot
   SET guide_asset_ids = jsonb_build_array(guide_asset_id)
 WHERE guide_asset_id IS NOT NULL AND guide_asset_ids = '[]'::jsonb;
COMMENT ON COLUMN ai_storyboard_shot.guide_asset_ids IS
    '引导图素材 id 列表；第一张作为首帧，其余作为参考图（SUBJECT 由前端 role 决定）';
