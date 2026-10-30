-- 基线 seed：权限字典/角色/区划/默认设置等写在迁移里的数据。
-- session_replication_role=replica：跳过外键检查，否则 region 的自引用外键会因插入顺序报错。
SET session_replication_role = replica;





INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01', 'CN', 'ADMIN', 2, 'CN', '{"zh": "安徽省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-02', 'CN', 'ADMIN', 2, 'CN', '{"zh": "澳门"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03', 'CN', 'ADMIN', 2, 'CN', '{"zh": "北京市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04', 'CN', 'ADMIN', 2, 'CN', '{"zh": "重庆市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05', 'CN', 'ADMIN', 2, 'CN', '{"zh": "福建省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06', 'CN', 'ADMIN', 2, 'CN', '{"zh": "甘肃省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07', 'CN', 'ADMIN', 2, 'CN', '{"zh": "广东省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08', 'CN', 'ADMIN', 2, 'CN', '{"zh": "广西"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09', 'CN', 'ADMIN', 2, 'CN', '{"zh": "贵州省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10', 'CN', 'ADMIN', 2, 'CN', '{"zh": "海南省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11', 'CN', 'ADMIN', 2, 'CN', '{"zh": "河北省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12', 'CN', 'ADMIN', 2, 'CN', '{"zh": "河南省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13', 'CN', 'ADMIN', 2, 'CN', '{"zh": "黑龙江省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14', 'CN', 'ADMIN', 2, 'CN', '{"zh": "湖北省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15', 'CN', 'ADMIN', 2, 'CN', '{"zh": "湖南省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16', 'CN', 'ADMIN', 2, 'CN', '{"zh": "吉林省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17', 'CN', 'ADMIN', 2, 'CN', '{"zh": "江苏省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18', 'CN', 'ADMIN', 2, 'CN', '{"zh": "江西省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19', 'CN', 'ADMIN', 2, 'CN', '{"zh": "辽宁省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20', 'CN', 'ADMIN', 2, 'CN', '{"zh": "内蒙古"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21', 'CN', 'ADMIN', 2, 'CN', '{"zh": "宁夏"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22', 'CN', 'ADMIN', 2, 'CN', '{"zh": "青海省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23', 'CN', 'ADMIN', 2, 'CN', '{"zh": "山东省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24', 'CN', 'ADMIN', 2, 'CN', '{"zh": "山西省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25', 'CN', 'ADMIN', 2, 'CN', '{"zh": "陕西省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26', 'CN', 'ADMIN', 2, 'CN', '{"zh": "上海市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27', 'CN', 'ADMIN', 2, 'CN', '{"zh": "四川省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28', 'CN', 'ADMIN', 2, 'CN', '{"zh": "台湾"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29', 'CN', 'ADMIN', 2, 'CN', '{"zh": "天津市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30', 'CN', 'ADMIN', 2, 'CN', '{"zh": "西藏"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-31', 'CN', 'ADMIN', 2, 'CN', '{"zh": "香港"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32', 'CN', 'ADMIN', 2, 'CN', '{"zh": "新疆"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33', 'CN', 'ADMIN', 2, 'CN', '{"zh": "云南省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34', 'CN', 'ADMIN', 2, 'CN', '{"zh": "浙江省"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "安庆市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-02', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "蚌埠市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-03', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "亳州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-04', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "巢湖市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-05', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "池州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-06', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "滁州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-07', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "阜阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-08', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "合肥市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-09', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "淮北市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-10', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "淮南市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-11', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "黄山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-12', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "六安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-13', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "马鞍山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-14', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "宿州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-15', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "铜陵市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-16', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "芜湖市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-17', 'CN-01', 'LOCALITY', 3, 'CN', '{"zh": "宣城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-02-01', 'CN-02', 'LOCALITY', 3, 'CN', '{"zh": "澳门"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-01', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "昌平区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-02', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "朝阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-03', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "大兴区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-04', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "东城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-05', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "房山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-06', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "丰台区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-07', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "海淀区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-08', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "怀柔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-09', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "门头沟区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-10', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "密云县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-11', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "平谷区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-12', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "石景山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-13', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "顺义区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-14', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "通州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-15', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "西城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-03-16', 'CN-03', 'LOCALITY', 3, 'CN', '{"zh": "延庆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-01', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "巴南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-02', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "北碚区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-03', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "璧山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-04', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "城口县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-05', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "大渡口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-06', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "大足区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-07', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "垫江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-08', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "丰都县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-09', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "奉节县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-10', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "涪陵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-11', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "合川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-12', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "江北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-13', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "江津区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-14', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "九龙坡区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-15', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "开县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-16', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "梁平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-17', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "南岸区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-18', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "南川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-19', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "彭水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-20', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "綦江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-21', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "黔江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-22', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "荣昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-23', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "沙坪坝区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-24', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "石柱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-25', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "铜梁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-26', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "潼南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-27', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "万州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-28', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "巫山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-29', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "巫溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-30', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "武隆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-31', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "秀山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-32', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "永川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-33', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "酉阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-34', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "渝北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-35', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "渝中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-36', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "云阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-37', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "长寿区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-04-38', 'CN-04', 'LOCALITY', 3, 'CN', '{"zh": "忠县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01', 'CN-05', 'LOCALITY', 3, 'CN', '{"zh": "福州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-02', 'CN-05', 'LOCALITY', 3, 'CN', '{"zh": "龙岩市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03', 'CN-05', 'LOCALITY', 3, 'CN', '{"zh": "南平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04', 'CN-05', 'LOCALITY', 3, 'CN', '{"zh": "宁德市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-05', 'CN-05', 'LOCALITY', 3, 'CN', '{"zh": "莆田市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06', 'CN-05', 'LOCALITY', 3, 'CN', '{"zh": "泉州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07', 'CN-05', 'LOCALITY', 3, 'CN', '{"zh": "三明市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-08', 'CN-05', 'LOCALITY', 3, 'CN', '{"zh": "厦门市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09', 'CN-05', 'LOCALITY', 3, 'CN', '{"zh": "漳州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-01', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "白银市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-02', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "定西市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-03', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "甘南州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-04', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "嘉峪关市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-05', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "金昌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-06', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "酒泉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-07', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "兰州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-08', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "临夏州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "陇南市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-10', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "平凉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-11', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "庆阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-12', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "天水市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-13', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "武威市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-14', 'CN-06', 'LOCALITY', 3, 'CN', '{"zh": "张掖市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-01', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "潮州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-02', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "东莞市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-03', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "佛山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "广州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-05', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "河源市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-06', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "惠州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-07', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "江门市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-08', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "揭阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-09', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "茂名市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-10', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "梅州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-11', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "清远市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-12', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "汕头市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-13', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "汕尾市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "韶关市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "深圳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-16', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "阳江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-17', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "云浮市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "湛江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-19', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "肇庆市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-20', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "中山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-21', 'CN-07', 'LOCALITY', 3, 'CN', '{"zh": "珠海市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "百色市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-02', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "北海市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-03', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "崇左市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-04', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "防城港市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-05', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "贵港市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "桂林市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "河池市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-08', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "贺州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-09', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "来宾市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "柳州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "南宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-12', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "钦州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-13', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "梧州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-14', 'CN-08', 'LOCALITY', 3, 'CN', '{"zh": "玉林市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-01', 'CN-09', 'LOCALITY', 3, 'CN', '{"zh": "安顺市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-02', 'CN-09', 'LOCALITY', 3, 'CN', '{"zh": "毕节地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03', 'CN-09', 'LOCALITY', 3, 'CN', '{"zh": "贵阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-04', 'CN-09', 'LOCALITY', 3, 'CN', '{"zh": "六盘水市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05', 'CN-09', 'LOCALITY', 3, 'CN', '{"zh": "黔东南州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06', 'CN-09', 'LOCALITY', 3, 'CN', '{"zh": "黔南州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-07', 'CN-09', 'LOCALITY', 3, 'CN', '{"zh": "黔西南州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08', 'CN-09', 'LOCALITY', 3, 'CN', '{"zh": "铜仁地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09', 'CN-09', 'LOCALITY', 3, 'CN', '{"zh": "遵义市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-01', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "白沙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-02', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "保亭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-03', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "昌江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-04', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "澄迈县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-05', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "儋州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-06', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "定安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-07', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "东方市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-08', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "海口市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-09', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "乐东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-10', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "临高县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-11', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "陵水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-12', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "琼海市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-13', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "琼中县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-14', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "三沙市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-15', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "三亚市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-16', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "屯昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-17', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "万宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-18', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "文昌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-19', 'CN-10', 'LOCALITY', 3, 'CN', '{"zh": "五指山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "保定市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "沧州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "承德市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "邯郸市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "衡水市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "廊坊市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-07', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "秦皇岛市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "石家庄市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "唐山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "邢台市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11', 'CN-11', 'LOCALITY', 3, 'CN', '{"zh": "张家口市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "安阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-02', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "鹤壁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "焦作市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "开封市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "洛阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-06', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "漯河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "南阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "平顶山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-09', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "濮阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-10', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "三门峡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "商丘市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "新乡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "信阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-14', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "许昌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "郑州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "周口市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17', 'CN-12', 'LOCALITY', 3, 'CN', '{"zh": "驻马店市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "大庆市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-02', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "大兴安岭地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "哈尔滨市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-04', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "鹤岗市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-05', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "黑河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "鸡西市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "佳木斯市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "牡丹江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-09', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "七台河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "齐齐哈尔市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-11', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "双鸭山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "绥化市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13', 'CN-13', 'LOCALITY', 3, 'CN', '{"zh": "伊春市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-01', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "鄂州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-02', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "恩施州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "黄冈市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-04', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "黄石市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-05', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "荆门市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-06', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "荆州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-07', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "潜江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-08', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "神农架林区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-09', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "十堰市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-10', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "随州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-11', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "天门市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "武汉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-13', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "仙桃市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-14', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "咸宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "襄阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-16', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "孝感市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17', 'CN-14', 'LOCALITY', 3, 'CN', '{"zh": "宜昌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "常德市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "郴州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "衡阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "怀化市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-05', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "娄底市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "邵阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-07', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "湘潭市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-08', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "湘西州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-09', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "益阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "永州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "岳阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-12', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "张家界市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "长沙市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14', 'CN-15', 'LOCALITY', 3, 'CN', '{"zh": "株洲市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-01', 'CN-16', 'LOCALITY', 3, 'CN', '{"zh": "白城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-02', 'CN-16', 'LOCALITY', 3, 'CN', '{"zh": "白山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03', 'CN-16', 'LOCALITY', 3, 'CN', '{"zh": "吉林市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-04', 'CN-16', 'LOCALITY', 3, 'CN', '{"zh": "辽源市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-05', 'CN-16', 'LOCALITY', 3, 'CN', '{"zh": "四平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-06', 'CN-16', 'LOCALITY', 3, 'CN', '{"zh": "松原市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-07', 'CN-16', 'LOCALITY', 3, 'CN', '{"zh": "通化市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-08', 'CN-16', 'LOCALITY', 3, 'CN', '{"zh": "延边州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09', 'CN-16', 'LOCALITY', 3, 'CN', '{"zh": "长春市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-01', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "常州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-02', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "淮安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-03', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "连云港市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "南京市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-05', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "南通市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "苏州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-07', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "宿迁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-08', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "泰州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "无锡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "徐州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "盐城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-12', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "扬州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-13', 'CN-17', 'LOCALITY', 3, 'CN', '{"zh": "镇江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "抚州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "赣州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "吉安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-04', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "景德镇市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "九江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "南昌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-07', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "萍乡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "上饶市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-09', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "新余市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "宜春市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-11', 'CN-18', 'LOCALITY', 3, 'CN', '{"zh": "鹰潭市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-01', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "鞍山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-02', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "本溪市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-03', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "朝阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "大连市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-05', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "丹东市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-06', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "抚顺市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-07', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "阜新市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-08', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "葫芦岛市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-09', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "锦州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-10', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "辽阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-11', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "盘锦市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "沈阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-13', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "铁岭市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-14', 'CN-19', 'LOCALITY', 3, 'CN', '{"zh": "营口市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-01', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "阿拉善盟"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-02', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "巴彦淖尔市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "包头市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "赤峰市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-05', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "鄂尔多斯市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "呼和浩特市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "呼伦贝尔市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-08', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "通辽市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-09', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "乌海市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "乌兰察布市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "锡林郭勒盟"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-12', 'CN-20', 'LOCALITY', 3, 'CN', '{"zh": "兴安盟"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-01', 'CN-21', 'LOCALITY', 3, 'CN', '{"zh": "固原市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-02', 'CN-21', 'LOCALITY', 3, 'CN', '{"zh": "石嘴山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-03', 'CN-21', 'LOCALITY', 3, 'CN', '{"zh": "吴忠市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-04', 'CN-21', 'LOCALITY', 3, 'CN', '{"zh": "银川市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-05', 'CN-21', 'LOCALITY', 3, 'CN', '{"zh": "中卫市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-01', 'CN-22', 'LOCALITY', 3, 'CN', '{"zh": "果洛州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-02', 'CN-22', 'LOCALITY', 3, 'CN', '{"zh": "海北州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-03', 'CN-22', 'LOCALITY', 3, 'CN', '{"zh": "海东市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-04', 'CN-22', 'LOCALITY', 3, 'CN', '{"zh": "海南州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-05', 'CN-22', 'LOCALITY', 3, 'CN', '{"zh": "海西州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-06', 'CN-22', 'LOCALITY', 3, 'CN', '{"zh": "黄南州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-07', 'CN-22', 'LOCALITY', 3, 'CN', '{"zh": "西宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-08', 'CN-22', 'LOCALITY', 3, 'CN', '{"zh": "玉树州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-01', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "滨州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "德州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-03', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "东营市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "菏泽市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "济南市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "济宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-07', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "莱芜市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-08', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "聊城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "临沂市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "青岛市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-11', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "日照市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-12', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "泰安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-13', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "威海市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "潍坊市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "烟台市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-16', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "枣庄市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-17', 'CN-23', 'LOCALITY', 3, 'CN', '{"zh": "淄博市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "大同市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-02', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "晋城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "晋中市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "临汾市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "吕梁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-06', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "朔州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "太原市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "忻州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-09', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "阳泉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "运城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11', 'CN-24', 'LOCALITY', 3, 'CN', '{"zh": "长治市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "安康市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "宝鸡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "汉中市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-04', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "商洛市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-05', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "铜川市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "渭南市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "西安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "咸阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "延安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10', 'CN-25', 'LOCALITY', 3, 'CN', '{"zh": "榆林市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-01', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "宝山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-02', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "崇明县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-03', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "奉贤区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-04', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "虹口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-05', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "黄浦区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-06', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "嘉定区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-07', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "金山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-08', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "静安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-09', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "闵行区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-10', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "浦东新区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-11', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "普陀区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-12', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "青浦区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-13', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "松江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-14', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "徐汇区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-15', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "杨浦区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-16', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "闸北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-26-17', 'CN-26', 'LOCALITY', 3, 'CN', '{"zh": "长宁区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "阿坝州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-02', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "巴中市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "成都市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-04', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "达州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-05', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "德阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "甘孜州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-07', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "广安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-08', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "广元市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "乐山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "凉山州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-11', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "泸州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-12', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "眉山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "绵阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "南充市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-15', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "内江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-16', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "攀枝花市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-17', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "遂宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-18', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "雅安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "宜宾市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-20', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "资阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-21', 'CN-27', 'LOCALITY', 3, 'CN', '{"zh": "自贡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "高雄市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "花蓮縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-03', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "基隆市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-04', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "嘉義市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "嘉義縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-06', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "金門縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-07', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "連江縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "苗栗縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "南投縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-10', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "澎湖縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "屏東縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "台北市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "台東縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "台南市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "台中市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "桃園市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "新北市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-18', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "新竹市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "新竹縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "宜蘭縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "雲林縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22', 'CN-28', 'LOCALITY', 3, 'CN', '{"zh": "彰化縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-01', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "宝坻区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-02', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "北辰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-03', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "滨海新区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-04', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "东丽区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-05', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "和平区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-06', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "河北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-07', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "河东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-08', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "河西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-09', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "红桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-10', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "蓟县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-11', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "津南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-12', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "静海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-13', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "南开区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-14', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "宁河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-15', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "武清区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-29-16', 'CN-29', 'LOCALITY', 3, 'CN', '{"zh": "西青区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-01', 'CN-30', 'LOCALITY', 3, 'CN', '{"zh": "阿里地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02', 'CN-30', 'LOCALITY', 3, 'CN', '{"zh": "昌都地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-03', 'CN-30', 'LOCALITY', 3, 'CN', '{"zh": "拉萨市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-04', 'CN-30', 'LOCALITY', 3, 'CN', '{"zh": "林芝地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05', 'CN-30', 'LOCALITY', 3, 'CN', '{"zh": "那曲地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06', 'CN-30', 'LOCALITY', 3, 'CN', '{"zh": "日喀则地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07', 'CN-30', 'LOCALITY', 3, 'CN', '{"zh": "山南地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-31-01', 'CN-31', 'LOCALITY', 3, 'CN', '{"zh": "九龍"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-31-02', 'CN-31', 'LOCALITY', 3, 'CN', '{"zh": "香港島"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-31-03', 'CN-31', 'LOCALITY', 3, 'CN', '{"zh": "新界"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "阿克苏地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-02', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "阿拉尔市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-03', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "阿勒泰地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "巴音郭楞州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-05', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "博尔塔拉州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-06', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "昌吉州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-07', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "哈密地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-08', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "和田地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "喀什地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-10', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "克拉玛依市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-11', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "克孜勒苏州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-12', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "石河子市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-13', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "塔城地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-14', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "图木舒克市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-15', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "吐鲁番地区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-16', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "乌鲁木齐市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-17', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "五家渠市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18', 'CN-32', 'LOCALITY', 3, 'CN', '{"zh": "伊犁州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-01', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "保山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "楚雄州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "大理州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-04', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "德宏州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-05', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "迪庆州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "红河州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "昆明市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-08', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "丽江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-09', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "临沧市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-10', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "怒江州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "普洱市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "曲靖市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-13', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "文山州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-14', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "西双版纳州"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "玉溪市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16', 'CN-33', 'LOCALITY', 3, 'CN', '{"zh": "昭通市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "杭州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-02', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "湖州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-03', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "嘉兴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "金华市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "丽水市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "宁波市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-07', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "衢州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-08', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "绍兴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "台州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "温州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-11', 'CN-34', 'LOCALITY', 3, 'CN', '{"zh": "舟山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('HK-01', 'HK', 'ADMIN', 2, 'HK', '{"zh-Hant": "九龍"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('HK-02', 'HK', 'ADMIN', 2, 'HK', '{"zh-Hant": "香港島"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('HK-03', 'HK', 'ADMIN', 2, 'HK', '{"zh-Hant": "新界"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-01', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "台中市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-02', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "台北市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-03', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "台東縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-04', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "台南市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-05', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "宜蘭縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-06', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "花蓮縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-07', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "金門縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-08', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "南投縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-09', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "屏東縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-10', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "苗栗縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-11', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "桃園市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-12', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "高雄市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-13', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "基隆市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-14', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "連江縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-15', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "雲林縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-16', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "新北市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MO', NULL, 'COUNTRY', 1, 'MO', '{"en": "MACAO", "zh-CN": "中国澳门"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-17', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "新竹市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-18', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "新竹縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-19', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "嘉義市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-20', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "嘉義縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-21', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "彰化縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW-22', 'TW', 'ADMIN', 2, 'TW', '{"zh-Hant": "澎湖縣"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-01', 'JP', 'ADMIN', 2, 'JP', '{"ja": "北海道"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-02', 'JP', 'ADMIN', 2, 'JP', '{"ja": "青森県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-03', 'JP', 'ADMIN', 2, 'JP', '{"ja": "岩手県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-04', 'JP', 'ADMIN', 2, 'JP', '{"ja": "宮城県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-05', 'JP', 'ADMIN', 2, 'JP', '{"ja": "秋田県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-06', 'JP', 'ADMIN', 2, 'JP', '{"ja": "山形県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-07', 'JP', 'ADMIN', 2, 'JP', '{"ja": "福島県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-08', 'JP', 'ADMIN', 2, 'JP', '{"ja": "茨城県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-09', 'JP', 'ADMIN', 2, 'JP', '{"ja": "栃木県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-10', 'JP', 'ADMIN', 2, 'JP', '{"ja": "群馬県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-11', 'JP', 'ADMIN', 2, 'JP', '{"ja": "埼玉県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-12', 'JP', 'ADMIN', 2, 'JP', '{"ja": "千葉県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-13', 'JP', 'ADMIN', 2, 'JP', '{"ja": "東京都"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-14', 'JP', 'ADMIN', 2, 'JP', '{"ja": "神奈川県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-15', 'JP', 'ADMIN', 2, 'JP', '{"ja": "新潟県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-16', 'JP', 'ADMIN', 2, 'JP', '{"ja": "富山県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-17', 'JP', 'ADMIN', 2, 'JP', '{"ja": "石川県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-18', 'JP', 'ADMIN', 2, 'JP', '{"ja": "福井県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-19', 'JP', 'ADMIN', 2, 'JP', '{"ja": "山梨県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-20', 'JP', 'ADMIN', 2, 'JP', '{"ja": "長野県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-21', 'JP', 'ADMIN', 2, 'JP', '{"ja": "岐阜県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-22', 'JP', 'ADMIN', 2, 'JP', '{"ja": "静岡県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-23', 'JP', 'ADMIN', 2, 'JP', '{"ja": "愛知県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-24', 'JP', 'ADMIN', 2, 'JP', '{"ja": "三重県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-25', 'JP', 'ADMIN', 2, 'JP', '{"ja": "滋賀県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-26', 'JP', 'ADMIN', 2, 'JP', '{"ja": "京都府"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-27', 'JP', 'ADMIN', 2, 'JP', '{"ja": "大阪府"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-28', 'JP', 'ADMIN', 2, 'JP', '{"ja": "兵庫県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-29', 'JP', 'ADMIN', 2, 'JP', '{"ja": "奈良県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-30', 'JP', 'ADMIN', 2, 'JP', '{"ja": "和歌山県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-31', 'JP', 'ADMIN', 2, 'JP', '{"ja": "鳥取県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-32', 'JP', 'ADMIN', 2, 'JP', '{"ja": "島根県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-33', 'JP', 'ADMIN', 2, 'JP', '{"ja": "岡山県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-34', 'JP', 'ADMIN', 2, 'JP', '{"ja": "広島県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-35', 'JP', 'ADMIN', 2, 'JP', '{"ja": "山口県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-36', 'JP', 'ADMIN', 2, 'JP', '{"ja": "徳島県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-37', 'JP', 'ADMIN', 2, 'JP', '{"ja": "香川県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-38', 'JP', 'ADMIN', 2, 'JP', '{"ja": "愛媛県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-39', 'JP', 'ADMIN', 2, 'JP', '{"ja": "高知県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-40', 'JP', 'ADMIN', 2, 'JP', '{"ja": "福岡県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-41', 'JP', 'ADMIN', 2, 'JP', '{"ja": "佐賀県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-42', 'JP', 'ADMIN', 2, 'JP', '{"ja": "長崎県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-43', 'JP', 'ADMIN', 2, 'JP', '{"ja": "熊本県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-44', 'JP', 'ADMIN', 2, 'JP', '{"ja": "大分県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-45', 'JP', 'ADMIN', 2, 'JP', '{"ja": "宮崎県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-46', 'JP', 'ADMIN', 2, 'JP', '{"ja": "鹿児島県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP-47', 'JP', 'ADMIN', 2, 'JP', '{"ja": "沖縄県"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-01', 'US', 'ADMIN', 2, 'US', '{"en": "Alabama"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-02', 'US', 'ADMIN', 2, 'US', '{"en": "Alaska"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-03', 'US', 'ADMIN', 2, 'US', '{"en": "American Samoa"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-04', 'US', 'ADMIN', 2, 'US', '{"en": "Arizona"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-05', 'US', 'ADMIN', 2, 'US', '{"en": "Arkansas"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-06', 'US', 'ADMIN', 2, 'US', '{"en": "Armed Forces (AA)"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-07', 'US', 'ADMIN', 2, 'US', '{"en": "Armed Forces (AE)"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-08', 'US', 'ADMIN', 2, 'US', '{"en": "Armed Forces (AP)"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-09', 'US', 'ADMIN', 2, 'US', '{"en": "California"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-10', 'US', 'ADMIN', 2, 'US', '{"en": "Colorado"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-11', 'US', 'ADMIN', 2, 'US', '{"en": "Connecticut"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-12', 'US', 'ADMIN', 2, 'US', '{"en": "Delaware"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-13', 'US', 'ADMIN', 2, 'US', '{"en": "District of Columbia"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-14', 'US', 'ADMIN', 2, 'US', '{"en": "Florida"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-15', 'US', 'ADMIN', 2, 'US', '{"en": "Georgia"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-16', 'US', 'ADMIN', 2, 'US', '{"en": "Guam"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-17', 'US', 'ADMIN', 2, 'US', '{"en": "Hawaii"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-18', 'US', 'ADMIN', 2, 'US', '{"en": "Idaho"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-19', 'US', 'ADMIN', 2, 'US', '{"en": "Illinois"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-20', 'US', 'ADMIN', 2, 'US', '{"en": "Indiana"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-21', 'US', 'ADMIN', 2, 'US', '{"en": "Iowa"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-22', 'US', 'ADMIN', 2, 'US', '{"en": "Kansas"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-23', 'US', 'ADMIN', 2, 'US', '{"en": "Kentucky"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-24', 'US', 'ADMIN', 2, 'US', '{"en": "Louisiana"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-25', 'US', 'ADMIN', 2, 'US', '{"en": "Maine"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US', NULL, 'COUNTRY', 1, 'US', '{"en": "UNITED STATES", "zh-CN": "美国"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-26', 'US', 'ADMIN', 2, 'US', '{"en": "Marshall Islands"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-27', 'US', 'ADMIN', 2, 'US', '{"en": "Maryland"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-28', 'US', 'ADMIN', 2, 'US', '{"en": "Massachusetts"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-29', 'US', 'ADMIN', 2, 'US', '{"en": "Michigan"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-30', 'US', 'ADMIN', 2, 'US', '{"en": "Micronesia"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-31', 'US', 'ADMIN', 2, 'US', '{"en": "Minnesota"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-32', 'US', 'ADMIN', 2, 'US', '{"en": "Mississippi"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-33', 'US', 'ADMIN', 2, 'US', '{"en": "Missouri"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-34', 'US', 'ADMIN', 2, 'US', '{"en": "Montana"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-35', 'US', 'ADMIN', 2, 'US', '{"en": "Nebraska"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-36', 'US', 'ADMIN', 2, 'US', '{"en": "Nevada"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-37', 'US', 'ADMIN', 2, 'US', '{"en": "New Hampshire"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-38', 'US', 'ADMIN', 2, 'US', '{"en": "New Jersey"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-39', 'US', 'ADMIN', 2, 'US', '{"en": "New Mexico"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-40', 'US', 'ADMIN', 2, 'US', '{"en": "New York"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-41', 'US', 'ADMIN', 2, 'US', '{"en": "North Carolina"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-42', 'US', 'ADMIN', 2, 'US', '{"en": "North Dakota"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-43', 'US', 'ADMIN', 2, 'US', '{"en": "Northern Mariana Islands"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-44', 'US', 'ADMIN', 2, 'US', '{"en": "Ohio"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-45', 'US', 'ADMIN', 2, 'US', '{"en": "Oklahoma"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-46', 'US', 'ADMIN', 2, 'US', '{"en": "Oregon"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-47', 'US', 'ADMIN', 2, 'US', '{"en": "Palau"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-48', 'US', 'ADMIN', 2, 'US', '{"en": "Pennsylvania"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-49', 'US', 'ADMIN', 2, 'US', '{"en": "Puerto Rico"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-50', 'US', 'ADMIN', 2, 'US', '{"en": "Rhode Island"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-51', 'US', 'ADMIN', 2, 'US', '{"en": "South Carolina"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-52', 'US', 'ADMIN', 2, 'US', '{"en": "South Dakota"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-53', 'US', 'ADMIN', 2, 'US', '{"en": "Tennessee"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-54', 'US', 'ADMIN', 2, 'US', '{"en": "Texas"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-55', 'US', 'ADMIN', 2, 'US', '{"en": "Utah"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-56', 'US', 'ADMIN', 2, 'US', '{"en": "Vermont"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-57', 'US', 'ADMIN', 2, 'US', '{"en": "Virgin Islands"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-58', 'US', 'ADMIN', 2, 'US', '{"en": "Virginia"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-59', 'US', 'ADMIN', 2, 'US', '{"en": "Washington"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-60', 'US', 'ADMIN', 2, 'US', '{"en": "West Virginia"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-61', 'US', 'ADMIN', 2, 'US', '{"en": "Wisconsin"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('US-62', 'US', 'ADMIN', 2, 'US', '{"en": "Wyoming"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-01', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Johor"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-02', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Kedah"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-03', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Kelantan"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-04', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Kuala Lumpur"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-05', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Labuan"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-06', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Melaka"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-07', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Negeri Sembilan"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-08', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Pahang"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-09', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Perak"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-10', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Perlis"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-11', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Pulau Pinang"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-12', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Putrajaya"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-13', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Sabah"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-14', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Sarawak"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-15', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Selangor"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY-16', 'MY', 'ADMIN', 2, 'MY', '{"ms": "Terengganu"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-01', 'TH', 'ADMIN', 2, 'TH', '{"th": "กระบี่"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-02', 'TH', 'ADMIN', 2, 'TH', '{"th": "กรุงเทพมหานคร"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-03', 'TH', 'ADMIN', 2, 'TH', '{"th": "กาญจนบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-04', 'TH', 'ADMIN', 2, 'TH', '{"th": "กาฬสินธุ์"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-05', 'TH', 'ADMIN', 2, 'TH', '{"th": "กำแพงเพชร"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-06', 'TH', 'ADMIN', 2, 'TH', '{"th": "ขอนแก่น"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-07', 'TH', 'ADMIN', 2, 'TH', '{"th": "จังหวัด บึงกาฬ"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-08', 'TH', 'ADMIN', 2, 'TH', '{"th": "จันทบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-09', 'TH', 'ADMIN', 2, 'TH', '{"th": "ฉะเชิงเทรา"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-10', 'TH', 'ADMIN', 2, 'TH', '{"th": "ชลบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-11', 'TH', 'ADMIN', 2, 'TH', '{"th": "ชัยนาท"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-12', 'TH', 'ADMIN', 2, 'TH', '{"th": "ชัยภูมิ"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-13', 'TH', 'ADMIN', 2, 'TH', '{"th": "ชุมพร"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-14', 'TH', 'ADMIN', 2, 'TH', '{"th": "เชียงราย"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-15', 'TH', 'ADMIN', 2, 'TH', '{"th": "เชียงใหม่"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-16', 'TH', 'ADMIN', 2, 'TH', '{"th": "ตรัง"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-17', 'TH', 'ADMIN', 2, 'TH', '{"th": "ตราด"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-18', 'TH', 'ADMIN', 2, 'TH', '{"th": "ตาก"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-19', 'TH', 'ADMIN', 2, 'TH', '{"th": "นครนายก"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-20', 'TH', 'ADMIN', 2, 'TH', '{"th": "นครปฐม"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-21', 'TH', 'ADMIN', 2, 'TH', '{"th": "นครพนม"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-22', 'TH', 'ADMIN', 2, 'TH', '{"th": "นครราชสีมา"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH', NULL, 'COUNTRY', 1, 'TH', '{"th": "THAILAND", "zh-CN": "泰国"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-23', 'TH', 'ADMIN', 2, 'TH', '{"th": "นครศรีธรรมราช"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-24', 'TH', 'ADMIN', 2, 'TH', '{"th": "นครสวรรค์"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-25', 'TH', 'ADMIN', 2, 'TH', '{"th": "นนทบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-26', 'TH', 'ADMIN', 2, 'TH', '{"th": "นราธิวาส"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-27', 'TH', 'ADMIN', 2, 'TH', '{"th": "น่าน"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-28', 'TH', 'ADMIN', 2, 'TH', '{"th": "บุรีรัมย์"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-29', 'TH', 'ADMIN', 2, 'TH', '{"th": "ปทุมธานี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-30', 'TH', 'ADMIN', 2, 'TH', '{"th": "ประจวบคีรีขันธ์"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-31', 'TH', 'ADMIN', 2, 'TH', '{"th": "ปราจีนบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-32', 'TH', 'ADMIN', 2, 'TH', '{"th": "ปัตตานี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-33', 'TH', 'ADMIN', 2, 'TH', '{"th": "พระนครศรีอยุธยา"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-34', 'TH', 'ADMIN', 2, 'TH', '{"th": "พะเยา"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-35', 'TH', 'ADMIN', 2, 'TH', '{"th": "พังงา"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-36', 'TH', 'ADMIN', 2, 'TH', '{"th": "พัทลุง"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-37', 'TH', 'ADMIN', 2, 'TH', '{"th": "พิจิตร"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-38', 'TH', 'ADMIN', 2, 'TH', '{"th": "พิษณุโลก"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-39', 'TH', 'ADMIN', 2, 'TH', '{"th": "เพชรบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-40', 'TH', 'ADMIN', 2, 'TH', '{"th": "เพชรบูรณ์"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-41', 'TH', 'ADMIN', 2, 'TH', '{"th": "แพร่"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-42', 'TH', 'ADMIN', 2, 'TH', '{"th": "ภูเก็ต"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-43', 'TH', 'ADMIN', 2, 'TH', '{"th": "มหาสารคาม"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-44', 'TH', 'ADMIN', 2, 'TH', '{"th": "มุกดาหาร"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-45', 'TH', 'ADMIN', 2, 'TH', '{"th": "แม่ฮ่องสอน"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-46', 'TH', 'ADMIN', 2, 'TH', '{"th": "ยโสธร"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-47', 'TH', 'ADMIN', 2, 'TH', '{"th": "ยะลา"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-48', 'TH', 'ADMIN', 2, 'TH', '{"th": "ร้อยเอ็ด"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-49', 'TH', 'ADMIN', 2, 'TH', '{"th": "ระนอง"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-50', 'TH', 'ADMIN', 2, 'TH', '{"th": "ระยอง"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-51', 'TH', 'ADMIN', 2, 'TH', '{"th": "ราชบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-52', 'TH', 'ADMIN', 2, 'TH', '{"th": "ลพบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-53', 'TH', 'ADMIN', 2, 'TH', '{"th": "ลำปาง"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-54', 'TH', 'ADMIN', 2, 'TH', '{"th": "ลำพูน"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-55', 'TH', 'ADMIN', 2, 'TH', '{"th": "เลย"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-56', 'TH', 'ADMIN', 2, 'TH', '{"th": "ศรีสะเกษ"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-57', 'TH', 'ADMIN', 2, 'TH', '{"th": "สกลนคร"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-58', 'TH', 'ADMIN', 2, 'TH', '{"th": "สงขลา"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-59', 'TH', 'ADMIN', 2, 'TH', '{"th": "สตูล"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-60', 'TH', 'ADMIN', 2, 'TH', '{"th": "สมุทรปราการ"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-61', 'TH', 'ADMIN', 2, 'TH', '{"th": "สมุทรสงคราม"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-62', 'TH', 'ADMIN', 2, 'TH', '{"th": "สมุทรสาคร"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-63', 'TH', 'ADMIN', 2, 'TH', '{"th": "สระแก้ว"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-64', 'TH', 'ADMIN', 2, 'TH', '{"th": "สระบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-65', 'TH', 'ADMIN', 2, 'TH', '{"th": "สิงห์บุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-66', 'TH', 'ADMIN', 2, 'TH', '{"th": "สุโขทัย"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-67', 'TH', 'ADMIN', 2, 'TH', '{"th": "สุพรรณบุรี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-68', 'TH', 'ADMIN', 2, 'TH', '{"th": "สุราษฎร์ธานี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-69', 'TH', 'ADMIN', 2, 'TH', '{"th": "สุรินทร์"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-70', 'TH', 'ADMIN', 2, 'TH', '{"th": "หนองคาย"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-71', 'TH', 'ADMIN', 2, 'TH', '{"th": "หนองบัวลำภู"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-72', 'TH', 'ADMIN', 2, 'TH', '{"th": "อ่างทอง"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-73', 'TH', 'ADMIN', 2, 'TH', '{"th": "อำนาจเจริญ"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-74', 'TH', 'ADMIN', 2, 'TH', '{"th": "อุดรธานี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-75', 'TH', 'ADMIN', 2, 'TH', '{"th": "อุตรดิตถ์"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-76', 'TH', 'ADMIN', 2, 'TH', '{"th": "อุทัยธานี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TH-77', 'TH', 'ADMIN', 2, 'TH', '{"th": "อุบลราชธานี"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN', NULL, 'COUNTRY', 1, 'CN', '{"zh": "CHINA", "zh-CN": "中国"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('HK', NULL, 'COUNTRY', 1, 'HK', '{"zh-CN": "中国香港", "zh-Hant": "HONG KONG"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('TW', NULL, 'COUNTRY', 1, 'TW', '{"zh-CN": "中国台湾", "zh-Hant": "TAIWAN"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('JP', NULL, 'COUNTRY', 1, 'JP', '{"ja": "JAPAN", "zh-CN": "日本"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('SG', NULL, 'COUNTRY', 1, 'SG', '{"en": "REP. OF SINGAPORE", "zh-CN": "新加坡"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('MY', NULL, 'COUNTRY', 1, 'MY', '{"ms": "MALAYSIA", "zh-CN": "马来西亚"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-01', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "枞阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-02', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "大观区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-03', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "怀宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-04', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "潜山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-05', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "宿松县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-06', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "太湖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-07', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "桐城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-08', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "望江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-09', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "宜秀区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-10', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "迎江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-01-11', 'CN-01-01', 'DEPENDENT', 4, 'CN', '{"zh": "岳西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-02-01', 'CN-01-02', 'DEPENDENT', 4, 'CN', '{"zh": "蚌山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-02-02', 'CN-01-02', 'DEPENDENT', 4, 'CN', '{"zh": "固镇县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-02-03', 'CN-01-02', 'DEPENDENT', 4, 'CN', '{"zh": "怀远县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-02-04', 'CN-01-02', 'DEPENDENT', 4, 'CN', '{"zh": "淮上区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-02-05', 'CN-01-02', 'DEPENDENT', 4, 'CN', '{"zh": "龙子湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-02-06', 'CN-01-02', 'DEPENDENT', 4, 'CN', '{"zh": "五河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-02-07', 'CN-01-02', 'DEPENDENT', 4, 'CN', '{"zh": "禹会区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-03-01', 'CN-01-03', 'DEPENDENT', 4, 'CN', '{"zh": "利辛县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-03-02', 'CN-01-03', 'DEPENDENT', 4, 'CN', '{"zh": "蒙城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-03-03', 'CN-01-03', 'DEPENDENT', 4, 'CN', '{"zh": "谯城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-03-04', 'CN-01-03', 'DEPENDENT', 4, 'CN', '{"zh": "涡阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-04-01', 'CN-01-04', 'DEPENDENT', 4, 'CN', '{"zh": "含山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-04-02', 'CN-01-04', 'DEPENDENT', 4, 'CN', '{"zh": "和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-04-03', 'CN-01-04', 'DEPENDENT', 4, 'CN', '{"zh": "居巢区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-04-04', 'CN-01-04', 'DEPENDENT', 4, 'CN', '{"zh": "庐江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-04-05', 'CN-01-04', 'DEPENDENT', 4, 'CN', '{"zh": "无为县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-05-01', 'CN-01-05', 'DEPENDENT', 4, 'CN', '{"zh": "东至县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-05-02', 'CN-01-05', 'DEPENDENT', 4, 'CN', '{"zh": "贵池区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-05-03', 'CN-01-05', 'DEPENDENT', 4, 'CN', '{"zh": "青阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-05-04', 'CN-01-05', 'DEPENDENT', 4, 'CN', '{"zh": "石台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-06-01', 'CN-01-06', 'DEPENDENT', 4, 'CN', '{"zh": "定远县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-06-02', 'CN-01-06', 'DEPENDENT', 4, 'CN', '{"zh": "凤阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-06-03', 'CN-01-06', 'DEPENDENT', 4, 'CN', '{"zh": "来安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-06-04', 'CN-01-06', 'DEPENDENT', 4, 'CN', '{"zh": "琅琊区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-06-05', 'CN-01-06', 'DEPENDENT', 4, 'CN', '{"zh": "明光市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-06-06', 'CN-01-06', 'DEPENDENT', 4, 'CN', '{"zh": "南谯区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-06-07', 'CN-01-06', 'DEPENDENT', 4, 'CN', '{"zh": "全椒县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-06-08', 'CN-01-06', 'DEPENDENT', 4, 'CN', '{"zh": "天长市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-07-01', 'CN-01-07', 'DEPENDENT', 4, 'CN', '{"zh": "阜南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-07-02', 'CN-01-07', 'DEPENDENT', 4, 'CN', '{"zh": "界首市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-07-03', 'CN-01-07', 'DEPENDENT', 4, 'CN', '{"zh": "临泉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-07-04', 'CN-01-07', 'DEPENDENT', 4, 'CN', '{"zh": "太和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-07-05', 'CN-01-07', 'DEPENDENT', 4, 'CN', '{"zh": "颍东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-07-06', 'CN-01-07', 'DEPENDENT', 4, 'CN', '{"zh": "颍泉区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-07-07', 'CN-01-07', 'DEPENDENT', 4, 'CN', '{"zh": "颍上县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-07-08', 'CN-01-07', 'DEPENDENT', 4, 'CN', '{"zh": "颍州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-08-01', 'CN-01-08', 'DEPENDENT', 4, 'CN', '{"zh": "包河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-08-02', 'CN-01-08', 'DEPENDENT', 4, 'CN', '{"zh": "肥东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-08-03', 'CN-01-08', 'DEPENDENT', 4, 'CN', '{"zh": "肥西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-08-04', 'CN-01-08', 'DEPENDENT', 4, 'CN', '{"zh": "庐阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-08-05', 'CN-01-08', 'DEPENDENT', 4, 'CN', '{"zh": "蜀山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-08-06', 'CN-01-08', 'DEPENDENT', 4, 'CN', '{"zh": "瑶海区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-08-07', 'CN-01-08', 'DEPENDENT', 4, 'CN', '{"zh": "长丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-09-01', 'CN-01-09', 'DEPENDENT', 4, 'CN', '{"zh": "杜集区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-09-02', 'CN-01-09', 'DEPENDENT', 4, 'CN', '{"zh": "烈山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-09-03', 'CN-01-09', 'DEPENDENT', 4, 'CN', '{"zh": "濉溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-09-04', 'CN-01-09', 'DEPENDENT', 4, 'CN', '{"zh": "相山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-10-01', 'CN-01-10', 'DEPENDENT', 4, 'CN', '{"zh": "八公山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-10-02', 'CN-01-10', 'DEPENDENT', 4, 'CN', '{"zh": "大通区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-10-03', 'CN-01-10', 'DEPENDENT', 4, 'CN', '{"zh": "凤台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-10-04', 'CN-01-10', 'DEPENDENT', 4, 'CN', '{"zh": "潘集区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-10-05', 'CN-01-10', 'DEPENDENT', 4, 'CN', '{"zh": "田家庵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-10-06', 'CN-01-10', 'DEPENDENT', 4, 'CN', '{"zh": "谢家集区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-11-01', 'CN-01-11', 'DEPENDENT', 4, 'CN', '{"zh": "黄山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-11-02', 'CN-01-11', 'DEPENDENT', 4, 'CN', '{"zh": "徽州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-11-03', 'CN-01-11', 'DEPENDENT', 4, 'CN', '{"zh": "祁门县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-11-04', 'CN-01-11', 'DEPENDENT', 4, 'CN', '{"zh": "歙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-11-05', 'CN-01-11', 'DEPENDENT', 4, 'CN', '{"zh": "屯溪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-11-06', 'CN-01-11', 'DEPENDENT', 4, 'CN', '{"zh": "休宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-11-07', 'CN-01-11', 'DEPENDENT', 4, 'CN', '{"zh": "黟县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-12-01', 'CN-01-12', 'DEPENDENT', 4, 'CN', '{"zh": "霍邱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-12-02', 'CN-01-12', 'DEPENDENT', 4, 'CN', '{"zh": "霍山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-12-03', 'CN-01-12', 'DEPENDENT', 4, 'CN', '{"zh": "金安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-12-04', 'CN-01-12', 'DEPENDENT', 4, 'CN', '{"zh": "金寨县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-12-05', 'CN-01-12', 'DEPENDENT', 4, 'CN', '{"zh": "寿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-12-06', 'CN-01-12', 'DEPENDENT', 4, 'CN', '{"zh": "舒城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-12-07', 'CN-01-12', 'DEPENDENT', 4, 'CN', '{"zh": "裕安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-13-01', 'CN-01-13', 'DEPENDENT', 4, 'CN', '{"zh": "当涂县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-13-02', 'CN-01-13', 'DEPENDENT', 4, 'CN', '{"zh": "花山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-13-03', 'CN-01-13', 'DEPENDENT', 4, 'CN', '{"zh": "金家庄区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-13-04', 'CN-01-13', 'DEPENDENT', 4, 'CN', '{"zh": "雨山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-14-01', 'CN-01-14', 'DEPENDENT', 4, 'CN', '{"zh": "砀山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-14-02', 'CN-01-14', 'DEPENDENT', 4, 'CN', '{"zh": "灵璧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-14-03', 'CN-01-14', 'DEPENDENT', 4, 'CN', '{"zh": "泗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-14-04', 'CN-01-14', 'DEPENDENT', 4, 'CN', '{"zh": "萧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-14-05', 'CN-01-14', 'DEPENDENT', 4, 'CN', '{"zh": "埇桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-15-01', 'CN-01-15', 'DEPENDENT', 4, 'CN', '{"zh": "郊区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-15-02', 'CN-01-15', 'DEPENDENT', 4, 'CN', '{"zh": "狮子山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-15-03', 'CN-01-15', 'DEPENDENT', 4, 'CN', '{"zh": "铜官山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-15-04', 'CN-01-15', 'DEPENDENT', 4, 'CN', '{"zh": "铜陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-16-01', 'CN-01-16', 'DEPENDENT', 4, 'CN', '{"zh": "繁昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-16-02', 'CN-01-16', 'DEPENDENT', 4, 'CN', '{"zh": "镜湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-16-03', 'CN-01-16', 'DEPENDENT', 4, 'CN', '{"zh": "鸠江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-16-04', 'CN-01-16', 'DEPENDENT', 4, 'CN', '{"zh": "南陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-16-05', 'CN-01-16', 'DEPENDENT', 4, 'CN', '{"zh": "三山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-16-06', 'CN-01-16', 'DEPENDENT', 4, 'CN', '{"zh": "无为县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-16-07', 'CN-01-16', 'DEPENDENT', 4, 'CN', '{"zh": "芜湖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-16-08', 'CN-01-16', 'DEPENDENT', 4, 'CN', '{"zh": "弋江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-17-01', 'CN-01-17', 'DEPENDENT', 4, 'CN', '{"zh": "广德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-17-02', 'CN-01-17', 'DEPENDENT', 4, 'CN', '{"zh": "绩溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-17-03', 'CN-01-17', 'DEPENDENT', 4, 'CN', '{"zh": "泾县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-17-04', 'CN-01-17', 'DEPENDENT', 4, 'CN', '{"zh": "旌德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-17-05', 'CN-01-17', 'DEPENDENT', 4, 'CN', '{"zh": "郎溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-17-06', 'CN-01-17', 'DEPENDENT', 4, 'CN', '{"zh": "宁国市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-01-17-07', 'CN-01-17', 'DEPENDENT', 4, 'CN', '{"zh": "宣州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-01', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "仓山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-02', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "福清市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-03', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "鼓楼区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-04', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "晋安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-05', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "连江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-06', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "罗源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-07', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "马尾区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-08', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "闽侯县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-09', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "闽清县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-10', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "平潭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-11', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "台江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-12', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "永泰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-01-13', 'CN-05-01', 'DEPENDENT', 4, 'CN', '{"zh": "长乐市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-02-01', 'CN-05-02', 'DEPENDENT', 4, 'CN', '{"zh": "连城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-02-02', 'CN-05-02', 'DEPENDENT', 4, 'CN', '{"zh": "上杭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-02-03', 'CN-05-02', 'DEPENDENT', 4, 'CN', '{"zh": "武平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-02-04', 'CN-05-02', 'DEPENDENT', 4, 'CN', '{"zh": "新罗区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-02-05', 'CN-05-02', 'DEPENDENT', 4, 'CN', '{"zh": "永定县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-02-06', 'CN-05-02', 'DEPENDENT', 4, 'CN', '{"zh": "漳平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-02-07', 'CN-05-02', 'DEPENDENT', 4, 'CN', '{"zh": "长汀县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-01', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "光泽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-02', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "建瓯市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-03', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "建阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-04', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "浦城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-05', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "邵武市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-06', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "顺昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-07', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "松溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-08', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "武夷山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-09', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "延平区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-03-10', 'CN-05-03', 'DEPENDENT', 4, 'CN', '{"zh": "政和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04-01', 'CN-05-04', 'DEPENDENT', 4, 'CN', '{"zh": "福安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04-02', 'CN-05-04', 'DEPENDENT', 4, 'CN', '{"zh": "福鼎市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04-03', 'CN-05-04', 'DEPENDENT', 4, 'CN', '{"zh": "古田县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04-04', 'CN-05-04', 'DEPENDENT', 4, 'CN', '{"zh": "蕉城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04-05', 'CN-05-04', 'DEPENDENT', 4, 'CN', '{"zh": "屏南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04-06', 'CN-05-04', 'DEPENDENT', 4, 'CN', '{"zh": "寿宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04-07', 'CN-05-04', 'DEPENDENT', 4, 'CN', '{"zh": "霞浦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04-08', 'CN-05-04', 'DEPENDENT', 4, 'CN', '{"zh": "柘荣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-04-09', 'CN-05-04', 'DEPENDENT', 4, 'CN', '{"zh": "周宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-05-01', 'CN-05-05', 'DEPENDENT', 4, 'CN', '{"zh": "城厢区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-05-02', 'CN-05-05', 'DEPENDENT', 4, 'CN', '{"zh": "涵江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-05-03', 'CN-05-05', 'DEPENDENT', 4, 'CN', '{"zh": "荔城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-05-04', 'CN-05-05', 'DEPENDENT', 4, 'CN', '{"zh": "仙游县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-05-05', 'CN-05-05', 'DEPENDENT', 4, 'CN', '{"zh": "秀屿区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-01', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "安溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-02', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "德化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-03', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "丰泽区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-04', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "惠安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-05', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "金门县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-06', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "晋江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-07', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "鲤城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-08', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "洛江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-09', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "南安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-10', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "泉港区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-11', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "石狮市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-06-12', 'CN-05-06', 'DEPENDENT', 4, 'CN', '{"zh": "永春县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-01', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "大田县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-02', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "建宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-03', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "将乐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-04', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "梅列区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-05', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "明溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-06', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "宁化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-07', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "清流县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-08', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "三元区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-09', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "沙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-10', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "泰宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-11', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "永安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-07-12', 'CN-05-07', 'DEPENDENT', 4, 'CN', '{"zh": "尤溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-08-01', 'CN-05-08', 'DEPENDENT', 4, 'CN', '{"zh": "海沧区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-08-02', 'CN-05-08', 'DEPENDENT', 4, 'CN', '{"zh": "湖里区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-08-03', 'CN-05-08', 'DEPENDENT', 4, 'CN', '{"zh": "集美区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-08-04', 'CN-05-08', 'DEPENDENT', 4, 'CN', '{"zh": "思明区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-08-05', 'CN-05-08', 'DEPENDENT', 4, 'CN', '{"zh": "同安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-08-06', 'CN-05-08', 'DEPENDENT', 4, 'CN', '{"zh": "翔安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-01', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "东山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-02', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "华安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-03', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "龙海市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-04', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "龙文区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-05', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "南靖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-06', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "平和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-07', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "芗城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-08', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "云霄县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-09', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "漳浦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-10', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "长泰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-05-09-11', 'CN-05-09', 'DEPENDENT', 4, 'CN', '{"zh": "诏安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-01-01', 'CN-06-01', 'DEPENDENT', 4, 'CN', '{"zh": "白银区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-01-02', 'CN-06-01', 'DEPENDENT', 4, 'CN', '{"zh": "会宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-01-03', 'CN-06-01', 'DEPENDENT', 4, 'CN', '{"zh": "景泰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-01-04', 'CN-06-01', 'DEPENDENT', 4, 'CN', '{"zh": "靖远县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-01-05', 'CN-06-01', 'DEPENDENT', 4, 'CN', '{"zh": "平川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-02-01', 'CN-06-02', 'DEPENDENT', 4, 'CN', '{"zh": "安定区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-02-02', 'CN-06-02', 'DEPENDENT', 4, 'CN', '{"zh": "临洮县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-02-03', 'CN-06-02', 'DEPENDENT', 4, 'CN', '{"zh": "陇西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-02-04', 'CN-06-02', 'DEPENDENT', 4, 'CN', '{"zh": "岷县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-02-05', 'CN-06-02', 'DEPENDENT', 4, 'CN', '{"zh": "通渭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-02-06', 'CN-06-02', 'DEPENDENT', 4, 'CN', '{"zh": "渭源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-02-07', 'CN-06-02', 'DEPENDENT', 4, 'CN', '{"zh": "漳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-03-01', 'CN-06-03', 'DEPENDENT', 4, 'CN', '{"zh": "迭部县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-03-02', 'CN-06-03', 'DEPENDENT', 4, 'CN', '{"zh": "合作市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-03-03', 'CN-06-03', 'DEPENDENT', 4, 'CN', '{"zh": "临潭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-03-04', 'CN-06-03', 'DEPENDENT', 4, 'CN', '{"zh": "碌曲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-03-05', 'CN-06-03', 'DEPENDENT', 4, 'CN', '{"zh": "玛曲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-03-06', 'CN-06-03', 'DEPENDENT', 4, 'CN', '{"zh": "夏河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-03-07', 'CN-06-03', 'DEPENDENT', 4, 'CN', '{"zh": "舟曲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-03-08', 'CN-06-03', 'DEPENDENT', 4, 'CN', '{"zh": "卓尼县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-05-01', 'CN-06-05', 'DEPENDENT', 4, 'CN', '{"zh": "金川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-05-02', 'CN-06-05', 'DEPENDENT', 4, 'CN', '{"zh": "永昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-06-01', 'CN-06-06', 'DEPENDENT', 4, 'CN', '{"zh": "阿克塞县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-06-02', 'CN-06-06', 'DEPENDENT', 4, 'CN', '{"zh": "敦煌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-06-03', 'CN-06-06', 'DEPENDENT', 4, 'CN', '{"zh": "瓜州县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-06-04', 'CN-06-06', 'DEPENDENT', 4, 'CN', '{"zh": "金塔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-06-05', 'CN-06-06', 'DEPENDENT', 4, 'CN', '{"zh": "肃北县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-06-06', 'CN-06-06', 'DEPENDENT', 4, 'CN', '{"zh": "肃州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-06-07', 'CN-06-06', 'DEPENDENT', 4, 'CN', '{"zh": "玉门市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-07-01', 'CN-06-07', 'DEPENDENT', 4, 'CN', '{"zh": "安宁区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-07-02', 'CN-06-07', 'DEPENDENT', 4, 'CN', '{"zh": "城关区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-07-03', 'CN-06-07', 'DEPENDENT', 4, 'CN', '{"zh": "皋兰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-07-04', 'CN-06-07', 'DEPENDENT', 4, 'CN', '{"zh": "红古区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-07-05', 'CN-06-07', 'DEPENDENT', 4, 'CN', '{"zh": "七里河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-07-06', 'CN-06-07', 'DEPENDENT', 4, 'CN', '{"zh": "西固区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-07-07', 'CN-06-07', 'DEPENDENT', 4, 'CN', '{"zh": "永登县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-07-08', 'CN-06-07', 'DEPENDENT', 4, 'CN', '{"zh": "榆中县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-08-01', 'CN-06-08', 'DEPENDENT', 4, 'CN', '{"zh": "东乡族县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-08-02', 'CN-06-08', 'DEPENDENT', 4, 'CN', '{"zh": "广河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-08-03', 'CN-06-08', 'DEPENDENT', 4, 'CN', '{"zh": "和政县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-08-04', 'CN-06-08', 'DEPENDENT', 4, 'CN', '{"zh": "积石山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-08-05', 'CN-06-08', 'DEPENDENT', 4, 'CN', '{"zh": "康乐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-08-06', 'CN-06-08', 'DEPENDENT', 4, 'CN', '{"zh": "临夏市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-08-07', 'CN-06-08', 'DEPENDENT', 4, 'CN', '{"zh": "临夏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-08-08', 'CN-06-08', 'DEPENDENT', 4, 'CN', '{"zh": "永靖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09-01', 'CN-06-09', 'DEPENDENT', 4, 'CN', '{"zh": "成县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09-02', 'CN-06-09', 'DEPENDENT', 4, 'CN', '{"zh": "宕昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09-03', 'CN-06-09', 'DEPENDENT', 4, 'CN', '{"zh": "徽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09-04', 'CN-06-09', 'DEPENDENT', 4, 'CN', '{"zh": "康县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09-05', 'CN-06-09', 'DEPENDENT', 4, 'CN', '{"zh": "礼县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09-06', 'CN-06-09', 'DEPENDENT', 4, 'CN', '{"zh": "两当县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09-07', 'CN-06-09', 'DEPENDENT', 4, 'CN', '{"zh": "文县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09-08', 'CN-06-09', 'DEPENDENT', 4, 'CN', '{"zh": "武都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-09-09', 'CN-06-09', 'DEPENDENT', 4, 'CN', '{"zh": "西和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-10-01', 'CN-06-10', 'DEPENDENT', 4, 'CN', '{"zh": "崇信县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-10-02', 'CN-06-10', 'DEPENDENT', 4, 'CN', '{"zh": "华亭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-10-03', 'CN-06-10', 'DEPENDENT', 4, 'CN', '{"zh": "泾川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-10-04', 'CN-06-10', 'DEPENDENT', 4, 'CN', '{"zh": "静宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-10-05', 'CN-06-10', 'DEPENDENT', 4, 'CN', '{"zh": "崆峒区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-10-06', 'CN-06-10', 'DEPENDENT', 4, 'CN', '{"zh": "灵台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-10-07', 'CN-06-10', 'DEPENDENT', 4, 'CN', '{"zh": "庄浪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-11-01', 'CN-06-11', 'DEPENDENT', 4, 'CN', '{"zh": "合水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-11-02', 'CN-06-11', 'DEPENDENT', 4, 'CN', '{"zh": "华池县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-11-03', 'CN-06-11', 'DEPENDENT', 4, 'CN', '{"zh": "环县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-11-04', 'CN-06-11', 'DEPENDENT', 4, 'CN', '{"zh": "宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-11-05', 'CN-06-11', 'DEPENDENT', 4, 'CN', '{"zh": "庆城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-11-06', 'CN-06-11', 'DEPENDENT', 4, 'CN', '{"zh": "西峰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-11-07', 'CN-06-11', 'DEPENDENT', 4, 'CN', '{"zh": "镇原县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-11-08', 'CN-06-11', 'DEPENDENT', 4, 'CN', '{"zh": "正宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-12-01', 'CN-06-12', 'DEPENDENT', 4, 'CN', '{"zh": "甘谷县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-12-02', 'CN-06-12', 'DEPENDENT', 4, 'CN', '{"zh": "麦积区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-12-03', 'CN-06-12', 'DEPENDENT', 4, 'CN', '{"zh": "秦安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-12-04', 'CN-06-12', 'DEPENDENT', 4, 'CN', '{"zh": "秦州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-12-05', 'CN-06-12', 'DEPENDENT', 4, 'CN', '{"zh": "清水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-12-06', 'CN-06-12', 'DEPENDENT', 4, 'CN', '{"zh": "武山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-12-07', 'CN-06-12', 'DEPENDENT', 4, 'CN', '{"zh": "张家川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-13-01', 'CN-06-13', 'DEPENDENT', 4, 'CN', '{"zh": "古浪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-13-02', 'CN-06-13', 'DEPENDENT', 4, 'CN', '{"zh": "凉州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-13-03', 'CN-06-13', 'DEPENDENT', 4, 'CN', '{"zh": "民勤县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-13-04', 'CN-06-13', 'DEPENDENT', 4, 'CN', '{"zh": "天祝县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-14-01', 'CN-06-14', 'DEPENDENT', 4, 'CN', '{"zh": "甘州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-14-02', 'CN-06-14', 'DEPENDENT', 4, 'CN', '{"zh": "高台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-14-03', 'CN-06-14', 'DEPENDENT', 4, 'CN', '{"zh": "临泽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-14-04', 'CN-06-14', 'DEPENDENT', 4, 'CN', '{"zh": "民乐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-14-05', 'CN-06-14', 'DEPENDENT', 4, 'CN', '{"zh": "山丹县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-06-14-06', 'CN-06-14', 'DEPENDENT', 4, 'CN', '{"zh": "肃南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-01-01', 'CN-07-01', 'DEPENDENT', 4, 'CN', '{"zh": "潮安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-01-02', 'CN-07-01', 'DEPENDENT', 4, 'CN', '{"zh": "饶平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-01-03', 'CN-07-01', 'DEPENDENT', 4, 'CN', '{"zh": "湘桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-03-01', 'CN-07-03', 'DEPENDENT', 4, 'CN', '{"zh": "禅城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-03-02', 'CN-07-03', 'DEPENDENT', 4, 'CN', '{"zh": "高明区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-03-03', 'CN-07-03', 'DEPENDENT', 4, 'CN', '{"zh": "南海区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-03-04', 'CN-07-03', 'DEPENDENT', 4, 'CN', '{"zh": "三水区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-03-05', 'CN-07-03', 'DEPENDENT', 4, 'CN', '{"zh": "顺德区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-01', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "白云区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-02', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "从化区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-03', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "番禺区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-04', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "海珠区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-05', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "花都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-06', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "黄埔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-07', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "荔湾区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-08', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "南沙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-09', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "天河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-10', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "越秀区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-04-11', 'CN-07-04', 'DEPENDENT', 4, 'CN', '{"zh": "增城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-05-01', 'CN-07-05', 'DEPENDENT', 4, 'CN', '{"zh": "东源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-05-02', 'CN-07-05', 'DEPENDENT', 4, 'CN', '{"zh": "和平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-05-03', 'CN-07-05', 'DEPENDENT', 4, 'CN', '{"zh": "连平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-05-04', 'CN-07-05', 'DEPENDENT', 4, 'CN', '{"zh": "龙川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-05-05', 'CN-07-05', 'DEPENDENT', 4, 'CN', '{"zh": "源城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-05-06', 'CN-07-05', 'DEPENDENT', 4, 'CN', '{"zh": "紫金县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-06-01', 'CN-07-06', 'DEPENDENT', 4, 'CN', '{"zh": "博罗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-06-02', 'CN-07-06', 'DEPENDENT', 4, 'CN', '{"zh": "惠城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-06-03', 'CN-07-06', 'DEPENDENT', 4, 'CN', '{"zh": "惠东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-06-04', 'CN-07-06', 'DEPENDENT', 4, 'CN', '{"zh": "惠阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-06-05', 'CN-07-06', 'DEPENDENT', 4, 'CN', '{"zh": "龙门县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-07-01', 'CN-07-07', 'DEPENDENT', 4, 'CN', '{"zh": "恩平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-07-02', 'CN-07-07', 'DEPENDENT', 4, 'CN', '{"zh": "鹤山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-07-03', 'CN-07-07', 'DEPENDENT', 4, 'CN', '{"zh": "江海区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-07-04', 'CN-07-07', 'DEPENDENT', 4, 'CN', '{"zh": "开平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-07-05', 'CN-07-07', 'DEPENDENT', 4, 'CN', '{"zh": "蓬江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-07-06', 'CN-07-07', 'DEPENDENT', 4, 'CN', '{"zh": "台山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-07-07', 'CN-07-07', 'DEPENDENT', 4, 'CN', '{"zh": "新会区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-08-01', 'CN-07-08', 'DEPENDENT', 4, 'CN', '{"zh": "惠来县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-08-02', 'CN-07-08', 'DEPENDENT', 4, 'CN', '{"zh": "揭东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-08-03', 'CN-07-08', 'DEPENDENT', 4, 'CN', '{"zh": "揭西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-08-04', 'CN-07-08', 'DEPENDENT', 4, 'CN', '{"zh": "普宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-08-05', 'CN-07-08', 'DEPENDENT', 4, 'CN', '{"zh": "榕城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-09-01', 'CN-07-09', 'DEPENDENT', 4, 'CN', '{"zh": "电白区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-09-02', 'CN-07-09', 'DEPENDENT', 4, 'CN', '{"zh": "高州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-09-03', 'CN-07-09', 'DEPENDENT', 4, 'CN', '{"zh": "化州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-09-04', 'CN-07-09', 'DEPENDENT', 4, 'CN', '{"zh": "茂南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-09-05', 'CN-07-09', 'DEPENDENT', 4, 'CN', '{"zh": "信宜市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-10-01', 'CN-07-10', 'DEPENDENT', 4, 'CN', '{"zh": "大埔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-10-02', 'CN-07-10', 'DEPENDENT', 4, 'CN', '{"zh": "丰顺县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-10-03', 'CN-07-10', 'DEPENDENT', 4, 'CN', '{"zh": "蕉岭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-10-04', 'CN-07-10', 'DEPENDENT', 4, 'CN', '{"zh": "梅江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-10-05', 'CN-07-10', 'DEPENDENT', 4, 'CN', '{"zh": "梅县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-10-06', 'CN-07-10', 'DEPENDENT', 4, 'CN', '{"zh": "平远县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-10-07', 'CN-07-10', 'DEPENDENT', 4, 'CN', '{"zh": "五华县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-10-08', 'CN-07-10', 'DEPENDENT', 4, 'CN', '{"zh": "兴宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-11-01', 'CN-07-11', 'DEPENDENT', 4, 'CN', '{"zh": "佛冈县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-11-02', 'CN-07-11', 'DEPENDENT', 4, 'CN', '{"zh": "连南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-11-03', 'CN-07-11', 'DEPENDENT', 4, 'CN', '{"zh": "连山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-11-04', 'CN-07-11', 'DEPENDENT', 4, 'CN', '{"zh": "连州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-11-05', 'CN-07-11', 'DEPENDENT', 4, 'CN', '{"zh": "清城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-11-06', 'CN-07-11', 'DEPENDENT', 4, 'CN', '{"zh": "清新县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-11-07', 'CN-07-11', 'DEPENDENT', 4, 'CN', '{"zh": "阳山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-11-08', 'CN-07-11', 'DEPENDENT', 4, 'CN', '{"zh": "英德市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-12-01', 'CN-07-12', 'DEPENDENT', 4, 'CN', '{"zh": "潮南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-12-02', 'CN-07-12', 'DEPENDENT', 4, 'CN', '{"zh": "潮阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-12-03', 'CN-07-12', 'DEPENDENT', 4, 'CN', '{"zh": "澄海区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-12-04', 'CN-07-12', 'DEPENDENT', 4, 'CN', '{"zh": "濠江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-12-05', 'CN-07-12', 'DEPENDENT', 4, 'CN', '{"zh": "金平区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-12-06', 'CN-07-12', 'DEPENDENT', 4, 'CN', '{"zh": "龙湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-12-07', 'CN-07-12', 'DEPENDENT', 4, 'CN', '{"zh": "南澳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-13-01', 'CN-07-13', 'DEPENDENT', 4, 'CN', '{"zh": "城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-13-02', 'CN-07-13', 'DEPENDENT', 4, 'CN', '{"zh": "海丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-13-03', 'CN-07-13', 'DEPENDENT', 4, 'CN', '{"zh": "陆丰市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-13-04', 'CN-07-13', 'DEPENDENT', 4, 'CN', '{"zh": "陆河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-01', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "乐昌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-02', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "南雄市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-03', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "曲江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-04', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "仁化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-05', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "乳源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-06', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "始兴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-07', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "翁源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-08', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "武江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-09', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "新丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-14-10', 'CN-07-14', 'DEPENDENT', 4, 'CN', '{"zh": "浈江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15-01', 'CN-07-15', 'DEPENDENT', 4, 'CN', '{"zh": "宝安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15-02', 'CN-07-15', 'DEPENDENT', 4, 'CN', '{"zh": "福田区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15-03', 'CN-07-15', 'DEPENDENT', 4, 'CN', '{"zh": "光明区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15-04', 'CN-07-15', 'DEPENDENT', 4, 'CN', '{"zh": "龙岗区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15-05', 'CN-07-15', 'DEPENDENT', 4, 'CN', '{"zh": "龙华区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15-06', 'CN-07-15', 'DEPENDENT', 4, 'CN', '{"zh": "罗湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15-07', 'CN-07-15', 'DEPENDENT', 4, 'CN', '{"zh": "南山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15-08', 'CN-07-15', 'DEPENDENT', 4, 'CN', '{"zh": "坪山新区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-15-09', 'CN-07-15', 'DEPENDENT', 4, 'CN', '{"zh": "盐田区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-16-01', 'CN-07-16', 'DEPENDENT', 4, 'CN', '{"zh": "江城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-16-02', 'CN-07-16', 'DEPENDENT', 4, 'CN', '{"zh": "阳春市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-16-03', 'CN-07-16', 'DEPENDENT', 4, 'CN', '{"zh": "阳东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-16-04', 'CN-07-16', 'DEPENDENT', 4, 'CN', '{"zh": "阳西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-17-01', 'CN-07-17', 'DEPENDENT', 4, 'CN', '{"zh": "罗定市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-17-02', 'CN-07-17', 'DEPENDENT', 4, 'CN', '{"zh": "新兴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-17-03', 'CN-07-17', 'DEPENDENT', 4, 'CN', '{"zh": "郁南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-17-04', 'CN-07-17', 'DEPENDENT', 4, 'CN', '{"zh": "云安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-17-05', 'CN-07-17', 'DEPENDENT', 4, 'CN', '{"zh": "云城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18-01', 'CN-07-18', 'DEPENDENT', 4, 'CN', '{"zh": "赤坎区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18-02', 'CN-07-18', 'DEPENDENT', 4, 'CN', '{"zh": "雷州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18-03', 'CN-07-18', 'DEPENDENT', 4, 'CN', '{"zh": "廉江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18-04', 'CN-07-18', 'DEPENDENT', 4, 'CN', '{"zh": "麻章区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18-05', 'CN-07-18', 'DEPENDENT', 4, 'CN', '{"zh": "坡头区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18-06', 'CN-07-18', 'DEPENDENT', 4, 'CN', '{"zh": "遂溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18-07', 'CN-07-18', 'DEPENDENT', 4, 'CN', '{"zh": "吴川市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18-08', 'CN-07-18', 'DEPENDENT', 4, 'CN', '{"zh": "霞山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-18-09', 'CN-07-18', 'DEPENDENT', 4, 'CN', '{"zh": "徐闻县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-19-01', 'CN-07-19', 'DEPENDENT', 4, 'CN', '{"zh": "德庆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-19-02', 'CN-07-19', 'DEPENDENT', 4, 'CN', '{"zh": "鼎湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-19-03', 'CN-07-19', 'DEPENDENT', 4, 'CN', '{"zh": "端州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-19-04', 'CN-07-19', 'DEPENDENT', 4, 'CN', '{"zh": "封开县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-19-05', 'CN-07-19', 'DEPENDENT', 4, 'CN', '{"zh": "高要区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-19-06', 'CN-07-19', 'DEPENDENT', 4, 'CN', '{"zh": "广宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-19-07', 'CN-07-19', 'DEPENDENT', 4, 'CN', '{"zh": "怀集县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-19-08', 'CN-07-19', 'DEPENDENT', 4, 'CN', '{"zh": "四会市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-21-01', 'CN-07-21', 'DEPENDENT', 4, 'CN', '{"zh": "斗门区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-21-02', 'CN-07-21', 'DEPENDENT', 4, 'CN', '{"zh": "金湾区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-07-21-03', 'CN-07-21', 'DEPENDENT', 4, 'CN', '{"zh": "香洲区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-01', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "德保县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-02', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "靖西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-03', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "乐业县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-04', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "凌云县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-05', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "隆林县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-06', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "那坡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-07', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "平果县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-08', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "田东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-09', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "田林县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-10', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "田阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-11', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "西林县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-01-12', 'CN-08-01', 'DEPENDENT', 4, 'CN', '{"zh": "右江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-02-01', 'CN-08-02', 'DEPENDENT', 4, 'CN', '{"zh": "海城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-02-02', 'CN-08-02', 'DEPENDENT', 4, 'CN', '{"zh": "合浦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-02-03', 'CN-08-02', 'DEPENDENT', 4, 'CN', '{"zh": "铁山港区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-02-04', 'CN-08-02', 'DEPENDENT', 4, 'CN', '{"zh": "银海区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-03-01', 'CN-08-03', 'DEPENDENT', 4, 'CN', '{"zh": "大新县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-03-02', 'CN-08-03', 'DEPENDENT', 4, 'CN', '{"zh": "扶绥县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-03-03', 'CN-08-03', 'DEPENDENT', 4, 'CN', '{"zh": "江洲区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-03-04', 'CN-08-03', 'DEPENDENT', 4, 'CN', '{"zh": "龙州县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-03-05', 'CN-08-03', 'DEPENDENT', 4, 'CN', '{"zh": "宁明县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-03-06', 'CN-08-03', 'DEPENDENT', 4, 'CN', '{"zh": "凭祥市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-03-07', 'CN-08-03', 'DEPENDENT', 4, 'CN', '{"zh": "天等县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-04-01', 'CN-08-04', 'DEPENDENT', 4, 'CN', '{"zh": "东兴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-04-02', 'CN-08-04', 'DEPENDENT', 4, 'CN', '{"zh": "防城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-04-03', 'CN-08-04', 'DEPENDENT', 4, 'CN', '{"zh": "港口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-04-04', 'CN-08-04', 'DEPENDENT', 4, 'CN', '{"zh": "上思县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-05-01', 'CN-08-05', 'DEPENDENT', 4, 'CN', '{"zh": "港北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-05-02', 'CN-08-05', 'DEPENDENT', 4, 'CN', '{"zh": "港南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-05-03', 'CN-08-05', 'DEPENDENT', 4, 'CN', '{"zh": "桂平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-05-04', 'CN-08-05', 'DEPENDENT', 4, 'CN', '{"zh": "平南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-05-05', 'CN-08-05', 'DEPENDENT', 4, 'CN', '{"zh": "覃塘区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-01', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "叠彩区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-02', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "恭城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-03', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "灌阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-04', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "荔浦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-05', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "临桂区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-06', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "灵川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-07', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "龙胜县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-08', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "平乐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-09', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "七星区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-10', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "全州县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-11', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "象山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-12', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "兴安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-13', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "秀峰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-14', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "雁山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-15', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "阳朔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-16', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "永福县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-06-17', 'CN-08-06', 'DEPENDENT', 4, 'CN', '{"zh": "资源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-01', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "巴马县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-02', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "大化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-03', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "东兰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-04', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "都安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-05', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "凤山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-06', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "环江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-07', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "金城江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-08', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "罗城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-09', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "南丹县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-10', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "天峨县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-07-11', 'CN-08-07', 'DEPENDENT', 4, 'CN', '{"zh": "宜州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-08-01', 'CN-08-08', 'DEPENDENT', 4, 'CN', '{"zh": "八步区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-08-02', 'CN-08-08', 'DEPENDENT', 4, 'CN', '{"zh": "富川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-08-03', 'CN-08-08', 'DEPENDENT', 4, 'CN', '{"zh": "昭平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-08-04', 'CN-08-08', 'DEPENDENT', 4, 'CN', '{"zh": "钟山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-09-01', 'CN-08-09', 'DEPENDENT', 4, 'CN', '{"zh": "合山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-09-02', 'CN-08-09', 'DEPENDENT', 4, 'CN', '{"zh": "金秀县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-09-03', 'CN-08-09', 'DEPENDENT', 4, 'CN', '{"zh": "武宣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-09-04', 'CN-08-09', 'DEPENDENT', 4, 'CN', '{"zh": "象州县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-09-05', 'CN-08-09', 'DEPENDENT', 4, 'CN', '{"zh": "忻城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-09-06', 'CN-08-09', 'DEPENDENT', 4, 'CN', '{"zh": "兴宾区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-01', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "城中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-02', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "柳北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-03', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "柳城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-04', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "柳江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-05', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "柳南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-06', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "鹿寨县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-07', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "融安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-08', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "融水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-09', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "三江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-10-10', 'CN-08-10', 'DEPENDENT', 4, 'CN', '{"zh": "鱼峰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-01', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "宾阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-02', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "横县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-03', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "江南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-04', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "良庆区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-05', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "隆安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-06', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "马山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-07', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "青秀区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-08', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "上林县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-09', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "武鸣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-10', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "西乡塘区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-11', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "兴宁区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-11-12', 'CN-08-11', 'DEPENDENT', 4, 'CN', '{"zh": "邕宁区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-12-01', 'CN-08-12', 'DEPENDENT', 4, 'CN', '{"zh": "灵山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-12-02', 'CN-08-12', 'DEPENDENT', 4, 'CN', '{"zh": "浦北县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-12-03', 'CN-08-12', 'DEPENDENT', 4, 'CN', '{"zh": "钦北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-12-04', 'CN-08-12', 'DEPENDENT', 4, 'CN', '{"zh": "钦南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-13-01', 'CN-08-13', 'DEPENDENT', 4, 'CN', '{"zh": "苍梧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-13-02', 'CN-08-13', 'DEPENDENT', 4, 'CN', '{"zh": "岑溪市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-13-03', 'CN-08-13', 'DEPENDENT', 4, 'CN', '{"zh": "蝶山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-13-04', 'CN-08-13', 'DEPENDENT', 4, 'CN', '{"zh": "蒙山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-13-05', 'CN-08-13', 'DEPENDENT', 4, 'CN', '{"zh": "藤县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-13-06', 'CN-08-13', 'DEPENDENT', 4, 'CN', '{"zh": "万秀区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-13-07', 'CN-08-13', 'DEPENDENT', 4, 'CN', '{"zh": "长洲区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-14-01', 'CN-08-14', 'DEPENDENT', 4, 'CN', '{"zh": "北流市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-14-02', 'CN-08-14', 'DEPENDENT', 4, 'CN', '{"zh": "博白县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-14-03', 'CN-08-14', 'DEPENDENT', 4, 'CN', '{"zh": "陆川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-14-04', 'CN-08-14', 'DEPENDENT', 4, 'CN', '{"zh": "容县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-14-05', 'CN-08-14', 'DEPENDENT', 4, 'CN', '{"zh": "兴业县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-08-14-06', 'CN-08-14', 'DEPENDENT', 4, 'CN', '{"zh": "玉州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-01-01', 'CN-09-01', 'DEPENDENT', 4, 'CN', '{"zh": "关岭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-01-02', 'CN-09-01', 'DEPENDENT', 4, 'CN', '{"zh": "平坝县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-01-03', 'CN-09-01', 'DEPENDENT', 4, 'CN', '{"zh": "普定县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-01-04', 'CN-09-01', 'DEPENDENT', 4, 'CN', '{"zh": "西秀区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-01-05', 'CN-09-01', 'DEPENDENT', 4, 'CN', '{"zh": "镇宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-01-06', 'CN-09-01', 'DEPENDENT', 4, 'CN', '{"zh": "紫云县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-02-01', 'CN-09-02', 'DEPENDENT', 4, 'CN', '{"zh": "毕节市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-02-02', 'CN-09-02', 'DEPENDENT', 4, 'CN', '{"zh": "大方县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-02-03', 'CN-09-02', 'DEPENDENT', 4, 'CN', '{"zh": "赫章县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-02-04', 'CN-09-02', 'DEPENDENT', 4, 'CN', '{"zh": "金沙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-02-05', 'CN-09-02', 'DEPENDENT', 4, 'CN', '{"zh": "纳雍县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-02-06', 'CN-09-02', 'DEPENDENT', 4, 'CN', '{"zh": "黔西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-02-07', 'CN-09-02', 'DEPENDENT', 4, 'CN', '{"zh": "威宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-02-08', 'CN-09-02', 'DEPENDENT', 4, 'CN', '{"zh": "织金县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-01', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "白云区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-02', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "观山湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-03', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "花溪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-04', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "开阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-05', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "南明区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-06', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "清镇市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-07', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "乌当区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-08', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "息烽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-09', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "修文县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-03-10', 'CN-09-03', 'DEPENDENT', 4, 'CN', '{"zh": "云岩区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-04-01', 'CN-09-04', 'DEPENDENT', 4, 'CN', '{"zh": "六枝特区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-04-02', 'CN-09-04', 'DEPENDENT', 4, 'CN', '{"zh": "盘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-04-03', 'CN-09-04', 'DEPENDENT', 4, 'CN', '{"zh": "水城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-04-04', 'CN-09-04', 'DEPENDENT', 4, 'CN', '{"zh": "钟山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-01', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "岑巩县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-02', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "从江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-03', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "丹寨县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-04', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "黄平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-05', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "剑河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-06', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "锦屏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-07', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "凯里市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-08', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "雷山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-09', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "黎平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-10', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "麻江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-11', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "榕江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-12', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "三穗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-13', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "施秉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-14', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "台江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-15', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "天柱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-05-16', 'CN-09-05', 'DEPENDENT', 4, 'CN', '{"zh": "镇远县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-01', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "都匀市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-02', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "独山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-03', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "福泉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-04', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "贵定县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-05', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "惠水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-06', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "荔波县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-07', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "龙里县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-08', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "罗甸县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-09', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "平塘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-10', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "三都县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-11', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "瓮安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-06-12', 'CN-09-06', 'DEPENDENT', 4, 'CN', '{"zh": "长顺县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-07-01', 'CN-09-07', 'DEPENDENT', 4, 'CN', '{"zh": "安龙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-07-02', 'CN-09-07', 'DEPENDENT', 4, 'CN', '{"zh": "册亨县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-07-03', 'CN-09-07', 'DEPENDENT', 4, 'CN', '{"zh": "普安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-07-04', 'CN-09-07', 'DEPENDENT', 4, 'CN', '{"zh": "晴隆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-07-05', 'CN-09-07', 'DEPENDENT', 4, 'CN', '{"zh": "望谟县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-07-06', 'CN-09-07', 'DEPENDENT', 4, 'CN', '{"zh": "兴仁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-07-07', 'CN-09-07', 'DEPENDENT', 4, 'CN', '{"zh": "兴义市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-07-08', 'CN-09-07', 'DEPENDENT', 4, 'CN', '{"zh": "贞丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-01', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "德江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-02', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "江口县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-03', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "石阡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-04', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "思南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-05', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "松桃县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-06', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "铜仁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-07', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "万山特区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-08', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "沿河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-09', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "印江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-08-10', 'CN-09-08', 'DEPENDENT', 4, 'CN', '{"zh": "玉屏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-01', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "赤水市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-02', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "道真县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-03', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "凤冈县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-04', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "红花岗区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-05', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "汇川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-06', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "湄潭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-07', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "仁怀市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-08', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "绥阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-09', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "桐梓县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-10', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "务川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-11', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "习水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-12', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "余庆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-13', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "正安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-09-09-14', 'CN-09-09', 'DEPENDENT', 4, 'CN', '{"zh": "遵义县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-08-01', 'CN-10-08', 'DEPENDENT', 4, 'CN', '{"zh": "龙华区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-08-02', 'CN-10-08', 'DEPENDENT', 4, 'CN', '{"zh": "美兰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-08-03', 'CN-10-08', 'DEPENDENT', 4, 'CN', '{"zh": "琼山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-08-04', 'CN-10-08', 'DEPENDENT', 4, 'CN', '{"zh": "秀英区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-14-01', 'CN-10-14', 'DEPENDENT', 4, 'CN', '{"zh": "南沙群岛"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-14-02', 'CN-10-14', 'DEPENDENT', 4, 'CN', '{"zh": "西沙群岛"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-10-14-03', 'CN-10-14', 'DEPENDENT', 4, 'CN', '{"zh": "中沙群岛"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-01', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "安国市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-02', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "安新县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-03', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "北市区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-04', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "博野县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-05', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "定兴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-06', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "定州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-07', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "阜平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-08', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "高碑店市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-09', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "高阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-10', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "涞水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-11', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "涞源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-12', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "蠡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-13', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "满城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-14', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "南市区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-15', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "清苑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-16', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "曲阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-17', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "容城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-18', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "顺平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-19', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "唐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-20', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "望都县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-21', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "新市区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-22', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "雄县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-23', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "徐水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-24', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "易县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-01-25', 'CN-11-01', 'DEPENDENT', 4, 'CN', '{"zh": "涿州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-01', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "沧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-02', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "东光县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-03', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "海兴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-04', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "河间市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-05', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "黄骅市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-06', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "孟村县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-07', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "南皮县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-08', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "泊头市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-09', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "青县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-10', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "任丘市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-11', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "肃宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-12', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "吴桥县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-13', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "献县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-14', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "新华区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-15', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "盐山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-02-16', 'CN-11-02', 'DEPENDENT', 4, 'CN', '{"zh": "运河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-01', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "承德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-02', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "丰宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-03', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "宽城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-04', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "隆化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-05', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "滦平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-06', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "平泉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-07', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "双滦区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-08', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "双桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-09', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "围场县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-10', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "兴隆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-03-11', 'CN-11-03', 'DEPENDENT', 4, 'CN', '{"zh": "鹰手营子矿区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-01', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "成安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-02', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "磁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-03', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "丛台区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-04', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "大名县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-05', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "肥乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-06', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "峰峰矿区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-07', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "复兴区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-08', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "馆陶县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-09', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "广平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-10', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "邯郸县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-11', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "邯山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-12', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "鸡泽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-13', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "临漳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-14', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "邱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-15', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "曲周县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-16', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "涉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-17', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "魏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-18', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "武安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-04-19', 'CN-11-04', 'DEPENDENT', 4, 'CN', '{"zh": "永年县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-01', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "安平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-02', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "阜城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-03', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "故城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-04', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "冀州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-05', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "景县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-06', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "饶阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-07', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "深州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-08', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "桃城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-09', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "武强县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-10', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "武邑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-05-11', 'CN-11-05', 'DEPENDENT', 4, 'CN', '{"zh": "枣强县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-01', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "安次区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-02', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "霸州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-03', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "大厂县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-04', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "大城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-05', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "固安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-06', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "广阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-07', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "三河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-08', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "文安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-09', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "香河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-06-10', 'CN-11-06', 'DEPENDENT', 4, 'CN', '{"zh": "永清县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-07-01', 'CN-11-07', 'DEPENDENT', 4, 'CN', '{"zh": "北戴河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-07-02', 'CN-11-07', 'DEPENDENT', 4, 'CN', '{"zh": "昌黎县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-07-03', 'CN-11-07', 'DEPENDENT', 4, 'CN', '{"zh": "抚宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-07-04', 'CN-11-07', 'DEPENDENT', 4, 'CN', '{"zh": "海港区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-07-05', 'CN-11-07', 'DEPENDENT', 4, 'CN', '{"zh": "卢龙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-07-06', 'CN-11-07', 'DEPENDENT', 4, 'CN', '{"zh": "青龙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-07-07', 'CN-11-07', 'DEPENDENT', 4, 'CN', '{"zh": "山海关区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-01', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "高邑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-02', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "藁城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-03', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "晋州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-04', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "井陉矿区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-05', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "井陉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-06', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "灵寿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-07', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "鹿泉区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-08', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "栾城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-09', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "平山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-10', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "桥西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-11', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "深泽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-12', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "无极县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-13', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "辛集市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-14', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "新华区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-15', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "新乐市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-16', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "行唐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-17', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "裕华区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-18', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "元氏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-19', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "赞皇县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-20', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "长安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-21', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "赵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-08-22', 'CN-11-08', 'DEPENDENT', 4, 'CN', '{"zh": "正定县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-01', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "丰南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-02', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "丰润区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-03', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "古冶区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-04', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "开平区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-05', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "乐亭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-06', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "路北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-07', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "路南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-08', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "滦南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-09', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "滦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-10', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "迁安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-11', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "迁西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-12', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "唐海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-13', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "玉田县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-09-14', 'CN-11-09', 'DEPENDENT', 4, 'CN', '{"zh": "遵化市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-01', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "柏乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-02', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "广宗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-03', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "巨鹿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-04', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "临城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-05', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "临西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-06', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "隆尧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-07', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "南宫市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-08', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "南和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-09', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "内丘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-10', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "宁晋县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-11', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "平乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-12', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "桥东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-13', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "桥西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-14', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "清河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-15', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "任县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-16', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "沙河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-17', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "威县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-18', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "新河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-10-19', 'CN-11-10', 'DEPENDENT', 4, 'CN', '{"zh": "邢台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-01', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "赤城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-02', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "崇礼县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-03', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "沽源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-04', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "怀安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-05', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "怀来县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-06', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "康保县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-07', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "桥东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-08', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "桥西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-09', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "尚义县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-10', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "万全县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-11', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "蔚县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-12', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "下花园区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-13', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "宣化区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-14', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "宣化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-15', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "阳原县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-16', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "张北县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-11-11-17', 'CN-11-11', 'DEPENDENT', 4, 'CN', '{"zh": "涿鹿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01-01', 'CN-12-01', 'DEPENDENT', 4, 'CN', '{"zh": "安阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01-02', 'CN-12-01', 'DEPENDENT', 4, 'CN', '{"zh": "北关区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01-03', 'CN-12-01', 'DEPENDENT', 4, 'CN', '{"zh": "滑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01-04', 'CN-12-01', 'DEPENDENT', 4, 'CN', '{"zh": "林州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01-05', 'CN-12-01', 'DEPENDENT', 4, 'CN', '{"zh": "龙安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01-06', 'CN-12-01', 'DEPENDENT', 4, 'CN', '{"zh": "内黄县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01-07', 'CN-12-01', 'DEPENDENT', 4, 'CN', '{"zh": "汤阴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01-08', 'CN-12-01', 'DEPENDENT', 4, 'CN', '{"zh": "文峰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-01-09', 'CN-12-01', 'DEPENDENT', 4, 'CN', '{"zh": "殷都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-02-01', 'CN-12-02', 'DEPENDENT', 4, 'CN', '{"zh": "鹤山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-02-02', 'CN-12-02', 'DEPENDENT', 4, 'CN', '{"zh": "浚县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-02-03', 'CN-12-02', 'DEPENDENT', 4, 'CN', '{"zh": "淇滨区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-02-04', 'CN-12-02', 'DEPENDENT', 4, 'CN', '{"zh": "淇县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-02-05', 'CN-12-02', 'DEPENDENT', 4, 'CN', '{"zh": "山城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-01', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "博爱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-02', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "济源市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-03', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "解放区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-04', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "马村区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-05', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "孟州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-06', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "沁阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-07', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "山阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-08', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "温县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-09', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "武陟县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-10', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "修武县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-03-11', 'CN-12-03', 'DEPENDENT', 4, 'CN', '{"zh": "中站区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-01', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "鼓楼区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-02', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "金明区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-03', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "开封县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-04', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "兰考县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-05', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "龙亭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-06', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "杞县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-07', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "顺河回族区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-08', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "通许县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-09', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "尉氏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-04-10', 'CN-12-04', 'DEPENDENT', 4, 'CN', '{"zh": "禹王台区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-01', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "瀍河回族区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-02', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "吉利区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-03', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "涧西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-04', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "老城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-05', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "栾川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-06', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "洛龙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-07', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "洛宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-08', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "孟津县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-09', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "汝阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-10', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "嵩县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-11', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "西工区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-12', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "新安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-13', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "偃师市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-14', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "伊川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-05-15', 'CN-12-05', 'DEPENDENT', 4, 'CN', '{"zh": "宜阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-06-01', 'CN-12-06', 'DEPENDENT', 4, 'CN', '{"zh": "临颍县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-06-02', 'CN-12-06', 'DEPENDENT', 4, 'CN', '{"zh": "舞阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-06-03', 'CN-12-06', 'DEPENDENT', 4, 'CN', '{"zh": "郾城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-06-04', 'CN-12-06', 'DEPENDENT', 4, 'CN', '{"zh": "源汇区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-06-05', 'CN-12-06', 'DEPENDENT', 4, 'CN', '{"zh": "召陵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-01', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "邓州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-02', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "方城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-03', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "南召县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-04', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "内乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-05', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "社旗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-06', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "唐河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-07', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "桐柏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-08', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "宛城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-09', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "卧龙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-10', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "西峡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-11', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "淅川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-12', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "新野县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-07-13', 'CN-12-07', 'DEPENDENT', 4, 'CN', '{"zh": "镇平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-01', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "宝丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-02', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "郏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-03', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "鲁山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-04', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "汝州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-05', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "石龙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-06', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "卫东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-07', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "舞钢市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-08', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "新华区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-09', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "叶县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-08-10', 'CN-12-08', 'DEPENDENT', 4, 'CN', '{"zh": "湛河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-09-01', 'CN-12-09', 'DEPENDENT', 4, 'CN', '{"zh": "范县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-09-02', 'CN-12-09', 'DEPENDENT', 4, 'CN', '{"zh": "华龙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-09-03', 'CN-12-09', 'DEPENDENT', 4, 'CN', '{"zh": "南乐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-09-04', 'CN-12-09', 'DEPENDENT', 4, 'CN', '{"zh": "濮阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-09-05', 'CN-12-09', 'DEPENDENT', 4, 'CN', '{"zh": "清丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-09-06', 'CN-12-09', 'DEPENDENT', 4, 'CN', '{"zh": "台前县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-10-01', 'CN-12-10', 'DEPENDENT', 4, 'CN', '{"zh": "湖滨区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-10-02', 'CN-12-10', 'DEPENDENT', 4, 'CN', '{"zh": "灵宝市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-10-03', 'CN-12-10', 'DEPENDENT', 4, 'CN', '{"zh": "卢氏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-10-04', 'CN-12-10', 'DEPENDENT', 4, 'CN', '{"zh": "渑池县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-10-05', 'CN-12-10', 'DEPENDENT', 4, 'CN', '{"zh": "陕县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-10-06', 'CN-12-10', 'DEPENDENT', 4, 'CN', '{"zh": "义马市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11-01', 'CN-12-11', 'DEPENDENT', 4, 'CN', '{"zh": "梁园区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11-02', 'CN-12-11', 'DEPENDENT', 4, 'CN', '{"zh": "民权县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11-03', 'CN-12-11', 'DEPENDENT', 4, 'CN', '{"zh": "宁陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11-04', 'CN-12-11', 'DEPENDENT', 4, 'CN', '{"zh": "睢县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11-05', 'CN-12-11', 'DEPENDENT', 4, 'CN', '{"zh": "睢阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11-06', 'CN-12-11', 'DEPENDENT', 4, 'CN', '{"zh": "夏邑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11-07', 'CN-12-11', 'DEPENDENT', 4, 'CN', '{"zh": "永城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11-08', 'CN-12-11', 'DEPENDENT', 4, 'CN', '{"zh": "虞城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-11-09', 'CN-12-11', 'DEPENDENT', 4, 'CN', '{"zh": "柘城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-01', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "封丘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-02', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "凤泉区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-03', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "红旗区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-04', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "辉县市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-05', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "获嘉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-06', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "牧野区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-07', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "卫滨区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-08', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "卫辉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-09', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "新乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-10', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "延津县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-11', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "原阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-12-12', 'CN-12-12', 'DEPENDENT', 4, 'CN', '{"zh": "长垣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-01', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "固始县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-02', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "光山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-03', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "淮滨县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-04', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "潢川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-05', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "罗山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-06', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "平桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-07', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "商城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-08', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "浉河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-09', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "息县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-13-10', 'CN-12-13', 'DEPENDENT', 4, 'CN', '{"zh": "新县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-14-01', 'CN-12-14', 'DEPENDENT', 4, 'CN', '{"zh": "魏都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-14-02', 'CN-12-14', 'DEPENDENT', 4, 'CN', '{"zh": "襄城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-14-03', 'CN-12-14', 'DEPENDENT', 4, 'CN', '{"zh": "许昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-14-04', 'CN-12-14', 'DEPENDENT', 4, 'CN', '{"zh": "鄢陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-14-05', 'CN-12-14', 'DEPENDENT', 4, 'CN', '{"zh": "禹州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-14-06', 'CN-12-14', 'DEPENDENT', 4, 'CN', '{"zh": "长葛市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-01', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "登封市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-02', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "二七区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-03', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "巩义市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-04', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "管城回族区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-05', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "惠济区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-06', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "金水区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-07', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "上街区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-08', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "新密市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-09', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "新郑市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-10', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "荥阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-11', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "中牟县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-15-12', 'CN-12-15', 'DEPENDENT', 4, 'CN', '{"zh": "中原区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-01', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "川汇区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-02', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "郸城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-03', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "扶沟县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-04', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "淮阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-05', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "鹿邑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-06', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "商水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-07', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "沈丘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-08', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "太康县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-09', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "西华县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-16-10', 'CN-12-16', 'DEPENDENT', 4, 'CN', '{"zh": "项城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-01', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "泌阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-02', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "平舆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-03', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "确山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-04', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "汝南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-05', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "上蔡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-06', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "遂平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-07', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "西平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-08', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "新蔡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-09', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "驿城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-12-17-10', 'CN-12-17', 'DEPENDENT', 4, 'CN', '{"zh": "正阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01-01', 'CN-13-01', 'DEPENDENT', 4, 'CN', '{"zh": "大同区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01-02', 'CN-13-01', 'DEPENDENT', 4, 'CN', '{"zh": "杜尔伯特县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01-03', 'CN-13-01', 'DEPENDENT', 4, 'CN', '{"zh": "红岗区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01-04', 'CN-13-01', 'DEPENDENT', 4, 'CN', '{"zh": "林甸县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01-05', 'CN-13-01', 'DEPENDENT', 4, 'CN', '{"zh": "龙凤区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01-06', 'CN-13-01', 'DEPENDENT', 4, 'CN', '{"zh": "让胡路区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01-07', 'CN-13-01', 'DEPENDENT', 4, 'CN', '{"zh": "萨尔图区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01-08', 'CN-13-01', 'DEPENDENT', 4, 'CN', '{"zh": "肇源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-01-09', 'CN-13-01', 'DEPENDENT', 4, 'CN', '{"zh": "肇州县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-02-01', 'CN-13-02', 'DEPENDENT', 4, 'CN', '{"zh": "呼玛县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-02-02', 'CN-13-02', 'DEPENDENT', 4, 'CN', '{"zh": "呼中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-02-03', 'CN-13-02', 'DEPENDENT', 4, 'CN', '{"zh": "加格达奇区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-02-04', 'CN-13-02', 'DEPENDENT', 4, 'CN', '{"zh": "漠河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-02-05', 'CN-13-02', 'DEPENDENT', 4, 'CN', '{"zh": "松岭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-02-06', 'CN-13-02', 'DEPENDENT', 4, 'CN', '{"zh": "塔河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-02-07', 'CN-13-02', 'DEPENDENT', 4, 'CN', '{"zh": "新林区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-01', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "阿城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-02', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "巴彦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-03', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "宾县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-04', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "道里区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-05', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "道外区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-06', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "方正县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-07', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "呼兰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-08', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "木兰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-09', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "南岗区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-10', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "平房区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-11', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "尚志市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-12', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "双城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-13', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "松北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-14', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "通河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-15', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "五常市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-16', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "香坊区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-17', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "延寿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-03-18', 'CN-13-03', 'DEPENDENT', 4, 'CN', '{"zh": "依兰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-04-01', 'CN-13-04', 'DEPENDENT', 4, 'CN', '{"zh": "东山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-04-02', 'CN-13-04', 'DEPENDENT', 4, 'CN', '{"zh": "工农区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-04-03', 'CN-13-04', 'DEPENDENT', 4, 'CN', '{"zh": "萝北县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-04-04', 'CN-13-04', 'DEPENDENT', 4, 'CN', '{"zh": "南山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-04-05', 'CN-13-04', 'DEPENDENT', 4, 'CN', '{"zh": "绥滨县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-04-06', 'CN-13-04', 'DEPENDENT', 4, 'CN', '{"zh": "向阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-04-07', 'CN-13-04', 'DEPENDENT', 4, 'CN', '{"zh": "兴安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-04-08', 'CN-13-04', 'DEPENDENT', 4, 'CN', '{"zh": "兴山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-05-01', 'CN-13-05', 'DEPENDENT', 4, 'CN', '{"zh": "爱辉区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-05-02', 'CN-13-05', 'DEPENDENT', 4, 'CN', '{"zh": "北安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-05-03', 'CN-13-05', 'DEPENDENT', 4, 'CN', '{"zh": "嫩江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-05-04', 'CN-13-05', 'DEPENDENT', 4, 'CN', '{"zh": "孙吴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-05-05', 'CN-13-05', 'DEPENDENT', 4, 'CN', '{"zh": "五大连池市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-05-06', 'CN-13-05', 'DEPENDENT', 4, 'CN', '{"zh": "逊克县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06-01', 'CN-13-06', 'DEPENDENT', 4, 'CN', '{"zh": "城子河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06-02', 'CN-13-06', 'DEPENDENT', 4, 'CN', '{"zh": "滴道区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06-03', 'CN-13-06', 'DEPENDENT', 4, 'CN', '{"zh": "恒山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06-04', 'CN-13-06', 'DEPENDENT', 4, 'CN', '{"zh": "虎林市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06-05', 'CN-13-06', 'DEPENDENT', 4, 'CN', '{"zh": "鸡东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06-06', 'CN-13-06', 'DEPENDENT', 4, 'CN', '{"zh": "鸡冠区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06-07', 'CN-13-06', 'DEPENDENT', 4, 'CN', '{"zh": "梨树区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06-08', 'CN-13-06', 'DEPENDENT', 4, 'CN', '{"zh": "麻山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-06-09', 'CN-13-06', 'DEPENDENT', 4, 'CN', '{"zh": "密山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-01', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "东风区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-02', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "抚远县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-03', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "富锦市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-04', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "桦川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-05', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "桦南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-06', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "郊区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-07', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "前进区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-08', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "汤原县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-09', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "同江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-07-10', 'CN-13-07', 'DEPENDENT', 4, 'CN', '{"zh": "向阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-01', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "爱民区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-02', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "东安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-03', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "东宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-04', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "海林市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-05', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "林口县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-06', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "穆棱市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-07', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "宁安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-08', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "绥芬河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-09', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "西安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-08-10', 'CN-13-08', 'DEPENDENT', 4, 'CN', '{"zh": "阳明区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-09-01', 'CN-13-09', 'DEPENDENT', 4, 'CN', '{"zh": "勃利县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-09-02', 'CN-13-09', 'DEPENDENT', 4, 'CN', '{"zh": "茄子河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-09-03', 'CN-13-09', 'DEPENDENT', 4, 'CN', '{"zh": "桃山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-09-04', 'CN-13-09', 'DEPENDENT', 4, 'CN', '{"zh": "新兴区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-01', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "昂昂溪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-02', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "拜泉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-03', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "富拉尔基区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-04', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "富裕县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-05', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "甘南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-06', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "建华区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-07', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "克东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-08', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "克山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-09', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "龙江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-10', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "龙沙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-11', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "梅里斯达斡尔族区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-12', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "讷河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-13', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "碾子山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-14', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "泰来县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-15', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "铁锋区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-10-16', 'CN-13-10', 'DEPENDENT', 4, 'CN', '{"zh": "依安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-11-01', 'CN-13-11', 'DEPENDENT', 4, 'CN', '{"zh": "宝清县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-11-02', 'CN-13-11', 'DEPENDENT', 4, 'CN', '{"zh": "宝山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-11-03', 'CN-13-11', 'DEPENDENT', 4, 'CN', '{"zh": "集贤县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-11-04', 'CN-13-11', 'DEPENDENT', 4, 'CN', '{"zh": "尖山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-11-05', 'CN-13-11', 'DEPENDENT', 4, 'CN', '{"zh": "岭东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-11-06', 'CN-13-11', 'DEPENDENT', 4, 'CN', '{"zh": "饶河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-11-07', 'CN-13-11', 'DEPENDENT', 4, 'CN', '{"zh": "四方台区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-11-08', 'CN-13-11', 'DEPENDENT', 4, 'CN', '{"zh": "友谊县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-01', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "安达市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-02', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "北林区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-03', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "海伦市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-04', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "兰西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-05', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "明水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-06', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "青冈县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-07', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "庆安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-08', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "绥棱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-09', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "望奎县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-12-10', 'CN-13-12', 'DEPENDENT', 4, 'CN', '{"zh": "肇东市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-01', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "翠峦区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-02', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "带岭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-03', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "红星区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-04', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "嘉荫县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-05', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "金山屯区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-06', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "美溪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-07', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "南岔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-08', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "上甘岭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-09', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "汤旺河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-10', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "铁力市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-11', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "乌马河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-12', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "乌伊岭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-13', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "五营区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-14', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "西林区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-15', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "新青区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-16', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "伊春区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-13-13-17', 'CN-13-13', 'DEPENDENT', 4, 'CN', '{"zh": "友好区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-01-01', 'CN-14-01', 'DEPENDENT', 4, 'CN', '{"zh": "鄂城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-01-02', 'CN-14-01', 'DEPENDENT', 4, 'CN', '{"zh": "华容区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-01-03', 'CN-14-01', 'DEPENDENT', 4, 'CN', '{"zh": "梁子湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-02-01', 'CN-14-02', 'DEPENDENT', 4, 'CN', '{"zh": "巴东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-02-02', 'CN-14-02', 'DEPENDENT', 4, 'CN', '{"zh": "恩施市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-02-03', 'CN-14-02', 'DEPENDENT', 4, 'CN', '{"zh": "鹤峰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-02-04', 'CN-14-02', 'DEPENDENT', 4, 'CN', '{"zh": "建始县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-02-05', 'CN-14-02', 'DEPENDENT', 4, 'CN', '{"zh": "来凤县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-02-06', 'CN-14-02', 'DEPENDENT', 4, 'CN', '{"zh": "利川市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-02-07', 'CN-14-02', 'DEPENDENT', 4, 'CN', '{"zh": "咸丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-02-08', 'CN-14-02', 'DEPENDENT', 4, 'CN', '{"zh": "宣恩县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-01', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "红安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-02', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "黄梅县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-03', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "黄州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-04', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "罗田县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-05', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "麻城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-06', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "蕲春县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-07', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "团风县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-08', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "武穴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-09', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "浠水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-03-10', 'CN-14-03', 'DEPENDENT', 4, 'CN', '{"zh": "英山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-04-01', 'CN-14-04', 'DEPENDENT', 4, 'CN', '{"zh": "大冶市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-04-02', 'CN-14-04', 'DEPENDENT', 4, 'CN', '{"zh": "黄石港区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-04-03', 'CN-14-04', 'DEPENDENT', 4, 'CN', '{"zh": "铁山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-04-04', 'CN-14-04', 'DEPENDENT', 4, 'CN', '{"zh": "西塞山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-04-05', 'CN-14-04', 'DEPENDENT', 4, 'CN', '{"zh": "下陆区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-04-06', 'CN-14-04', 'DEPENDENT', 4, 'CN', '{"zh": "阳新县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-05-01', 'CN-14-05', 'DEPENDENT', 4, 'CN', '{"zh": "东宝区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-05-02', 'CN-14-05', 'DEPENDENT', 4, 'CN', '{"zh": "掇刀区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-05-03', 'CN-14-05', 'DEPENDENT', 4, 'CN', '{"zh": "京山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-05-04', 'CN-14-05', 'DEPENDENT', 4, 'CN', '{"zh": "沙洋县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-05-05', 'CN-14-05', 'DEPENDENT', 4, 'CN', '{"zh": "钟祥市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-06-01', 'CN-14-06', 'DEPENDENT', 4, 'CN', '{"zh": "公安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-06-02', 'CN-14-06', 'DEPENDENT', 4, 'CN', '{"zh": "洪湖市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-06-03', 'CN-14-06', 'DEPENDENT', 4, 'CN', '{"zh": "监利县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-06-04', 'CN-14-06', 'DEPENDENT', 4, 'CN', '{"zh": "江陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-06-05', 'CN-14-06', 'DEPENDENT', 4, 'CN', '{"zh": "荆州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-06-06', 'CN-14-06', 'DEPENDENT', 4, 'CN', '{"zh": "沙市区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-06-07', 'CN-14-06', 'DEPENDENT', 4, 'CN', '{"zh": "石首市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-06-08', 'CN-14-06', 'DEPENDENT', 4, 'CN', '{"zh": "松滋市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-09-01', 'CN-14-09', 'DEPENDENT', 4, 'CN', '{"zh": "丹江口市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-09-02', 'CN-14-09', 'DEPENDENT', 4, 'CN', '{"zh": "房县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-09-03', 'CN-14-09', 'DEPENDENT', 4, 'CN', '{"zh": "茅箭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-09-04', 'CN-14-09', 'DEPENDENT', 4, 'CN', '{"zh": "郧西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-09-05', 'CN-14-09', 'DEPENDENT', 4, 'CN', '{"zh": "郧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-09-06', 'CN-14-09', 'DEPENDENT', 4, 'CN', '{"zh": "张湾区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-09-07', 'CN-14-09', 'DEPENDENT', 4, 'CN', '{"zh": "竹山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-09-08', 'CN-14-09', 'DEPENDENT', 4, 'CN', '{"zh": "竹溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-10-01', 'CN-14-10', 'DEPENDENT', 4, 'CN', '{"zh": "曾都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-10-02', 'CN-14-10', 'DEPENDENT', 4, 'CN', '{"zh": "广水市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-01', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "蔡甸区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-02', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "东西湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-03', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "汉南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-04', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "汉阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-05', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "洪山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-06', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "黄陂区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-07', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "江岸区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-08', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "江汉区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-09', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "江夏区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-10', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "硚口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-11', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "青山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-12', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "武昌区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-12-13', 'CN-14-12', 'DEPENDENT', 4, 'CN', '{"zh": "新洲区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-14-01', 'CN-14-14', 'DEPENDENT', 4, 'CN', '{"zh": "赤壁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-14-02', 'CN-14-14', 'DEPENDENT', 4, 'CN', '{"zh": "崇阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-14-03', 'CN-14-14', 'DEPENDENT', 4, 'CN', '{"zh": "嘉鱼县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-14-04', 'CN-14-14', 'DEPENDENT', 4, 'CN', '{"zh": "通城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-14-05', 'CN-14-14', 'DEPENDENT', 4, 'CN', '{"zh": "通山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-14-06', 'CN-14-14', 'DEPENDENT', 4, 'CN', '{"zh": "咸安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15-01', 'CN-14-15', 'DEPENDENT', 4, 'CN', '{"zh": "保康县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15-02', 'CN-14-15', 'DEPENDENT', 4, 'CN', '{"zh": "樊城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15-03', 'CN-14-15', 'DEPENDENT', 4, 'CN', '{"zh": "谷城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15-04', 'CN-14-15', 'DEPENDENT', 4, 'CN', '{"zh": "老河口市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15-05', 'CN-14-15', 'DEPENDENT', 4, 'CN', '{"zh": "南漳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15-06', 'CN-14-15', 'DEPENDENT', 4, 'CN', '{"zh": "襄城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15-07', 'CN-14-15', 'DEPENDENT', 4, 'CN', '{"zh": "襄州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15-08', 'CN-14-15', 'DEPENDENT', 4, 'CN', '{"zh": "宜城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-15-09', 'CN-14-15', 'DEPENDENT', 4, 'CN', '{"zh": "枣阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-16-01', 'CN-14-16', 'DEPENDENT', 4, 'CN', '{"zh": "安陆市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-16-02', 'CN-14-16', 'DEPENDENT', 4, 'CN', '{"zh": "大悟县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-16-03', 'CN-14-16', 'DEPENDENT', 4, 'CN', '{"zh": "汉川市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-16-04', 'CN-14-16', 'DEPENDENT', 4, 'CN', '{"zh": "孝昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-16-05', 'CN-14-16', 'DEPENDENT', 4, 'CN', '{"zh": "孝南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-16-06', 'CN-14-16', 'DEPENDENT', 4, 'CN', '{"zh": "应城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-16-07', 'CN-14-16', 'DEPENDENT', 4, 'CN', '{"zh": "云梦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-01', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "当阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-02', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "点军区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-03', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "五峰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-04', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "伍家岗区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-05', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "西陵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-06', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "猇亭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-07', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "兴山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-08', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "夷陵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-09', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "宜都市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-10', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "远安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-11', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "长阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-12', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "枝江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-14-17-13', 'CN-14-17', 'DEPENDENT', 4, 'CN', '{"zh": "秭归县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01-01', 'CN-15-01', 'DEPENDENT', 4, 'CN', '{"zh": "安乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01-02', 'CN-15-01', 'DEPENDENT', 4, 'CN', '{"zh": "鼎城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01-03', 'CN-15-01', 'DEPENDENT', 4, 'CN', '{"zh": "汉寿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01-04', 'CN-15-01', 'DEPENDENT', 4, 'CN', '{"zh": "津市市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01-05', 'CN-15-01', 'DEPENDENT', 4, 'CN', '{"zh": "澧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01-06', 'CN-15-01', 'DEPENDENT', 4, 'CN', '{"zh": "临澧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01-07', 'CN-15-01', 'DEPENDENT', 4, 'CN', '{"zh": "石门县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01-08', 'CN-15-01', 'DEPENDENT', 4, 'CN', '{"zh": "桃源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-01-09', 'CN-15-01', 'DEPENDENT', 4, 'CN', '{"zh": "武陵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-01', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "安仁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-02', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "北湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-03', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "桂东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-04', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "桂阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-05', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "嘉禾县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-06', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "临武县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-07', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "汝城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-08', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "苏仙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-09', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "宜章县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-10', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "永兴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-02-11', 'CN-15-02', 'DEPENDENT', 4, 'CN', '{"zh": "资兴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-01', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "常宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-02', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "衡东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-03', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "衡南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-04', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "衡山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-05', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "衡阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-06', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "耒阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-07', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "南岳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-08', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "祁东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-09', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "石鼓区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-10', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "雁峰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-11', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "蒸湘区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-03-12', 'CN-15-03', 'DEPENDENT', 4, 'CN', '{"zh": "珠晖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-01', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "辰溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-02', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "鹤城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-03', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "洪江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-04', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "会同县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-05', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "靖州县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-06', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "麻阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-07', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "通道县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-08', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "新晃县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-09', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "溆浦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-10', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "沅陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-11', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "芷江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-04-12', 'CN-15-04', 'DEPENDENT', 4, 'CN', '{"zh": "中方县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-05-01', 'CN-15-05', 'DEPENDENT', 4, 'CN', '{"zh": "冷水江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-05-02', 'CN-15-05', 'DEPENDENT', 4, 'CN', '{"zh": "涟源市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-05-03', 'CN-15-05', 'DEPENDENT', 4, 'CN', '{"zh": "娄星区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-05-04', 'CN-15-05', 'DEPENDENT', 4, 'CN', '{"zh": "双峰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-05-05', 'CN-15-05', 'DEPENDENT', 4, 'CN', '{"zh": "新化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-01', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "北塔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-02', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "城步县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-03', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "大祥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-04', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "洞口县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-05', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "隆回县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-06', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "邵东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-07', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "邵阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-08', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "双清区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-09', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "绥宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-10', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "武冈市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-11', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "新宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-06-12', 'CN-15-06', 'DEPENDENT', 4, 'CN', '{"zh": "新邵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-07-01', 'CN-15-07', 'DEPENDENT', 4, 'CN', '{"zh": "韶山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-07-02', 'CN-15-07', 'DEPENDENT', 4, 'CN', '{"zh": "湘潭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-07-03', 'CN-15-07', 'DEPENDENT', 4, 'CN', '{"zh": "湘乡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-07-04', 'CN-15-07', 'DEPENDENT', 4, 'CN', '{"zh": "雨湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-07-05', 'CN-15-07', 'DEPENDENT', 4, 'CN', '{"zh": "岳塘区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-08-01', 'CN-15-08', 'DEPENDENT', 4, 'CN', '{"zh": "保靖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-08-02', 'CN-15-08', 'DEPENDENT', 4, 'CN', '{"zh": "凤凰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-08-03', 'CN-15-08', 'DEPENDENT', 4, 'CN', '{"zh": "古丈县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-08-04', 'CN-15-08', 'DEPENDENT', 4, 'CN', '{"zh": "花垣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-08-05', 'CN-15-08', 'DEPENDENT', 4, 'CN', '{"zh": "吉首市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-08-06', 'CN-15-08', 'DEPENDENT', 4, 'CN', '{"zh": "龙山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-08-07', 'CN-15-08', 'DEPENDENT', 4, 'CN', '{"zh": "泸溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-08-08', 'CN-15-08', 'DEPENDENT', 4, 'CN', '{"zh": "永顺县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-09-01', 'CN-15-09', 'DEPENDENT', 4, 'CN', '{"zh": "安化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-09-02', 'CN-15-09', 'DEPENDENT', 4, 'CN', '{"zh": "赫山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-09-03', 'CN-15-09', 'DEPENDENT', 4, 'CN', '{"zh": "南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-09-04', 'CN-15-09', 'DEPENDENT', 4, 'CN', '{"zh": "桃江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-09-05', 'CN-15-09', 'DEPENDENT', 4, 'CN', '{"zh": "沅江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-09-06', 'CN-15-09', 'DEPENDENT', 4, 'CN', '{"zh": "资阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-01', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "道县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-02', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "东安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-03', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "江华县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-04', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "江永县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-05', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "蓝山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-06', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "冷水滩区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-07', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "零陵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-08', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "宁远县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-09', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "祁阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-10', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "双牌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-10-11', 'CN-15-10', 'DEPENDENT', 4, 'CN', '{"zh": "新田县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11-01', 'CN-15-11', 'DEPENDENT', 4, 'CN', '{"zh": "华容县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11-02', 'CN-15-11', 'DEPENDENT', 4, 'CN', '{"zh": "君山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11-03', 'CN-15-11', 'DEPENDENT', 4, 'CN', '{"zh": "临湘市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11-04', 'CN-15-11', 'DEPENDENT', 4, 'CN', '{"zh": "汨罗市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11-05', 'CN-15-11', 'DEPENDENT', 4, 'CN', '{"zh": "平江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11-06', 'CN-15-11', 'DEPENDENT', 4, 'CN', '{"zh": "湘阴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11-07', 'CN-15-11', 'DEPENDENT', 4, 'CN', '{"zh": "岳阳楼区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11-08', 'CN-15-11', 'DEPENDENT', 4, 'CN', '{"zh": "岳阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-11-09', 'CN-15-11', 'DEPENDENT', 4, 'CN', '{"zh": "云溪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-12-01', 'CN-15-12', 'DEPENDENT', 4, 'CN', '{"zh": "慈利县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-12-02', 'CN-15-12', 'DEPENDENT', 4, 'CN', '{"zh": "桑植县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-12-03', 'CN-15-12', 'DEPENDENT', 4, 'CN', '{"zh": "武陵源区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-12-04', 'CN-15-12', 'DEPENDENT', 4, 'CN', '{"zh": "永定区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13-01', 'CN-15-13', 'DEPENDENT', 4, 'CN', '{"zh": "芙蓉区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13-02', 'CN-15-13', 'DEPENDENT', 4, 'CN', '{"zh": "开福区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13-03', 'CN-15-13', 'DEPENDENT', 4, 'CN', '{"zh": "浏阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13-04', 'CN-15-13', 'DEPENDENT', 4, 'CN', '{"zh": "宁乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13-05', 'CN-15-13', 'DEPENDENT', 4, 'CN', '{"zh": "天心区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13-06', 'CN-15-13', 'DEPENDENT', 4, 'CN', '{"zh": "望城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13-07', 'CN-15-13', 'DEPENDENT', 4, 'CN', '{"zh": "雨花区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13-08', 'CN-15-13', 'DEPENDENT', 4, 'CN', '{"zh": "岳麓区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-13-09', 'CN-15-13', 'DEPENDENT', 4, 'CN', '{"zh": "长沙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14-01', 'CN-15-14', 'DEPENDENT', 4, 'CN', '{"zh": "茶陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14-02', 'CN-15-14', 'DEPENDENT', 4, 'CN', '{"zh": "荷塘区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14-03', 'CN-15-14', 'DEPENDENT', 4, 'CN', '{"zh": "醴陵市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14-04', 'CN-15-14', 'DEPENDENT', 4, 'CN', '{"zh": "芦淞区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14-05', 'CN-15-14', 'DEPENDENT', 4, 'CN', '{"zh": "石峰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14-06', 'CN-15-14', 'DEPENDENT', 4, 'CN', '{"zh": "天元区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14-07', 'CN-15-14', 'DEPENDENT', 4, 'CN', '{"zh": "炎陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14-08', 'CN-15-14', 'DEPENDENT', 4, 'CN', '{"zh": "攸县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-15-14-09', 'CN-15-14', 'DEPENDENT', 4, 'CN', '{"zh": "株洲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-01-01', 'CN-16-01', 'DEPENDENT', 4, 'CN', '{"zh": "大安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-01-02', 'CN-16-01', 'DEPENDENT', 4, 'CN', '{"zh": "洮北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-01-03', 'CN-16-01', 'DEPENDENT', 4, 'CN', '{"zh": "洮南市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-01-04', 'CN-16-01', 'DEPENDENT', 4, 'CN', '{"zh": "通榆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-01-05', 'CN-16-01', 'DEPENDENT', 4, 'CN', '{"zh": "镇赉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-02-01', 'CN-16-02', 'DEPENDENT', 4, 'CN', '{"zh": "八道江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-02-02', 'CN-16-02', 'DEPENDENT', 4, 'CN', '{"zh": "抚松县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-02-03', 'CN-16-02', 'DEPENDENT', 4, 'CN', '{"zh": "江源区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-02-04', 'CN-16-02', 'DEPENDENT', 4, 'CN', '{"zh": "靖宇县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-02-05', 'CN-16-02', 'DEPENDENT', 4, 'CN', '{"zh": "临江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-02-06', 'CN-16-02', 'DEPENDENT', 4, 'CN', '{"zh": "长白县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03-01', 'CN-16-03', 'DEPENDENT', 4, 'CN', '{"zh": "昌邑区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03-02', 'CN-16-03', 'DEPENDENT', 4, 'CN', '{"zh": "船营区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03-03', 'CN-16-03', 'DEPENDENT', 4, 'CN', '{"zh": "丰满区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03-04', 'CN-16-03', 'DEPENDENT', 4, 'CN', '{"zh": "桦甸市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03-05', 'CN-16-03', 'DEPENDENT', 4, 'CN', '{"zh": "蛟河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03-06', 'CN-16-03', 'DEPENDENT', 4, 'CN', '{"zh": "龙潭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03-07', 'CN-16-03', 'DEPENDENT', 4, 'CN', '{"zh": "磐石市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03-08', 'CN-16-03', 'DEPENDENT', 4, 'CN', '{"zh": "舒兰市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-03-09', 'CN-16-03', 'DEPENDENT', 4, 'CN', '{"zh": "永吉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-04-01', 'CN-16-04', 'DEPENDENT', 4, 'CN', '{"zh": "东丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-04-02', 'CN-16-04', 'DEPENDENT', 4, 'CN', '{"zh": "东辽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-04-03', 'CN-16-04', 'DEPENDENT', 4, 'CN', '{"zh": "龙山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-04-04', 'CN-16-04', 'DEPENDENT', 4, 'CN', '{"zh": "西安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-05-01', 'CN-16-05', 'DEPENDENT', 4, 'CN', '{"zh": "公主岭市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-05-02', 'CN-16-05', 'DEPENDENT', 4, 'CN', '{"zh": "梨树县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-05-03', 'CN-16-05', 'DEPENDENT', 4, 'CN', '{"zh": "双辽市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-05-04', 'CN-16-05', 'DEPENDENT', 4, 'CN', '{"zh": "铁东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-05-05', 'CN-16-05', 'DEPENDENT', 4, 'CN', '{"zh": "铁西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-05-06', 'CN-16-05', 'DEPENDENT', 4, 'CN', '{"zh": "伊通县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-06-01', 'CN-16-06', 'DEPENDENT', 4, 'CN', '{"zh": "扶余县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-06-02', 'CN-16-06', 'DEPENDENT', 4, 'CN', '{"zh": "宁江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-06-03', 'CN-16-06', 'DEPENDENT', 4, 'CN', '{"zh": "前郭尔罗斯县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-06-04', 'CN-16-06', 'DEPENDENT', 4, 'CN', '{"zh": "乾安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-06-05', 'CN-16-06', 'DEPENDENT', 4, 'CN', '{"zh": "长岭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-07-01', 'CN-16-07', 'DEPENDENT', 4, 'CN', '{"zh": "东昌区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-07-02', 'CN-16-07', 'DEPENDENT', 4, 'CN', '{"zh": "二道江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-07-03', 'CN-16-07', 'DEPENDENT', 4, 'CN', '{"zh": "辉南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-07-04', 'CN-16-07', 'DEPENDENT', 4, 'CN', '{"zh": "集安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-07-05', 'CN-16-07', 'DEPENDENT', 4, 'CN', '{"zh": "柳河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-07-06', 'CN-16-07', 'DEPENDENT', 4, 'CN', '{"zh": "梅河口市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-07-07', 'CN-16-07', 'DEPENDENT', 4, 'CN', '{"zh": "通化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-08-01', 'CN-16-08', 'DEPENDENT', 4, 'CN', '{"zh": "安图县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-08-02', 'CN-16-08', 'DEPENDENT', 4, 'CN', '{"zh": "敦化市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-08-03', 'CN-16-08', 'DEPENDENT', 4, 'CN', '{"zh": "和龙市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-08-04', 'CN-16-08', 'DEPENDENT', 4, 'CN', '{"zh": "珲春市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-08-05', 'CN-16-08', 'DEPENDENT', 4, 'CN', '{"zh": "龙井市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-08-06', 'CN-16-08', 'DEPENDENT', 4, 'CN', '{"zh": "图们市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-08-07', 'CN-16-08', 'DEPENDENT', 4, 'CN', '{"zh": "汪清县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-08-08', 'CN-16-08', 'DEPENDENT', 4, 'CN', '{"zh": "延吉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-01', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "朝阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-02', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "德惠市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-03', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "二道区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-04', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "九台市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-05', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "宽城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-06', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "绿园区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-07', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "南关区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-08', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "农安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-09', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "双阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-16-09-10', 'CN-16-09', 'DEPENDENT', 4, 'CN', '{"zh": "榆树市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-01-01', 'CN-17-01', 'DEPENDENT', 4, 'CN', '{"zh": "金坛市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-01-02', 'CN-17-01', 'DEPENDENT', 4, 'CN', '{"zh": "溧阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-01-03', 'CN-17-01', 'DEPENDENT', 4, 'CN', '{"zh": "戚墅堰区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-01-04', 'CN-17-01', 'DEPENDENT', 4, 'CN', '{"zh": "天宁区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-01-05', 'CN-17-01', 'DEPENDENT', 4, 'CN', '{"zh": "武进区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-01-06', 'CN-17-01', 'DEPENDENT', 4, 'CN', '{"zh": "新北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-01-07', 'CN-17-01', 'DEPENDENT', 4, 'CN', '{"zh": "钟楼区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-02-01', 'CN-17-02', 'DEPENDENT', 4, 'CN', '{"zh": "楚州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-02-02', 'CN-17-02', 'DEPENDENT', 4, 'CN', '{"zh": "洪泽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-02-03', 'CN-17-02', 'DEPENDENT', 4, 'CN', '{"zh": "淮阴区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-02-04', 'CN-17-02', 'DEPENDENT', 4, 'CN', '{"zh": "金湖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-02-05', 'CN-17-02', 'DEPENDENT', 4, 'CN', '{"zh": "涟水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-02-06', 'CN-17-02', 'DEPENDENT', 4, 'CN', '{"zh": "清河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-02-07', 'CN-17-02', 'DEPENDENT', 4, 'CN', '{"zh": "清浦区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-02-08', 'CN-17-02', 'DEPENDENT', 4, 'CN', '{"zh": "盱眙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-03-01', 'CN-17-03', 'DEPENDENT', 4, 'CN', '{"zh": "东海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-03-02', 'CN-17-03', 'DEPENDENT', 4, 'CN', '{"zh": "赣榆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-03-03', 'CN-17-03', 'DEPENDENT', 4, 'CN', '{"zh": "灌南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-03-04', 'CN-17-03', 'DEPENDENT', 4, 'CN', '{"zh": "灌云县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-03-05', 'CN-17-03', 'DEPENDENT', 4, 'CN', '{"zh": "海州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-03-06', 'CN-17-03', 'DEPENDENT', 4, 'CN', '{"zh": "连云区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-03-07', 'CN-17-03', 'DEPENDENT', 4, 'CN', '{"zh": "新浦区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-01', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "高淳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-02', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "鼓楼区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-03', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "建邺区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-04', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "江宁区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-05', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "溧水区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-06', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "六合区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-07', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "浦口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-08', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "栖霞区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-09', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "秦淮区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-10', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "玄武区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-04-11', 'CN-17-04', 'DEPENDENT', 4, 'CN', '{"zh": "雨花台区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-05-01', 'CN-17-05', 'DEPENDENT', 4, 'CN', '{"zh": "崇川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-05-02', 'CN-17-05', 'DEPENDENT', 4, 'CN', '{"zh": "港闸区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-05-03', 'CN-17-05', 'DEPENDENT', 4, 'CN', '{"zh": "海安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-05-04', 'CN-17-05', 'DEPENDENT', 4, 'CN', '{"zh": "海门市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-05-05', 'CN-17-05', 'DEPENDENT', 4, 'CN', '{"zh": "启东市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-05-06', 'CN-17-05', 'DEPENDENT', 4, 'CN', '{"zh": "如东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-05-07', 'CN-17-05', 'DEPENDENT', 4, 'CN', '{"zh": "如皋市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-05-08', 'CN-17-05', 'DEPENDENT', 4, 'CN', '{"zh": "通州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06-01', 'CN-17-06', 'DEPENDENT', 4, 'CN', '{"zh": "常熟市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06-02', 'CN-17-06', 'DEPENDENT', 4, 'CN', '{"zh": "姑苏区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06-03', 'CN-17-06', 'DEPENDENT', 4, 'CN', '{"zh": "虎丘区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06-04', 'CN-17-06', 'DEPENDENT', 4, 'CN', '{"zh": "昆山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06-05', 'CN-17-06', 'DEPENDENT', 4, 'CN', '{"zh": "太仓市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06-06', 'CN-17-06', 'DEPENDENT', 4, 'CN', '{"zh": "吴江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06-07', 'CN-17-06', 'DEPENDENT', 4, 'CN', '{"zh": "吴中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06-08', 'CN-17-06', 'DEPENDENT', 4, 'CN', '{"zh": "相城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-06-09', 'CN-17-06', 'DEPENDENT', 4, 'CN', '{"zh": "张家港市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-07-01', 'CN-17-07', 'DEPENDENT', 4, 'CN', '{"zh": "沭阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-07-02', 'CN-17-07', 'DEPENDENT', 4, 'CN', '{"zh": "泗洪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-07-03', 'CN-17-07', 'DEPENDENT', 4, 'CN', '{"zh": "泗阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-07-04', 'CN-17-07', 'DEPENDENT', 4, 'CN', '{"zh": "宿城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-07-05', 'CN-17-07', 'DEPENDENT', 4, 'CN', '{"zh": "宿豫区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-08-01', 'CN-17-08', 'DEPENDENT', 4, 'CN', '{"zh": "高港区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-08-02', 'CN-17-08', 'DEPENDENT', 4, 'CN', '{"zh": "海陵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-08-03', 'CN-17-08', 'DEPENDENT', 4, 'CN', '{"zh": "姜堰市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-08-04', 'CN-17-08', 'DEPENDENT', 4, 'CN', '{"zh": "靖江市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-08-05', 'CN-17-08', 'DEPENDENT', 4, 'CN', '{"zh": "泰兴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-08-06', 'CN-17-08', 'DEPENDENT', 4, 'CN', '{"zh": "兴化市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-01', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "北塘区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-02', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "滨湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-03', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "崇安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-04', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "惠山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-05', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "江阴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-06', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "梁溪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-07', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "南长区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-08', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "锡山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-09', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "新吴区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-09-10', 'CN-17-09', 'DEPENDENT', 4, 'CN', '{"zh": "宜兴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-01', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-02', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "鼓楼区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-03', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "贾汪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-04', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "九里区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-05', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "沛县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-06', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "邳州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-07', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "泉山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-08', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "睢宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-09', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "铜山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-10', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "新沂市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-10-11', 'CN-17-10', 'DEPENDENT', 4, 'CN', '{"zh": "云龙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11-01', 'CN-17-11', 'DEPENDENT', 4, 'CN', '{"zh": "滨海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11-02', 'CN-17-11', 'DEPENDENT', 4, 'CN', '{"zh": "大丰市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11-03', 'CN-17-11', 'DEPENDENT', 4, 'CN', '{"zh": "东台市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11-04', 'CN-17-11', 'DEPENDENT', 4, 'CN', '{"zh": "阜宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11-05', 'CN-17-11', 'DEPENDENT', 4, 'CN', '{"zh": "建湖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11-06', 'CN-17-11', 'DEPENDENT', 4, 'CN', '{"zh": "射阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11-07', 'CN-17-11', 'DEPENDENT', 4, 'CN', '{"zh": "亭湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11-08', 'CN-17-11', 'DEPENDENT', 4, 'CN', '{"zh": "响水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-11-09', 'CN-17-11', 'DEPENDENT', 4, 'CN', '{"zh": "盐都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-12-01', 'CN-17-12', 'DEPENDENT', 4, 'CN', '{"zh": "宝应县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-12-02', 'CN-17-12', 'DEPENDENT', 4, 'CN', '{"zh": "高邮市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-12-03', 'CN-17-12', 'DEPENDENT', 4, 'CN', '{"zh": "广陵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-12-04', 'CN-17-12', 'DEPENDENT', 4, 'CN', '{"zh": "邗江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-12-05', 'CN-17-12', 'DEPENDENT', 4, 'CN', '{"zh": "江都市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-12-06', 'CN-17-12', 'DEPENDENT', 4, 'CN', '{"zh": "维扬区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-12-07', 'CN-17-12', 'DEPENDENT', 4, 'CN', '{"zh": "仪征市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-13-01', 'CN-17-13', 'DEPENDENT', 4, 'CN', '{"zh": "丹徒区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-13-02', 'CN-17-13', 'DEPENDENT', 4, 'CN', '{"zh": "丹阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-13-03', 'CN-17-13', 'DEPENDENT', 4, 'CN', '{"zh": "京口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-13-04', 'CN-17-13', 'DEPENDENT', 4, 'CN', '{"zh": "句容市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-13-05', 'CN-17-13', 'DEPENDENT', 4, 'CN', '{"zh": "润州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-17-13-06', 'CN-17-13', 'DEPENDENT', 4, 'CN', '{"zh": "扬中市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-01', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "崇仁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-02', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "东乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-03', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "广昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-04', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "金溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-05', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "乐安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-06', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "黎川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-07', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "临川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-08', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "南城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-09', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "南丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-10', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "宜黄县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-01-11', 'CN-18-01', 'DEPENDENT', 4, 'CN', '{"zh": "资溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-01', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "安远县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-02', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "崇义县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-03', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "大余县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-04', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "定南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-05', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "赣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-06', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "会昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-07', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "龙南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-08', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "南康市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-09', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "宁都县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-10', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "全南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-11', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "瑞金市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-12', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "上犹县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-13', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "石城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-14', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "信丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-15', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "兴国县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-16', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "寻乌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-17', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "于都县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-02-18', 'CN-18-02', 'DEPENDENT', 4, 'CN', '{"zh": "章贡区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-01', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "安福县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-02', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "吉安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-03', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "吉水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-04', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "吉州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-05', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "井冈山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-06', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "青原区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-07', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "遂川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-08', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "泰和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-09', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "万安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-10', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "峡江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-11', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "新干县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-12', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "永丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-03-13', 'CN-18-03', 'DEPENDENT', 4, 'CN', '{"zh": "永新县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-04-01', 'CN-18-04', 'DEPENDENT', 4, 'CN', '{"zh": "昌江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-04-02', 'CN-18-04', 'DEPENDENT', 4, 'CN', '{"zh": "浮梁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-04-03', 'CN-18-04', 'DEPENDENT', 4, 'CN', '{"zh": "乐平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-04-04', 'CN-18-04', 'DEPENDENT', 4, 'CN', '{"zh": "珠山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-01', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "德安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-02', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "都昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-03', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "湖口县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-04', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "九江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-05', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "庐山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-06', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "彭泽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-07', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "瑞昌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-08', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "武宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-09', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "星子县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-10', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "修水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-11', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "浔阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-05-12', 'CN-18-05', 'DEPENDENT', 4, 'CN', '{"zh": "永修县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06-01', 'CN-18-06', 'DEPENDENT', 4, 'CN', '{"zh": "安义县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06-02', 'CN-18-06', 'DEPENDENT', 4, 'CN', '{"zh": "东湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06-03', 'CN-18-06', 'DEPENDENT', 4, 'CN', '{"zh": "进贤县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06-04', 'CN-18-06', 'DEPENDENT', 4, 'CN', '{"zh": "南昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06-05', 'CN-18-06', 'DEPENDENT', 4, 'CN', '{"zh": "青山湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06-06', 'CN-18-06', 'DEPENDENT', 4, 'CN', '{"zh": "青云谱区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06-07', 'CN-18-06', 'DEPENDENT', 4, 'CN', '{"zh": "湾里区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06-08', 'CN-18-06', 'DEPENDENT', 4, 'CN', '{"zh": "西湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-06-09', 'CN-18-06', 'DEPENDENT', 4, 'CN', '{"zh": "新建县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-07-01', 'CN-18-07', 'DEPENDENT', 4, 'CN', '{"zh": "安源区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-07-02', 'CN-18-07', 'DEPENDENT', 4, 'CN', '{"zh": "莲花县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-07-03', 'CN-18-07', 'DEPENDENT', 4, 'CN', '{"zh": "芦溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-07-04', 'CN-18-07', 'DEPENDENT', 4, 'CN', '{"zh": "上栗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-07-05', 'CN-18-07', 'DEPENDENT', 4, 'CN', '{"zh": "湘东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-01', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "德兴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-02', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "广丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-03', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "横峰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-04', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "鄱阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-05', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "铅山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-06', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "上饶县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-07', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "万年县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-08', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "婺源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-09', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "信州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-10', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "弋阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-11', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "余干县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-08-12', 'CN-18-08', 'DEPENDENT', 4, 'CN', '{"zh": "玉山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-09-01', 'CN-18-09', 'DEPENDENT', 4, 'CN', '{"zh": "分宜县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-09-02', 'CN-18-09', 'DEPENDENT', 4, 'CN', '{"zh": "渝水区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-01', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "丰城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-02', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "奉新县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-03', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "高安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-04', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "靖安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-05', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "上高县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-06', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "铜鼓县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-07', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "万载县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-08', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "宜丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-09', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "袁州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-10-10', 'CN-18-10', 'DEPENDENT', 4, 'CN', '{"zh": "樟树市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-11-01', 'CN-18-11', 'DEPENDENT', 4, 'CN', '{"zh": "贵溪市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-11-02', 'CN-18-11', 'DEPENDENT', 4, 'CN', '{"zh": "余江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-18-11-03', 'CN-18-11', 'DEPENDENT', 4, 'CN', '{"zh": "月湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-01-01', 'CN-19-01', 'DEPENDENT', 4, 'CN', '{"zh": "海城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-01-02', 'CN-19-01', 'DEPENDENT', 4, 'CN', '{"zh": "立山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-01-03', 'CN-19-01', 'DEPENDENT', 4, 'CN', '{"zh": "千山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-01-04', 'CN-19-01', 'DEPENDENT', 4, 'CN', '{"zh": "台安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-01-05', 'CN-19-01', 'DEPENDENT', 4, 'CN', '{"zh": "铁东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-01-06', 'CN-19-01', 'DEPENDENT', 4, 'CN', '{"zh": "铁西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-01-07', 'CN-19-01', 'DEPENDENT', 4, 'CN', '{"zh": "岫岩县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-02-01', 'CN-19-02', 'DEPENDENT', 4, 'CN', '{"zh": "本溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-02-02', 'CN-19-02', 'DEPENDENT', 4, 'CN', '{"zh": "桓仁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-02-03', 'CN-19-02', 'DEPENDENT', 4, 'CN', '{"zh": "明山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-02-04', 'CN-19-02', 'DEPENDENT', 4, 'CN', '{"zh": "南芬区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-02-05', 'CN-19-02', 'DEPENDENT', 4, 'CN', '{"zh": "平山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-02-06', 'CN-19-02', 'DEPENDENT', 4, 'CN', '{"zh": "溪湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-03-01', 'CN-19-03', 'DEPENDENT', 4, 'CN', '{"zh": "北票市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-03-02', 'CN-19-03', 'DEPENDENT', 4, 'CN', '{"zh": "朝阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-03-03', 'CN-19-03', 'DEPENDENT', 4, 'CN', '{"zh": "建平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-03-04', 'CN-19-03', 'DEPENDENT', 4, 'CN', '{"zh": "喀喇沁左翼县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-03-05', 'CN-19-03', 'DEPENDENT', 4, 'CN', '{"zh": "凌源市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-03-06', 'CN-19-03', 'DEPENDENT', 4, 'CN', '{"zh": "龙城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-03-07', 'CN-19-03', 'DEPENDENT', 4, 'CN', '{"zh": "双塔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-01', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "甘井子区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-02', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "金州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-03', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "旅顺口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-04', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "普兰店市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-05', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "沙河口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-06', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "瓦房店市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-07', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "西岗区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-08', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "长海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-09', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "中山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-04-10', 'CN-19-04', 'DEPENDENT', 4, 'CN', '{"zh": "庄河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-05-01', 'CN-19-05', 'DEPENDENT', 4, 'CN', '{"zh": "东港市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-05-02', 'CN-19-05', 'DEPENDENT', 4, 'CN', '{"zh": "凤城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-05-03', 'CN-19-05', 'DEPENDENT', 4, 'CN', '{"zh": "宽甸县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-05-04', 'CN-19-05', 'DEPENDENT', 4, 'CN', '{"zh": "元宝区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-05-05', 'CN-19-05', 'DEPENDENT', 4, 'CN', '{"zh": "振安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-05-06', 'CN-19-05', 'DEPENDENT', 4, 'CN', '{"zh": "振兴区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-06-01', 'CN-19-06', 'DEPENDENT', 4, 'CN', '{"zh": "东洲区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-06-02', 'CN-19-06', 'DEPENDENT', 4, 'CN', '{"zh": "抚顺县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-06-03', 'CN-19-06', 'DEPENDENT', 4, 'CN', '{"zh": "清原县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-06-04', 'CN-19-06', 'DEPENDENT', 4, 'CN', '{"zh": "顺城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-06-05', 'CN-19-06', 'DEPENDENT', 4, 'CN', '{"zh": "望花区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-06-06', 'CN-19-06', 'DEPENDENT', 4, 'CN', '{"zh": "新宾县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-06-07', 'CN-19-06', 'DEPENDENT', 4, 'CN', '{"zh": "新抚区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-07-01', 'CN-19-07', 'DEPENDENT', 4, 'CN', '{"zh": "阜新县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-07-02', 'CN-19-07', 'DEPENDENT', 4, 'CN', '{"zh": "海州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-07-03', 'CN-19-07', 'DEPENDENT', 4, 'CN', '{"zh": "清河门区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-07-04', 'CN-19-07', 'DEPENDENT', 4, 'CN', '{"zh": "太平区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-07-05', 'CN-19-07', 'DEPENDENT', 4, 'CN', '{"zh": "细河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-07-06', 'CN-19-07', 'DEPENDENT', 4, 'CN', '{"zh": "新邱区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-07-07', 'CN-19-07', 'DEPENDENT', 4, 'CN', '{"zh": "彰武县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-08-01', 'CN-19-08', 'DEPENDENT', 4, 'CN', '{"zh": "建昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-08-02', 'CN-19-08', 'DEPENDENT', 4, 'CN', '{"zh": "连山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-08-03', 'CN-19-08', 'DEPENDENT', 4, 'CN', '{"zh": "龙港区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-08-04', 'CN-19-08', 'DEPENDENT', 4, 'CN', '{"zh": "南票区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-08-05', 'CN-19-08', 'DEPENDENT', 4, 'CN', '{"zh": "绥中县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-08-06', 'CN-19-08', 'DEPENDENT', 4, 'CN', '{"zh": "兴城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-09-01', 'CN-19-09', 'DEPENDENT', 4, 'CN', '{"zh": "北镇市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-09-02', 'CN-19-09', 'DEPENDENT', 4, 'CN', '{"zh": "古塔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-09-03', 'CN-19-09', 'DEPENDENT', 4, 'CN', '{"zh": "黑山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-09-04', 'CN-19-09', 'DEPENDENT', 4, 'CN', '{"zh": "凌海市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-09-05', 'CN-19-09', 'DEPENDENT', 4, 'CN', '{"zh": "凌河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-09-06', 'CN-19-09', 'DEPENDENT', 4, 'CN', '{"zh": "太和区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-09-07', 'CN-19-09', 'DEPENDENT', 4, 'CN', '{"zh": "义县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-10-01', 'CN-19-10', 'DEPENDENT', 4, 'CN', '{"zh": "白塔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-10-02', 'CN-19-10', 'DEPENDENT', 4, 'CN', '{"zh": "灯塔市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-10-03', 'CN-19-10', 'DEPENDENT', 4, 'CN', '{"zh": "弓长岭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-10-04', 'CN-19-10', 'DEPENDENT', 4, 'CN', '{"zh": "宏伟区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-10-05', 'CN-19-10', 'DEPENDENT', 4, 'CN', '{"zh": "辽阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-10-06', 'CN-19-10', 'DEPENDENT', 4, 'CN', '{"zh": "太子河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-10-07', 'CN-19-10', 'DEPENDENT', 4, 'CN', '{"zh": "文圣区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-11-01', 'CN-19-11', 'DEPENDENT', 4, 'CN', '{"zh": "大洼县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-11-02', 'CN-19-11', 'DEPENDENT', 4, 'CN', '{"zh": "盘山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-11-03', 'CN-19-11', 'DEPENDENT', 4, 'CN', '{"zh": "双台子区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-11-04', 'CN-19-11', 'DEPENDENT', 4, 'CN', '{"zh": "兴隆台区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-01', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "大东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-02', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "法库县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-03', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "和平区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-04', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "皇姑区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-05', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "浑南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-06', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "康平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-07', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "辽中县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-08', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "沈北新区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-09', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "沈河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-10', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "苏家屯区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-11', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "铁西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-12', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "新民市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-12-13', 'CN-19-12', 'DEPENDENT', 4, 'CN', '{"zh": "于洪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-13-01', 'CN-19-13', 'DEPENDENT', 4, 'CN', '{"zh": "昌图县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-13-02', 'CN-19-13', 'DEPENDENT', 4, 'CN', '{"zh": "调兵山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-13-03', 'CN-19-13', 'DEPENDENT', 4, 'CN', '{"zh": "开原市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-13-04', 'CN-19-13', 'DEPENDENT', 4, 'CN', '{"zh": "清河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-13-05', 'CN-19-13', 'DEPENDENT', 4, 'CN', '{"zh": "铁岭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-13-06', 'CN-19-13', 'DEPENDENT', 4, 'CN', '{"zh": "西丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-13-07', 'CN-19-13', 'DEPENDENT', 4, 'CN', '{"zh": "银州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-14-01', 'CN-19-14', 'DEPENDENT', 4, 'CN', '{"zh": "鲅鱼圈区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-14-02', 'CN-19-14', 'DEPENDENT', 4, 'CN', '{"zh": "大石桥市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-14-03', 'CN-19-14', 'DEPENDENT', 4, 'CN', '{"zh": "盖州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-14-04', 'CN-19-14', 'DEPENDENT', 4, 'CN', '{"zh": "老边区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-14-05', 'CN-19-14', 'DEPENDENT', 4, 'CN', '{"zh": "西市区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-19-14-06', 'CN-19-14', 'DEPENDENT', 4, 'CN', '{"zh": "站前区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-01-01', 'CN-20-01', 'DEPENDENT', 4, 'CN', '{"zh": "阿拉善右旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-01-02', 'CN-20-01', 'DEPENDENT', 4, 'CN', '{"zh": "阿拉善左旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-01-03', 'CN-20-01', 'DEPENDENT', 4, 'CN', '{"zh": "额济纳旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-02-01', 'CN-20-02', 'DEPENDENT', 4, 'CN', '{"zh": "磴口县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-02-02', 'CN-20-02', 'DEPENDENT', 4, 'CN', '{"zh": "杭锦后旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-02-03', 'CN-20-02', 'DEPENDENT', 4, 'CN', '{"zh": "临河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-02-04', 'CN-20-02', 'DEPENDENT', 4, 'CN', '{"zh": "乌拉特后旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-02-05', 'CN-20-02', 'DEPENDENT', 4, 'CN', '{"zh": "乌拉特前旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-02-06', 'CN-20-02', 'DEPENDENT', 4, 'CN', '{"zh": "乌拉特中旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-02-07', 'CN-20-02', 'DEPENDENT', 4, 'CN', '{"zh": "五原县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03-01', 'CN-20-03', 'DEPENDENT', 4, 'CN', '{"zh": "白云矿区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03-02', 'CN-20-03', 'DEPENDENT', 4, 'CN', '{"zh": "达尔罕茂明安联合旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03-03', 'CN-20-03', 'DEPENDENT', 4, 'CN', '{"zh": "东河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03-04', 'CN-20-03', 'DEPENDENT', 4, 'CN', '{"zh": "固阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03-05', 'CN-20-03', 'DEPENDENT', 4, 'CN', '{"zh": "九原区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03-06', 'CN-20-03', 'DEPENDENT', 4, 'CN', '{"zh": "昆都仑区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03-07', 'CN-20-03', 'DEPENDENT', 4, 'CN', '{"zh": "青山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03-08', 'CN-20-03', 'DEPENDENT', 4, 'CN', '{"zh": "石拐区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-03-09', 'CN-20-03', 'DEPENDENT', 4, 'CN', '{"zh": "土默特右旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-01', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "阿鲁科尔沁旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-02', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "敖汉旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-03', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "巴林右旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-04', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "巴林左旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-05', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "红山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-06', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "喀喇沁旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-07', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "克什克腾旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-08', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "林西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-09', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "宁城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-10', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "松山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-11', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "翁牛特旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-04-12', 'CN-20-04', 'DEPENDENT', 4, 'CN', '{"zh": "元宝山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-05-01', 'CN-20-05', 'DEPENDENT', 4, 'CN', '{"zh": "达拉特旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-05-02', 'CN-20-05', 'DEPENDENT', 4, 'CN', '{"zh": "东胜区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-05-03', 'CN-20-05', 'DEPENDENT', 4, 'CN', '{"zh": "鄂托克旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-05-04', 'CN-20-05', 'DEPENDENT', 4, 'CN', '{"zh": "鄂托克前旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-05-05', 'CN-20-05', 'DEPENDENT', 4, 'CN', '{"zh": "杭锦旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-05-06', 'CN-20-05', 'DEPENDENT', 4, 'CN', '{"zh": "乌审旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-05-07', 'CN-20-05', 'DEPENDENT', 4, 'CN', '{"zh": "伊金霍洛旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-05-08', 'CN-20-05', 'DEPENDENT', 4, 'CN', '{"zh": "准格尔旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06-01', 'CN-20-06', 'DEPENDENT', 4, 'CN', '{"zh": "和林格尔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06-02', 'CN-20-06', 'DEPENDENT', 4, 'CN', '{"zh": "回民区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06-03', 'CN-20-06', 'DEPENDENT', 4, 'CN', '{"zh": "清水河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06-04', 'CN-20-06', 'DEPENDENT', 4, 'CN', '{"zh": "赛罕区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06-05', 'CN-20-06', 'DEPENDENT', 4, 'CN', '{"zh": "土默特左旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06-06', 'CN-20-06', 'DEPENDENT', 4, 'CN', '{"zh": "托克托县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06-07', 'CN-20-06', 'DEPENDENT', 4, 'CN', '{"zh": "武川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06-08', 'CN-20-06', 'DEPENDENT', 4, 'CN', '{"zh": "新城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-06-09', 'CN-20-06', 'DEPENDENT', 4, 'CN', '{"zh": "玉泉区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-01', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "阿荣旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-02', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "陈巴尔虎旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-03', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "额尔古纳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-04', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "鄂伦春旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-05', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "鄂温克族旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-06', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "根河市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-07', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "海拉尔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-08', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "满洲里市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-09', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "莫力达瓦旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-10', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "新巴尔虎右旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-11', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "新巴尔虎左旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-12', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "牙克石市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-07-13', 'CN-20-07', 'DEPENDENT', 4, 'CN', '{"zh": "扎兰屯市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-08-01', 'CN-20-08', 'DEPENDENT', 4, 'CN', '{"zh": "霍林郭勒市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-08-02', 'CN-20-08', 'DEPENDENT', 4, 'CN', '{"zh": "开鲁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-08-03', 'CN-20-08', 'DEPENDENT', 4, 'CN', '{"zh": "科尔沁区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-08-04', 'CN-20-08', 'DEPENDENT', 4, 'CN', '{"zh": "科尔沁左翼后旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-08-05', 'CN-20-08', 'DEPENDENT', 4, 'CN', '{"zh": "科尔沁左翼中旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-08-06', 'CN-20-08', 'DEPENDENT', 4, 'CN', '{"zh": "库伦旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-08-07', 'CN-20-08', 'DEPENDENT', 4, 'CN', '{"zh": "奈曼旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-08-08', 'CN-20-08', 'DEPENDENT', 4, 'CN', '{"zh": "扎鲁特旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-09-01', 'CN-20-09', 'DEPENDENT', 4, 'CN', '{"zh": "海勃湾区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-09-02', 'CN-20-09', 'DEPENDENT', 4, 'CN', '{"zh": "海南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-09-03', 'CN-20-09', 'DEPENDENT', 4, 'CN', '{"zh": "乌达区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-01', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "察哈尔右翼后旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-02', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "察哈尔右翼前旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-03', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "察哈尔右翼中旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-04', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "丰镇市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-05', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "化德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-06', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "集宁区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-07', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "凉城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-08', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "商都县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-09', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "四子王旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-10', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "兴和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-10-11', 'CN-20-10', 'DEPENDENT', 4, 'CN', '{"zh": "卓资县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-01', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "阿巴嘎旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-02', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "东乌珠穆沁旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-03', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "多伦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-04', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "二连浩特市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-05', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "苏尼特右旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-06', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "苏尼特左旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-07', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "太仆寺旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-08', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "西乌珠穆沁旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-09', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "锡林浩特市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-10', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "镶黄旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-11', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "正蓝旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-11-12', 'CN-20-11', 'DEPENDENT', 4, 'CN', '{"zh": "正镶白旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-12-01', 'CN-20-12', 'DEPENDENT', 4, 'CN', '{"zh": "阿尔山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-12-02', 'CN-20-12', 'DEPENDENT', 4, 'CN', '{"zh": "科尔沁右翼前旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-12-03', 'CN-20-12', 'DEPENDENT', 4, 'CN', '{"zh": "科尔沁右翼中旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-12-04', 'CN-20-12', 'DEPENDENT', 4, 'CN', '{"zh": "突泉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-12-05', 'CN-20-12', 'DEPENDENT', 4, 'CN', '{"zh": "乌兰浩特市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-20-12-06', 'CN-20-12', 'DEPENDENT', 4, 'CN', '{"zh": "扎赉特旗"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-01-01', 'CN-21-01', 'DEPENDENT', 4, 'CN', '{"zh": "泾源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-01-02', 'CN-21-01', 'DEPENDENT', 4, 'CN', '{"zh": "隆德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-01-03', 'CN-21-01', 'DEPENDENT', 4, 'CN', '{"zh": "彭阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-01-04', 'CN-21-01', 'DEPENDENT', 4, 'CN', '{"zh": "西吉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-01-05', 'CN-21-01', 'DEPENDENT', 4, 'CN', '{"zh": "原州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-02-01', 'CN-21-02', 'DEPENDENT', 4, 'CN', '{"zh": "大武口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-02-02', 'CN-21-02', 'DEPENDENT', 4, 'CN', '{"zh": "惠农区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-02-03', 'CN-21-02', 'DEPENDENT', 4, 'CN', '{"zh": "平罗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-03-01', 'CN-21-03', 'DEPENDENT', 4, 'CN', '{"zh": "利通区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-03-02', 'CN-21-03', 'DEPENDENT', 4, 'CN', '{"zh": "青铜峡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-03-03', 'CN-21-03', 'DEPENDENT', 4, 'CN', '{"zh": "同心县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-03-04', 'CN-21-03', 'DEPENDENT', 4, 'CN', '{"zh": "盐池县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-04-01', 'CN-21-04', 'DEPENDENT', 4, 'CN', '{"zh": "贺兰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-04-02', 'CN-21-04', 'DEPENDENT', 4, 'CN', '{"zh": "金凤区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-04-03', 'CN-21-04', 'DEPENDENT', 4, 'CN', '{"zh": "灵武市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-04-04', 'CN-21-04', 'DEPENDENT', 4, 'CN', '{"zh": "西夏区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-04-05', 'CN-21-04', 'DEPENDENT', 4, 'CN', '{"zh": "兴庆区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-04-06', 'CN-21-04', 'DEPENDENT', 4, 'CN', '{"zh": "永宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-05-01', 'CN-21-05', 'DEPENDENT', 4, 'CN', '{"zh": "海原县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-05-02', 'CN-21-05', 'DEPENDENT', 4, 'CN', '{"zh": "沙坡头区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-21-05-03', 'CN-21-05', 'DEPENDENT', 4, 'CN', '{"zh": "中宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-01-01', 'CN-22-01', 'DEPENDENT', 4, 'CN', '{"zh": "班玛县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-01-02', 'CN-22-01', 'DEPENDENT', 4, 'CN', '{"zh": "达日县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-01-03', 'CN-22-01', 'DEPENDENT', 4, 'CN', '{"zh": "甘德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-01-04', 'CN-22-01', 'DEPENDENT', 4, 'CN', '{"zh": "久治县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-01-05', 'CN-22-01', 'DEPENDENT', 4, 'CN', '{"zh": "玛多县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-01-06', 'CN-22-01', 'DEPENDENT', 4, 'CN', '{"zh": "玛沁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-02-01', 'CN-22-02', 'DEPENDENT', 4, 'CN', '{"zh": "刚察县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-02-02', 'CN-22-02', 'DEPENDENT', 4, 'CN', '{"zh": "海晏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-02-03', 'CN-22-02', 'DEPENDENT', 4, 'CN', '{"zh": "门源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-02-04', 'CN-22-02', 'DEPENDENT', 4, 'CN', '{"zh": "祁连县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-03-01', 'CN-22-03', 'DEPENDENT', 4, 'CN', '{"zh": "互助县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-03-02', 'CN-22-03', 'DEPENDENT', 4, 'CN', '{"zh": "化隆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-03-03', 'CN-22-03', 'DEPENDENT', 4, 'CN', '{"zh": "乐都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-03-04', 'CN-22-03', 'DEPENDENT', 4, 'CN', '{"zh": "民和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-03-05', 'CN-22-03', 'DEPENDENT', 4, 'CN', '{"zh": "平安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-03-06', 'CN-22-03', 'DEPENDENT', 4, 'CN', '{"zh": "循化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-04-01', 'CN-22-04', 'DEPENDENT', 4, 'CN', '{"zh": "共和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-04-02', 'CN-22-04', 'DEPENDENT', 4, 'CN', '{"zh": "贵德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-04-03', 'CN-22-04', 'DEPENDENT', 4, 'CN', '{"zh": "贵南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-04-04', 'CN-22-04', 'DEPENDENT', 4, 'CN', '{"zh": "同德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-04-05', 'CN-22-04', 'DEPENDENT', 4, 'CN', '{"zh": "兴海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-05-01', 'CN-22-05', 'DEPENDENT', 4, 'CN', '{"zh": "德令哈市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-05-02', 'CN-22-05', 'DEPENDENT', 4, 'CN', '{"zh": "都兰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-05-03', 'CN-22-05', 'DEPENDENT', 4, 'CN', '{"zh": "格尔木市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-05-04', 'CN-22-05', 'DEPENDENT', 4, 'CN', '{"zh": "天峻县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-05-05', 'CN-22-05', 'DEPENDENT', 4, 'CN', '{"zh": "乌兰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-06-01', 'CN-22-06', 'DEPENDENT', 4, 'CN', '{"zh": "河南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-06-02', 'CN-22-06', 'DEPENDENT', 4, 'CN', '{"zh": "尖扎县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-06-03', 'CN-22-06', 'DEPENDENT', 4, 'CN', '{"zh": "同仁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-06-04', 'CN-22-06', 'DEPENDENT', 4, 'CN', '{"zh": "泽库县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-07-01', 'CN-22-07', 'DEPENDENT', 4, 'CN', '{"zh": "城北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-07-02', 'CN-22-07', 'DEPENDENT', 4, 'CN', '{"zh": "城东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-07-03', 'CN-22-07', 'DEPENDENT', 4, 'CN', '{"zh": "城西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-07-04', 'CN-22-07', 'DEPENDENT', 4, 'CN', '{"zh": "城中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-07-05', 'CN-22-07', 'DEPENDENT', 4, 'CN', '{"zh": "大通县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-07-06', 'CN-22-07', 'DEPENDENT', 4, 'CN', '{"zh": "湟源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-07-07', 'CN-22-07', 'DEPENDENT', 4, 'CN', '{"zh": "湟中县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-08-01', 'CN-22-08', 'DEPENDENT', 4, 'CN', '{"zh": "称多县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-08-02', 'CN-22-08', 'DEPENDENT', 4, 'CN', '{"zh": "囊谦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-08-03', 'CN-22-08', 'DEPENDENT', 4, 'CN', '{"zh": "曲麻莱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-08-04', 'CN-22-08', 'DEPENDENT', 4, 'CN', '{"zh": "玉树县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-08-05', 'CN-22-08', 'DEPENDENT', 4, 'CN', '{"zh": "杂多县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-22-08-06', 'CN-22-08', 'DEPENDENT', 4, 'CN', '{"zh": "治多县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-01-01', 'CN-23-01', 'DEPENDENT', 4, 'CN', '{"zh": "滨城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-01-02', 'CN-23-01', 'DEPENDENT', 4, 'CN', '{"zh": "博兴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-01-03', 'CN-23-01', 'DEPENDENT', 4, 'CN', '{"zh": "惠民县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-01-04', 'CN-23-01', 'DEPENDENT', 4, 'CN', '{"zh": "无棣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-01-05', 'CN-23-01', 'DEPENDENT', 4, 'CN', '{"zh": "阳信县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-01-06', 'CN-23-01', 'DEPENDENT', 4, 'CN', '{"zh": "沾化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-01-07', 'CN-23-01', 'DEPENDENT', 4, 'CN', '{"zh": "邹平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-01', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "德城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-02', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "乐陵市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-03', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "临邑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-04', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-05', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "宁津县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-06', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "平原县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-07', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "齐河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-08', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "庆云县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-09', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "武城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-10', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "夏津县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-02-11', 'CN-23-02', 'DEPENDENT', 4, 'CN', '{"zh": "禹城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-03-01', 'CN-23-03', 'DEPENDENT', 4, 'CN', '{"zh": "东营区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-03-02', 'CN-23-03', 'DEPENDENT', 4, 'CN', '{"zh": "广饶县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-03-03', 'CN-23-03', 'DEPENDENT', 4, 'CN', '{"zh": "河口区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-03-04', 'CN-23-03', 'DEPENDENT', 4, 'CN', '{"zh": "垦利县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-03-05', 'CN-23-03', 'DEPENDENT', 4, 'CN', '{"zh": "利津县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04-01', 'CN-23-04', 'DEPENDENT', 4, 'CN', '{"zh": "曹县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04-02', 'CN-23-04', 'DEPENDENT', 4, 'CN', '{"zh": "成武县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04-03', 'CN-23-04', 'DEPENDENT', 4, 'CN', '{"zh": "单县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04-04', 'CN-23-04', 'DEPENDENT', 4, 'CN', '{"zh": "定陶县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04-05', 'CN-23-04', 'DEPENDENT', 4, 'CN', '{"zh": "东明县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04-06', 'CN-23-04', 'DEPENDENT', 4, 'CN', '{"zh": "巨野县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04-07', 'CN-23-04', 'DEPENDENT', 4, 'CN', '{"zh": "鄄城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04-08', 'CN-23-04', 'DEPENDENT', 4, 'CN', '{"zh": "牡丹区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-04-09', 'CN-23-04', 'DEPENDENT', 4, 'CN', '{"zh": "郓城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-01', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "槐荫区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-02', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "济阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-03', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "历城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-04', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "历下区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-05', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "平阴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-06', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "商河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-07', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "市中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-08', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "天桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-09', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "章丘市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-05-10', 'CN-23-05', 'DEPENDENT', 4, 'CN', '{"zh": "长清区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-01', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "嘉祥县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-02', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "金乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-03', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "梁山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-04', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "曲阜市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-05', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "任城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-06', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "市中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-07', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "泗水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-08', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "微山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-09', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "汶上县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-10', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "兖州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-11', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "鱼台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-06-12', 'CN-23-06', 'DEPENDENT', 4, 'CN', '{"zh": "邹城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-07-01', 'CN-23-07', 'DEPENDENT', 4, 'CN', '{"zh": "钢城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-07-02', 'CN-23-07', 'DEPENDENT', 4, 'CN', '{"zh": "莱城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-08-01', 'CN-23-08', 'DEPENDENT', 4, 'CN', '{"zh": "茌平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-08-02', 'CN-23-08', 'DEPENDENT', 4, 'CN', '{"zh": "东阿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-08-03', 'CN-23-08', 'DEPENDENT', 4, 'CN', '{"zh": "东昌府区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-08-04', 'CN-23-08', 'DEPENDENT', 4, 'CN', '{"zh": "高唐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-08-05', 'CN-23-08', 'DEPENDENT', 4, 'CN', '{"zh": "冠县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-08-06', 'CN-23-08', 'DEPENDENT', 4, 'CN', '{"zh": "临清市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-08-07', 'CN-23-08', 'DEPENDENT', 4, 'CN', '{"zh": "莘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-08-08', 'CN-23-08', 'DEPENDENT', 4, 'CN', '{"zh": "阳谷县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-01', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "苍山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-02', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "费县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-03', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "河东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-04', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "莒南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-05', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "兰山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-06', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "临沭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-07', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "罗庄区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-08', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "蒙阴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-09', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "平邑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-10', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "郯城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-11', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "沂南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-09-12', 'CN-23-09', 'DEPENDENT', 4, 'CN', '{"zh": "沂水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-01', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "城阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-02', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "黄岛区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-03', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "即墨市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-04', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "胶南市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-05', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "胶州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-06', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "莱西市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-07', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "崂山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-08', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "李沧区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-09', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "平度市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-10', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "市北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-11', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "市南区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-10-12', 'CN-23-10', 'DEPENDENT', 4, 'CN', '{"zh": "四方区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-11-01', 'CN-23-11', 'DEPENDENT', 4, 'CN', '{"zh": "东港区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-11-02', 'CN-23-11', 'DEPENDENT', 4, 'CN', '{"zh": "莒县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-11-03', 'CN-23-11', 'DEPENDENT', 4, 'CN', '{"zh": "岚山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-11-04', 'CN-23-11', 'DEPENDENT', 4, 'CN', '{"zh": "五莲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-12-01', 'CN-23-12', 'DEPENDENT', 4, 'CN', '{"zh": "岱岳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-12-02', 'CN-23-12', 'DEPENDENT', 4, 'CN', '{"zh": "东平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-12-03', 'CN-23-12', 'DEPENDENT', 4, 'CN', '{"zh": "肥城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-12-04', 'CN-23-12', 'DEPENDENT', 4, 'CN', '{"zh": "宁阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-12-05', 'CN-23-12', 'DEPENDENT', 4, 'CN', '{"zh": "泰山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-12-06', 'CN-23-12', 'DEPENDENT', 4, 'CN', '{"zh": "新泰市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-13-01', 'CN-23-13', 'DEPENDENT', 4, 'CN', '{"zh": "环翠区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-13-02', 'CN-23-13', 'DEPENDENT', 4, 'CN', '{"zh": "荣成市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-13-03', 'CN-23-13', 'DEPENDENT', 4, 'CN', '{"zh": "乳山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-13-04', 'CN-23-13', 'DEPENDENT', 4, 'CN', '{"zh": "文登区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-01', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "安丘市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-02', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "昌乐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-03', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "昌邑市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-04', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "坊子区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-05', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "高密市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-06', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "寒亭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-07', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "奎文区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-08', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "临朐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-09', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "青州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-10', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "寿光市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-11', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "潍城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-14-12', 'CN-23-14', 'DEPENDENT', 4, 'CN', '{"zh": "诸城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-01', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "福山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-02', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "海阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-03', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "莱山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-04', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "莱阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-05', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "莱州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-06', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "龙口市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-07', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "牟平区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-08', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "蓬莱市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-09', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "栖霞市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-10', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "长岛县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-11', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "招远市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-15-12', 'CN-23-15', 'DEPENDENT', 4, 'CN', '{"zh": "芝罘区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-16-01', 'CN-23-16', 'DEPENDENT', 4, 'CN', '{"zh": "山亭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-16-02', 'CN-23-16', 'DEPENDENT', 4, 'CN', '{"zh": "市中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-16-03', 'CN-23-16', 'DEPENDENT', 4, 'CN', '{"zh": "台儿庄区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-16-04', 'CN-23-16', 'DEPENDENT', 4, 'CN', '{"zh": "滕州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-16-05', 'CN-23-16', 'DEPENDENT', 4, 'CN', '{"zh": "薛城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-16-06', 'CN-23-16', 'DEPENDENT', 4, 'CN', '{"zh": "峄城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-17-01', 'CN-23-17', 'DEPENDENT', 4, 'CN', '{"zh": "博山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-17-02', 'CN-23-17', 'DEPENDENT', 4, 'CN', '{"zh": "高青县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-17-03', 'CN-23-17', 'DEPENDENT', 4, 'CN', '{"zh": "桓台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-17-04', 'CN-23-17', 'DEPENDENT', 4, 'CN', '{"zh": "临淄区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-17-05', 'CN-23-17', 'DEPENDENT', 4, 'CN', '{"zh": "沂源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-17-06', 'CN-23-17', 'DEPENDENT', 4, 'CN', '{"zh": "张店区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-17-07', 'CN-23-17', 'DEPENDENT', 4, 'CN', '{"zh": "周村区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-23-17-08', 'CN-23-17', 'DEPENDENT', 4, 'CN', '{"zh": "淄川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-01', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-02', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "大同县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-03', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "广灵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-04', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "浑源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-05', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "矿区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-06', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "灵丘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-07', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "南郊区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-08', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "天镇县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-09', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "新荣区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-10', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "阳高县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-01-11', 'CN-24-01', 'DEPENDENT', 4, 'CN', '{"zh": "左云县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-02-01', 'CN-24-02', 'DEPENDENT', 4, 'CN', '{"zh": "城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-02-02', 'CN-24-02', 'DEPENDENT', 4, 'CN', '{"zh": "高平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-02-03', 'CN-24-02', 'DEPENDENT', 4, 'CN', '{"zh": "陵川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-02-04', 'CN-24-02', 'DEPENDENT', 4, 'CN', '{"zh": "沁水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-02-05', 'CN-24-02', 'DEPENDENT', 4, 'CN', '{"zh": "阳城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-02-06', 'CN-24-02', 'DEPENDENT', 4, 'CN', '{"zh": "泽州县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-01', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "和顺县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-02', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "介休市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-03', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "灵石县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-04', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "平遥县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-05', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "祁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-06', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "寿阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-07', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "太谷县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-08', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "昔阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-09', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "榆次区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-10', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "榆社县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-03-11', 'CN-24-03', 'DEPENDENT', 4, 'CN', '{"zh": "左权县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-01', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "安泽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-02', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "大宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-03', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "汾西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-04', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "浮山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-05', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "古县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-06', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "洪洞县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-07', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "侯马市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-08', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "霍州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-09', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "吉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-10', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "蒲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-11', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "曲沃县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-12', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "隰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-13', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "乡宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-14', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "襄汾县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-15', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "尧都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-16', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "翼城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-04-17', 'CN-24-04', 'DEPENDENT', 4, 'CN', '{"zh": "永和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-01', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "方山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-02', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "汾阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-03', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "交城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-04', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "交口县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-05', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "岚县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-06', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "离石区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-07', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "临县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-08', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "柳林县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-09', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "石楼县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-10', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "文水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-11', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "孝义市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-12', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "兴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-05-13', 'CN-24-05', 'DEPENDENT', 4, 'CN', '{"zh": "中阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-06-01', 'CN-24-06', 'DEPENDENT', 4, 'CN', '{"zh": "怀仁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-06-02', 'CN-24-06', 'DEPENDENT', 4, 'CN', '{"zh": "平鲁区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-06-03', 'CN-24-06', 'DEPENDENT', 4, 'CN', '{"zh": "山阴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-06-04', 'CN-24-06', 'DEPENDENT', 4, 'CN', '{"zh": "朔城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-06-05', 'CN-24-06', 'DEPENDENT', 4, 'CN', '{"zh": "应县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-06-06', 'CN-24-06', 'DEPENDENT', 4, 'CN', '{"zh": "右玉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-01', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "古交市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-02', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "尖草坪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-03', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "晋源区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-04', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "娄烦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-05', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "清徐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-06', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "万柏林区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-07', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "小店区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-08', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "杏花岭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-09', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "阳曲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-07-10', 'CN-24-07', 'DEPENDENT', 4, 'CN', '{"zh": "迎泽区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-01', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "保德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-02', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "代县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-03', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "定襄县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-04', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "繁峙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-05', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "河曲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-06', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "静乐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-07', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "岢岚县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-08', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "宁武县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-09', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "偏关县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-10', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "神池县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-11', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "五台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-12', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "五寨县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-13', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "忻府区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-08-14', 'CN-24-08', 'DEPENDENT', 4, 'CN', '{"zh": "原平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-09-01', 'CN-24-09', 'DEPENDENT', 4, 'CN', '{"zh": "城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-09-02', 'CN-24-09', 'DEPENDENT', 4, 'CN', '{"zh": "郊区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-09-03', 'CN-24-09', 'DEPENDENT', 4, 'CN', '{"zh": "矿区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-09-04', 'CN-24-09', 'DEPENDENT', 4, 'CN', '{"zh": "平定县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-09-05', 'CN-24-09', 'DEPENDENT', 4, 'CN', '{"zh": "盂县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-01', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "河津市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-02', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "稷山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-03', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "绛县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-04', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "临猗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-05', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "平陆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-06', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "芮城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-07', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "万荣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-08', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "闻喜县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-09', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "夏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-10', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "新绛县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-11', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "盐湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-12', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "永济市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-10-13', 'CN-24-10', 'DEPENDENT', 4, 'CN', '{"zh": "垣曲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-01', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-02', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "壶关县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-03', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "郊区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-04', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "黎城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-05', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "潞城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-06', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "平顺县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-07', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "沁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-08', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "沁源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-09', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "屯留县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-10', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "武乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-11', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "襄垣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-12', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "长治县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-24-11-13', 'CN-24-11', 'DEPENDENT', 4, 'CN', '{"zh": "长子县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-01', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "白河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-02', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "汉滨区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-03', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "汉阴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-04', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "岚皋县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-05', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "宁陕县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-06', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "平利县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-07', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "石泉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-08', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "旬阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-09', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "镇坪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-01-10', 'CN-25-01', 'DEPENDENT', 4, 'CN', '{"zh": "紫阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-01', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "陈仓区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-02', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "凤县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-03', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "凤翔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-04', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "扶风县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-05', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "金台区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-06', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "麟游县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-07', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "陇县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-08', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "眉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-09', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "岐山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-10', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "千阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-11', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "太白县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-02-12', 'CN-25-02', 'DEPENDENT', 4, 'CN', '{"zh": "渭滨区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-01', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "城固县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-02', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "佛坪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-03', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "汉台区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-04', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "留坝县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-05', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "略阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-06', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "勉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-07', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "南郑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-08', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "宁强县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-09', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "西乡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-10', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "洋县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-03-11', 'CN-25-03', 'DEPENDENT', 4, 'CN', '{"zh": "镇巴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-04-01', 'CN-25-04', 'DEPENDENT', 4, 'CN', '{"zh": "丹凤县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-04-02', 'CN-25-04', 'DEPENDENT', 4, 'CN', '{"zh": "洛南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-04-03', 'CN-25-04', 'DEPENDENT', 4, 'CN', '{"zh": "山阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-04-04', 'CN-25-04', 'DEPENDENT', 4, 'CN', '{"zh": "商南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-04-05', 'CN-25-04', 'DEPENDENT', 4, 'CN', '{"zh": "商州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-04-06', 'CN-25-04', 'DEPENDENT', 4, 'CN', '{"zh": "柞水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-04-07', 'CN-25-04', 'DEPENDENT', 4, 'CN', '{"zh": "镇安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-05-01', 'CN-25-05', 'DEPENDENT', 4, 'CN', '{"zh": "王益区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-05-02', 'CN-25-05', 'DEPENDENT', 4, 'CN', '{"zh": "耀州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-05-03', 'CN-25-05', 'DEPENDENT', 4, 'CN', '{"zh": "宜君县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-05-04', 'CN-25-05', 'DEPENDENT', 4, 'CN', '{"zh": "印台区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-01', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "白水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-02', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "澄城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-03', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "大荔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-04', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "富平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-05', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "韩城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-06', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "合阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-07', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "华县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-08', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "华阴市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-09', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "临渭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-10', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "蒲城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-06-11', 'CN-25-06', 'DEPENDENT', 4, 'CN', '{"zh": "潼关县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-01', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "灞桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-02', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "碑林区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-03', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "高陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-04', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "户县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-05', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "蓝田县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-06', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "莲湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-07', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "临潼区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-08', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "未央区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-09', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "新城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-10', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "阎良区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-11', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "雁塔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-12', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "长安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-07-13', 'CN-25-07', 'DEPENDENT', 4, 'CN', '{"zh": "周至县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-01', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "彬县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-02', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "淳化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-03', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "泾阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-04', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "礼泉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-05', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "乾县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-06', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "秦都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-07', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "三原县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-08', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "渭城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-09', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "武功县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-10', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "兴平市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-11', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "旬邑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-12', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "杨凌区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-13', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "永寿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-08-14', 'CN-25-08', 'DEPENDENT', 4, 'CN', '{"zh": "长武县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-01', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "安塞县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-02', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "宝塔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-03', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "富县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-04', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "甘泉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-05', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "黄陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-06', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "黄龙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-07', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "洛川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-08', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "吴起县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-09', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "延川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-10', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "延长县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-11', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "宜川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-12', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "志丹县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-09-13', 'CN-25-09', 'DEPENDENT', 4, 'CN', '{"zh": "子长县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-01', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "定边县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-02', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "府谷县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-03', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "横山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-04', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "佳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-05', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "靖边县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-06', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "米脂县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-07', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "清涧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-08', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "神木县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-09', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "绥德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-10', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "吴堡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-11', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "榆阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-25-10-12', 'CN-25-10', 'DEPENDENT', 4, 'CN', '{"zh": "子洲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-01', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "阿坝县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-02', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "黑水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-03', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "红原县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-04', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "金川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-05', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "九寨沟县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-06', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "理县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-07', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "马尔康县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-08', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "茂县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-09', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "壤塘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-10', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "若尔盖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-11', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "松潘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-12', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "汶川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-01-13', 'CN-27-01', 'DEPENDENT', 4, 'CN', '{"zh": "小金县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-02-01', 'CN-27-02', 'DEPENDENT', 4, 'CN', '{"zh": "巴州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-02-02', 'CN-27-02', 'DEPENDENT', 4, 'CN', '{"zh": "南江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-02-03', 'CN-27-02', 'DEPENDENT', 4, 'CN', '{"zh": "平昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-02-04', 'CN-27-02', 'DEPENDENT', 4, 'CN', '{"zh": "通江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-01', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "成华区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-02', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "崇州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-03', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "大邑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-04', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "都江堰市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-05', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "金牛区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-06', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "金堂县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-07', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "锦江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-08', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "龙泉驿区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-09', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "彭州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-10', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "郫县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-11', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "蒲江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-12', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "青白江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-13', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "青羊区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-14', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "邛崃市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-15', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "双流县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-16', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "温江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-17', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "武侯区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-18', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "新都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-03-19', 'CN-27-03', 'DEPENDENT', 4, 'CN', '{"zh": "新津县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-04-01', 'CN-27-04', 'DEPENDENT', 4, 'CN', '{"zh": "达川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-04-02', 'CN-27-04', 'DEPENDENT', 4, 'CN', '{"zh": "大竹县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-04-03', 'CN-27-04', 'DEPENDENT', 4, 'CN', '{"zh": "开江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-04-04', 'CN-27-04', 'DEPENDENT', 4, 'CN', '{"zh": "渠县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-04-05', 'CN-27-04', 'DEPENDENT', 4, 'CN', '{"zh": "通川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-04-06', 'CN-27-04', 'DEPENDENT', 4, 'CN', '{"zh": "万源市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-04-07', 'CN-27-04', 'DEPENDENT', 4, 'CN', '{"zh": "宣汉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-05-01', 'CN-27-05', 'DEPENDENT', 4, 'CN', '{"zh": "广汉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-05-02', 'CN-27-05', 'DEPENDENT', 4, 'CN', '{"zh": "旌阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-05-03', 'CN-27-05', 'DEPENDENT', 4, 'CN', '{"zh": "罗江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-05-04', 'CN-27-05', 'DEPENDENT', 4, 'CN', '{"zh": "绵竹市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-05-05', 'CN-27-05', 'DEPENDENT', 4, 'CN', '{"zh": "什邡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-05-06', 'CN-27-05', 'DEPENDENT', 4, 'CN', '{"zh": "中江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-01', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "巴塘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-02', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "白玉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-03', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "丹巴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-04', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "道孚县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-05', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "稻城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-06', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "得荣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-07', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "德格县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-08', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "甘孜县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-09', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "九龙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-10', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "康定县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-11', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "理塘县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-12', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "泸定县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-13', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "炉霍县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-14', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "色达县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-15', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "石渠县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-16', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "乡城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-17', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "新龙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-06-18', 'CN-27-06', 'DEPENDENT', 4, 'CN', '{"zh": "雅江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-07-01', 'CN-27-07', 'DEPENDENT', 4, 'CN', '{"zh": "广安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-07-02', 'CN-27-07', 'DEPENDENT', 4, 'CN', '{"zh": "华蓥市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-07-03', 'CN-27-07', 'DEPENDENT', 4, 'CN', '{"zh": "邻水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-07-04', 'CN-27-07', 'DEPENDENT', 4, 'CN', '{"zh": "武胜县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-07-05', 'CN-27-07', 'DEPENDENT', 4, 'CN', '{"zh": "岳池县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-08-01', 'CN-27-08', 'DEPENDENT', 4, 'CN', '{"zh": "苍溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-08-02', 'CN-27-08', 'DEPENDENT', 4, 'CN', '{"zh": "朝天区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-08-03', 'CN-27-08', 'DEPENDENT', 4, 'CN', '{"zh": "剑阁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-08-04', 'CN-27-08', 'DEPENDENT', 4, 'CN', '{"zh": "利州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-08-05', 'CN-27-08', 'DEPENDENT', 4, 'CN', '{"zh": "青川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-08-06', 'CN-27-08', 'DEPENDENT', 4, 'CN', '{"zh": "市中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-08-07', 'CN-27-08', 'DEPENDENT', 4, 'CN', '{"zh": "旺苍县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-08-08', 'CN-27-08', 'DEPENDENT', 4, 'CN', '{"zh": "元坝区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-01', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "峨边县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-02', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "峨眉山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-03', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "夹江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-04', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "犍为县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-05', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "金口河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-06', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "井研县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-07', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "马边县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-08', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "沐川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-09', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "沙湾区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-10', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "市中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-09-11', 'CN-27-09', 'DEPENDENT', 4, 'CN', '{"zh": "五通桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-01', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "布拖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-02', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "德昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-03', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "甘洛县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-04', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "会东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-05', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "会理县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-06', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "金阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-07', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "雷波县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-08', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "美姑县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-09', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "冕宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-10', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "木里县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-11', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "宁南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-12', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "普格县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-13', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "西昌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-14', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "喜德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-15', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "盐源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-16', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "越西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-10-17', 'CN-27-10', 'DEPENDENT', 4, 'CN', '{"zh": "昭觉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-11-01', 'CN-27-11', 'DEPENDENT', 4, 'CN', '{"zh": "古蔺县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-11-02', 'CN-27-11', 'DEPENDENT', 4, 'CN', '{"zh": "合江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-11-03', 'CN-27-11', 'DEPENDENT', 4, 'CN', '{"zh": "江阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-11-04', 'CN-27-11', 'DEPENDENT', 4, 'CN', '{"zh": "龙马潭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-11-05', 'CN-27-11', 'DEPENDENT', 4, 'CN', '{"zh": "泸县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-11-06', 'CN-27-11', 'DEPENDENT', 4, 'CN', '{"zh": "纳溪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-11-07', 'CN-27-11', 'DEPENDENT', 4, 'CN', '{"zh": "叙永县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-12-01', 'CN-27-12', 'DEPENDENT', 4, 'CN', '{"zh": "丹棱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-12-02', 'CN-27-12', 'DEPENDENT', 4, 'CN', '{"zh": "东坡区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-12-03', 'CN-27-12', 'DEPENDENT', 4, 'CN', '{"zh": "洪雅县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-12-04', 'CN-27-12', 'DEPENDENT', 4, 'CN', '{"zh": "彭山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-12-05', 'CN-27-12', 'DEPENDENT', 4, 'CN', '{"zh": "青神县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-12-06', 'CN-27-12', 'DEPENDENT', 4, 'CN', '{"zh": "仁寿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13-01', 'CN-27-13', 'DEPENDENT', 4, 'CN', '{"zh": "安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13-02', 'CN-27-13', 'DEPENDENT', 4, 'CN', '{"zh": "北川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13-03', 'CN-27-13', 'DEPENDENT', 4, 'CN', '{"zh": "涪城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13-04', 'CN-27-13', 'DEPENDENT', 4, 'CN', '{"zh": "江油市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13-05', 'CN-27-13', 'DEPENDENT', 4, 'CN', '{"zh": "平武县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13-06', 'CN-27-13', 'DEPENDENT', 4, 'CN', '{"zh": "三台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13-07', 'CN-27-13', 'DEPENDENT', 4, 'CN', '{"zh": "盐亭县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13-08', 'CN-27-13', 'DEPENDENT', 4, 'CN', '{"zh": "游仙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-13-09', 'CN-27-13', 'DEPENDENT', 4, 'CN', '{"zh": "梓潼县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14-01', 'CN-27-14', 'DEPENDENT', 4, 'CN', '{"zh": "高坪区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14-02', 'CN-27-14', 'DEPENDENT', 4, 'CN', '{"zh": "嘉陵区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14-03', 'CN-27-14', 'DEPENDENT', 4, 'CN', '{"zh": "阆中市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14-04', 'CN-27-14', 'DEPENDENT', 4, 'CN', '{"zh": "南部县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14-05', 'CN-27-14', 'DEPENDENT', 4, 'CN', '{"zh": "蓬安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14-06', 'CN-27-14', 'DEPENDENT', 4, 'CN', '{"zh": "顺庆区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14-07', 'CN-27-14', 'DEPENDENT', 4, 'CN', '{"zh": "西充县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14-08', 'CN-27-14', 'DEPENDENT', 4, 'CN', '{"zh": "仪陇县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-14-09', 'CN-27-14', 'DEPENDENT', 4, 'CN', '{"zh": "营山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-15-01', 'CN-27-15', 'DEPENDENT', 4, 'CN', '{"zh": "东兴区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-15-02', 'CN-27-15', 'DEPENDENT', 4, 'CN', '{"zh": "隆昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-15-03', 'CN-27-15', 'DEPENDENT', 4, 'CN', '{"zh": "市中区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-15-04', 'CN-27-15', 'DEPENDENT', 4, 'CN', '{"zh": "威远县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-15-05', 'CN-27-15', 'DEPENDENT', 4, 'CN', '{"zh": "资中县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-16-01', 'CN-27-16', 'DEPENDENT', 4, 'CN', '{"zh": "东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-16-02', 'CN-27-16', 'DEPENDENT', 4, 'CN', '{"zh": "米易县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-16-03', 'CN-27-16', 'DEPENDENT', 4, 'CN', '{"zh": "仁和区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-16-04', 'CN-27-16', 'DEPENDENT', 4, 'CN', '{"zh": "西区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-16-05', 'CN-27-16', 'DEPENDENT', 4, 'CN', '{"zh": "盐边县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-17-01', 'CN-27-17', 'DEPENDENT', 4, 'CN', '{"zh": "安居区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-17-02', 'CN-27-17', 'DEPENDENT', 4, 'CN', '{"zh": "船山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-17-03', 'CN-27-17', 'DEPENDENT', 4, 'CN', '{"zh": "大英县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-17-04', 'CN-27-17', 'DEPENDENT', 4, 'CN', '{"zh": "蓬溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-17-05', 'CN-27-17', 'DEPENDENT', 4, 'CN', '{"zh": "射洪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-18-01', 'CN-27-18', 'DEPENDENT', 4, 'CN', '{"zh": "宝兴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-18-02', 'CN-27-18', 'DEPENDENT', 4, 'CN', '{"zh": "汉源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-18-03', 'CN-27-18', 'DEPENDENT', 4, 'CN', '{"zh": "芦山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-18-04', 'CN-27-18', 'DEPENDENT', 4, 'CN', '{"zh": "名山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-18-05', 'CN-27-18', 'DEPENDENT', 4, 'CN', '{"zh": "石棉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-18-06', 'CN-27-18', 'DEPENDENT', 4, 'CN', '{"zh": "天全县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-18-07', 'CN-27-18', 'DEPENDENT', 4, 'CN', '{"zh": "荥经县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-18-08', 'CN-27-18', 'DEPENDENT', 4, 'CN', '{"zh": "雨城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-01', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "翠屏区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-02', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "高县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-03', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "珙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-04', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "江安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-05', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "南溪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-06', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "屏山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-07', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "兴文县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-08', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "宜宾县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-09', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "筠连县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-19-10', 'CN-27-19', 'DEPENDENT', 4, 'CN', '{"zh": "长宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-20-01', 'CN-27-20', 'DEPENDENT', 4, 'CN', '{"zh": "安岳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-20-02', 'CN-27-20', 'DEPENDENT', 4, 'CN', '{"zh": "简阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-20-03', 'CN-27-20', 'DEPENDENT', 4, 'CN', '{"zh": "乐至县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-20-04', 'CN-27-20', 'DEPENDENT', 4, 'CN', '{"zh": "雁江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-21-01', 'CN-27-21', 'DEPENDENT', 4, 'CN', '{"zh": "大安区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-21-02', 'CN-27-21', 'DEPENDENT', 4, 'CN', '{"zh": "富顺县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-21-03', 'CN-27-21', 'DEPENDENT', 4, 'CN', '{"zh": "贡井区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-21-04', 'CN-27-21', 'DEPENDENT', 4, 'CN', '{"zh": "荣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-21-05', 'CN-27-21', 'DEPENDENT', 4, 'CN', '{"zh": "沿滩区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-27-21-06', 'CN-27-21', 'DEPENDENT', 4, 'CN', '{"zh": "自流井区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-01', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "阿蓮區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-02', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "大寮區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-03', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "大社區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-04', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "大樹區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-05', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "東沙島"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-06', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "鳳山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-07', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "岡山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-08', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "鼓山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-09', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "湖內區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-10', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "茄萣區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-11', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "甲仙區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-12', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "林園區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-13', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "苓雅區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-14', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "六龜區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-15', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "路竹區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-16', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "茂林區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-17', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "美濃區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-18', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "彌陀區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-19', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "那瑪夏區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-20', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "南沙島"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-21', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "楠梓區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-22', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "內門區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-23', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "鳥松區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-24', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "旗津區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-25', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "旗山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-26', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "前金區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-27', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "前鎮區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-28', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "橋頭區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-29', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "仁武區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-30', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "三民區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-31', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "杉林區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-32', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "桃源區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-33', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "田寮區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-34', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "小港區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-35', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "新興區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-36', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "鹽埕區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-37', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "燕巢區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-38', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "永安區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-39', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "梓官區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-01-40', 'CN-28-01', 'DEPENDENT', 4, 'CN', '{"zh": "左營區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-01', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "豐濱鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-02', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "鳳林鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-03', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "富里鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-04', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "光復鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-05', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "花蓮市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-06', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "吉安鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-07', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "瑞穗鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-08', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "壽豐鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-09', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "萬榮鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-10', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "新城鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-11', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "秀林鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-12', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "玉里鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-02-13', 'CN-28-02', 'DEPENDENT', 4, 'CN', '{"zh": "卓溪鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-03-01', 'CN-28-03', 'DEPENDENT', 4, 'CN', '{"zh": "安樂區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-03-02', 'CN-28-03', 'DEPENDENT', 4, 'CN', '{"zh": "暖暖區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-03-03', 'CN-28-03', 'DEPENDENT', 4, 'CN', '{"zh": "七堵區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-03-04', 'CN-28-03', 'DEPENDENT', 4, 'CN', '{"zh": "仁愛區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-03-05', 'CN-28-03', 'DEPENDENT', 4, 'CN', '{"zh": "信義區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-03-06', 'CN-28-03', 'DEPENDENT', 4, 'CN', '{"zh": "中山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-03-07', 'CN-28-03', 'DEPENDENT', 4, 'CN', '{"zh": "中正區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-04-01', 'CN-28-04', 'DEPENDENT', 4, 'CN', '{"zh": "東區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-04-02', 'CN-28-04', 'DEPENDENT', 4, 'CN', '{"zh": "西區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-01', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "阿里山鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-02', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "布袋鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-03', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "大埔鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-04', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "大林鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-05', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "東石鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-06', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "番路鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-07', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "六腳鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-08', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "鹿草鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-09', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "梅山鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-10', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "民雄鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-11', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "朴子市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-12', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "水上鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-13', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "太保市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-14', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "溪口鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-15', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "新港鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-16', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "義竹鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-17', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "中埔鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-05-18', 'CN-28-05', 'DEPENDENT', 4, 'CN', '{"zh": "竹崎鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-06-01', 'CN-28-06', 'DEPENDENT', 4, 'CN', '{"zh": "金城鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-06-02', 'CN-28-06', 'DEPENDENT', 4, 'CN', '{"zh": "金湖鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-06-03', 'CN-28-06', 'DEPENDENT', 4, 'CN', '{"zh": "金寧鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-06-04', 'CN-28-06', 'DEPENDENT', 4, 'CN', '{"zh": "金沙鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-06-05', 'CN-28-06', 'DEPENDENT', 4, 'CN', '{"zh": "烈嶼鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-06-06', 'CN-28-06', 'DEPENDENT', 4, 'CN', '{"zh": "烏坵鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-07-01', 'CN-28-07', 'DEPENDENT', 4, 'CN', '{"zh": "北竿鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-07-02', 'CN-28-07', 'DEPENDENT', 4, 'CN', '{"zh": "東引鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-07-03', 'CN-28-07', 'DEPENDENT', 4, 'CN', '{"zh": "莒光鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-07-04', 'CN-28-07', 'DEPENDENT', 4, 'CN', '{"zh": "南竿鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-01', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "大湖鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-02', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "公館鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-03', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "後龍鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-04', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "苗栗市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-05', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "南庄鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-06', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "三灣鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-07', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "三義鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-08', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "獅潭鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-09', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "泰安鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-10', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "通霄鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-11', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "銅鑼鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-12', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "頭份市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-13', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "頭屋鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-14', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "西湖鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-15', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "苑裡鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-16', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "造橋鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-17', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "竹南鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-08-18', 'CN-28-08', 'DEPENDENT', 4, 'CN', '{"zh": "卓蘭鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-01', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "埔里鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-02', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "草屯鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-03', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "國姓鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-04', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "集集鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-05', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "鹿谷鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-06', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "名間鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-07', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "南投市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-08', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "仁愛鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-09', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "水里鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-10', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "信義鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-11', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "魚池鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-12', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "中寮鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-09-13', 'CN-28-09', 'DEPENDENT', 4, 'CN', '{"zh": "竹山鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-10-01', 'CN-28-10', 'DEPENDENT', 4, 'CN', '{"zh": "白沙鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-10-02', 'CN-28-10', 'DEPENDENT', 4, 'CN', '{"zh": "湖西鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-10-03', 'CN-28-10', 'DEPENDENT', 4, 'CN', '{"zh": "馬公市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-10-04', 'CN-28-10', 'DEPENDENT', 4, 'CN', '{"zh": "七美鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-10-05', 'CN-28-10', 'DEPENDENT', 4, 'CN', '{"zh": "望安鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-10-06', 'CN-28-10', 'DEPENDENT', 4, 'CN', '{"zh": "西嶼鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-01', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "潮州鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-02', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "車城鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-03', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "春日鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-04', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "東港鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-05', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "枋寮鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-06', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "枋山鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-07', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "高樹鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-08', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "恆春鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-09', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "佳冬鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-10', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "九如鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-11', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "崁頂鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-12', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "來義鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-13', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "里港鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-14', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "林邊鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-15', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "麟洛鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-16', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "琉球鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-17', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "瑪家鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-18', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "滿洲鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-19', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "牡丹鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-20', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "南州鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-21', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "內埔鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-22', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "屏東市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-23', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "三地門鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-24', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "獅子鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-25', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "泰武鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-26', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "萬丹鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-27', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "萬巒鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-28', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "霧台鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-29', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "新埤鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-30', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "新園鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-31', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "鹽埔鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-32', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "長治鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-11-33', 'CN-28-11', 'DEPENDENT', 4, 'CN', '{"zh": "竹田鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-01', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "北投區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-02', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "大安區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-03', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "大同區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-04', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "南港區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-05', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "內湖區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-06', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "士林區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-07', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "松山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-08', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "萬華區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-09', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "文山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-10', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "信義區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-11', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "中山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-12-12', 'CN-28-12', 'DEPENDENT', 4, 'CN', '{"zh": "中正區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-01', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "卑南鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-02', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "成功鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-03', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "池上鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-04', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "達仁鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-05', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "大武鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-06', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "東河鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-07', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "關山鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-08', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "海端鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-09', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "金峰鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-10', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "蘭嶼鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-11', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "鹿野鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-12', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "綠島鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-13', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "台東市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-14', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "太麻里鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-15', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "延平鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-13-16', 'CN-28-13', 'DEPENDENT', 4, 'CN', '{"zh": "長濱鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-01', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "安定區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-02', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "安南區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-03', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "安平區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-04', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "白河區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-05', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "北門區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-06', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "北區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-07', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "大內區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-08', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "東區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-09', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "東山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-10', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "官田區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-11', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "關廟區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-12', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "歸仁區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-13', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "後壁區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-14', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "佳里區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-15', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "將軍區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-16', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "柳營區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-17', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "六甲區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-18', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "龍崎區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-19', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "麻豆區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-20', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "南化區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-21', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "南區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-22', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "楠西區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-23', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "七股區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-24', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "仁德區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-25', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "山上區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-26', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "善化區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-27', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "西港區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-28', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "下營區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-29', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "新化區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-30', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "新市區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-31', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "新營區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-32', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "學甲區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-33', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "鹽水區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-34', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "永康區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-35', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "玉井區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-36', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "中西區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-14-37', 'CN-28-14', 'DEPENDENT', 4, 'CN', '{"zh": "左鎮區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-01', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "北區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-02', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "北屯區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-03', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "大安區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-04', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "大肚區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-05', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "大甲區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-06', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "大里區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-07', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "大雅區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-08', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "東區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-09', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "東勢區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-10', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "豐原區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-11', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "和平區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-12', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "后里區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-13', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "龍井區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-14', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "南區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-15', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "南屯區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-16', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "清水區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-17', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "沙鹿區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-18', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "神岡區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-19', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "石岡區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-20', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "太平區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-21', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "潭子區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-22', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "外埔區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-23', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "烏日區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-24', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "梧棲區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-25', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "霧峰區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-26', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "西區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-27', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "西屯區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-28', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "新社區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-15-29', 'CN-28-15', 'DEPENDENT', 4, 'CN', '{"zh": "中區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-01', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "八德區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-02', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "大溪區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-03', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "大園區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-04', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "復興區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-05', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "觀音區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-06', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "龜山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-07', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "龍潭區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-08', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "蘆竹區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-09', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "平鎮區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-10', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "桃園區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-11', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "新屋區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-12', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "楊梅區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-16-13', 'CN-28-16', 'DEPENDENT', 4, 'CN', '{"zh": "中壢區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-01', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "八里區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-02', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "板橋區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-03', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "淡水區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-04', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "貢寮區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-05', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "金山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-06', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "林口區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-07', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "蘆洲區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-08', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "平溪區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-09', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "坪林區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-10', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "瑞芳區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-11', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "三峽區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-12', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "三芝區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-13', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "三重區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-14', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "深坑區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-15', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "石碇區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-16', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "石門區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-17', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "樹林區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-18', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "雙溪區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-19', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "泰山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-20', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "土城區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-21', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "萬里區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-22', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "烏來區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-23', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "五股區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-24', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "汐止區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-25', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "新店區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-26', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "新莊區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-27', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "鶯歌區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-28', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "永和區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-17-29', 'CN-28-17', 'DEPENDENT', 4, 'CN', '{"zh": "中和區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-18-01', 'CN-28-18', 'DEPENDENT', 4, 'CN', '{"zh": "北區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-18-02', 'CN-28-18', 'DEPENDENT', 4, 'CN', '{"zh": "東區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-18-03', 'CN-28-18', 'DEPENDENT', 4, 'CN', '{"zh": "香山區"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-01', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "寶山鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-02', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "北埔鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-03', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "峨眉鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-04', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "關西鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-05', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "橫山鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-06', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "湖口鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-07', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "尖石鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-08', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "五峰鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-09', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "新埔鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-10', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "新豐鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-11', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "芎林鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-12', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "竹北市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-19-13', 'CN-28-19', 'DEPENDENT', 4, 'CN', '{"zh": "竹東鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-01', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "大同鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-02', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "冬山鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-03', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "礁溪鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-04', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "羅東鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-05', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "南澳鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-06', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "三星鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-07', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "蘇澳鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-08', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "頭城鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-09', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "五結鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-10', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "宜蘭市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-11', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "員山鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-20-12', 'CN-28-20', 'DEPENDENT', 4, 'CN', '{"zh": "壯圍鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-01', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "褒忠鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-02', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "北港鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-03', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "莿桐鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-04', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "大埤鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-05', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "東勢鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-06', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "斗六市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-07', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "斗南鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-08', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "二崙鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-09', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "古坑鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-10', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "虎尾鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-11', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "口湖鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-12', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "林內鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-13', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "崙背鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-14', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "麥寮鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-15', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "水林鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-16', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "四湖鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-17', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "台西鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-18', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "土庫鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-19', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "西螺鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-21-20', 'CN-28-21', 'DEPENDENT', 4, 'CN', '{"zh": "元長鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-01', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "北斗鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-02', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "埔心鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-03', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "埔鹽鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-04', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "大城鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-05', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "大村鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-06', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "二林鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-07', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "二水鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-08', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "芳苑鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-09', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "芬園鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-10', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "福興鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-11', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "和美鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-12', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "花壇鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-13', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "鹿港鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-14', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "埤頭鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-15', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "社頭鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-16', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "伸港鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-17', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "田尾鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-18', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "田中鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-19', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "溪湖鎮"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-20', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "溪州鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-21', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "線西鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-22', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "秀水鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-23', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "永靖鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-24', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "員林市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-25', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "彰化市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-28-22-26', 'CN-28-22', 'DEPENDENT', 4, 'CN', '{"zh": "竹塘鄉"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-01-01', 'CN-30-01', 'DEPENDENT', 4, 'CN', '{"zh": "措勤县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-01-02', 'CN-30-01', 'DEPENDENT', 4, 'CN', '{"zh": "噶尔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-01-03', 'CN-30-01', 'DEPENDENT', 4, 'CN', '{"zh": "改则县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-01-04', 'CN-30-01', 'DEPENDENT', 4, 'CN', '{"zh": "革吉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-01-05', 'CN-30-01', 'DEPENDENT', 4, 'CN', '{"zh": "普兰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-01-06', 'CN-30-01', 'DEPENDENT', 4, 'CN', '{"zh": "日土县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-01-07', 'CN-30-01', 'DEPENDENT', 4, 'CN', '{"zh": "札达县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-01', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "八宿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-02', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "边坝县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-03', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "察雅县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-04', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "昌都县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-05', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "丁青县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-06', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "贡觉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-07', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "江达县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-08', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "类乌齐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-09', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "洛隆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-10', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "芒康县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-02-11', 'CN-30-02', 'DEPENDENT', 4, 'CN', '{"zh": "左贡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-03-01', 'CN-30-03', 'DEPENDENT', 4, 'CN', '{"zh": "城关区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-03-02', 'CN-30-03', 'DEPENDENT', 4, 'CN', '{"zh": "达孜县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-03-03', 'CN-30-03', 'DEPENDENT', 4, 'CN', '{"zh": "当雄县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-03-04', 'CN-30-03', 'DEPENDENT', 4, 'CN', '{"zh": "堆龙德庆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-03-05', 'CN-30-03', 'DEPENDENT', 4, 'CN', '{"zh": "林周县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-03-06', 'CN-30-03', 'DEPENDENT', 4, 'CN', '{"zh": "墨竹工卡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-03-07', 'CN-30-03', 'DEPENDENT', 4, 'CN', '{"zh": "尼木县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-03-08', 'CN-30-03', 'DEPENDENT', 4, 'CN', '{"zh": "曲水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-04-01', 'CN-30-04', 'DEPENDENT', 4, 'CN', '{"zh": "波密县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-04-02', 'CN-30-04', 'DEPENDENT', 4, 'CN', '{"zh": "察隅县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-04-03', 'CN-30-04', 'DEPENDENT', 4, 'CN', '{"zh": "工布江达县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-04-04', 'CN-30-04', 'DEPENDENT', 4, 'CN', '{"zh": "朗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-04-05', 'CN-30-04', 'DEPENDENT', 4, 'CN', '{"zh": "林芝县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-04-06', 'CN-30-04', 'DEPENDENT', 4, 'CN', '{"zh": "米林县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-04-07', 'CN-30-04', 'DEPENDENT', 4, 'CN', '{"zh": "墨脱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-01', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "安多县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-02', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "巴青县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-03', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "班戈县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-04', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "比如县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-05', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "嘉黎县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-06', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "那曲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-07', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "尼玛县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-08', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "聂荣县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-09', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "申扎县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-05-10', 'CN-30-05', 'DEPENDENT', 4, 'CN', '{"zh": "索县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-01', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "昂仁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-02', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "白朗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-03', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "定结县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-04', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "定日县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-05', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "岗巴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-06', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "吉隆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-07', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "江孜县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-08', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "康马县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-09', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "拉孜县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-10', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "南木林县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-11', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "聂拉木县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-12', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "仁布县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-13', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "日喀则市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-14', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "萨嘎县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-15', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "萨迦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-16', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "谢通门县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-17', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "亚东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-06-18', 'CN-30-06', 'DEPENDENT', 4, 'CN', '{"zh": "仲巴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-01', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "措美县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-02', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "错那县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-03', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "贡嘎县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-04', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "加查县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-05', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "浪卡子县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-06', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "隆子县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-07', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "洛扎县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-08', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "乃东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-09', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "琼结县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-10', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "曲松县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-11', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "桑日县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-30-07-12', 'CN-30-07', 'DEPENDENT', 4, 'CN', '{"zh": "扎囊县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01-01', 'CN-32-01', 'DEPENDENT', 4, 'CN', '{"zh": "阿克苏市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01-02', 'CN-32-01', 'DEPENDENT', 4, 'CN', '{"zh": "阿瓦提县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01-03', 'CN-32-01', 'DEPENDENT', 4, 'CN', '{"zh": "拜城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01-04', 'CN-32-01', 'DEPENDENT', 4, 'CN', '{"zh": "柯坪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01-05', 'CN-32-01', 'DEPENDENT', 4, 'CN', '{"zh": "库车县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01-06', 'CN-32-01', 'DEPENDENT', 4, 'CN', '{"zh": "沙雅县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01-07', 'CN-32-01', 'DEPENDENT', 4, 'CN', '{"zh": "温宿县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01-08', 'CN-32-01', 'DEPENDENT', 4, 'CN', '{"zh": "乌什县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-01-09', 'CN-32-01', 'DEPENDENT', 4, 'CN', '{"zh": "新和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-03-01', 'CN-32-03', 'DEPENDENT', 4, 'CN', '{"zh": "阿勒泰市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-03-02', 'CN-32-03', 'DEPENDENT', 4, 'CN', '{"zh": "布尔津县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-03-03', 'CN-32-03', 'DEPENDENT', 4, 'CN', '{"zh": "福海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-03-04', 'CN-32-03', 'DEPENDENT', 4, 'CN', '{"zh": "富蕴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-03-05', 'CN-32-03', 'DEPENDENT', 4, 'CN', '{"zh": "哈巴河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-03-06', 'CN-32-03', 'DEPENDENT', 4, 'CN', '{"zh": "吉木乃县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-03-07', 'CN-32-03', 'DEPENDENT', 4, 'CN', '{"zh": "青河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04-01', 'CN-32-04', 'DEPENDENT', 4, 'CN', '{"zh": "博湖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04-02', 'CN-32-04', 'DEPENDENT', 4, 'CN', '{"zh": "和静县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04-03', 'CN-32-04', 'DEPENDENT', 4, 'CN', '{"zh": "和硕县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04-04', 'CN-32-04', 'DEPENDENT', 4, 'CN', '{"zh": "库尔勒市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04-05', 'CN-32-04', 'DEPENDENT', 4, 'CN', '{"zh": "轮台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04-06', 'CN-32-04', 'DEPENDENT', 4, 'CN', '{"zh": "且末县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04-07', 'CN-32-04', 'DEPENDENT', 4, 'CN', '{"zh": "若羌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04-08', 'CN-32-04', 'DEPENDENT', 4, 'CN', '{"zh": "尉犁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-04-09', 'CN-32-04', 'DEPENDENT', 4, 'CN', '{"zh": "焉耆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-05-01', 'CN-32-05', 'DEPENDENT', 4, 'CN', '{"zh": "博乐市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-05-02', 'CN-32-05', 'DEPENDENT', 4, 'CN', '{"zh": "精河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-05-03', 'CN-32-05', 'DEPENDENT', 4, 'CN', '{"zh": "温泉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-06-01', 'CN-32-06', 'DEPENDENT', 4, 'CN', '{"zh": "昌吉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-06-02', 'CN-32-06', 'DEPENDENT', 4, 'CN', '{"zh": "阜康市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-06-03', 'CN-32-06', 'DEPENDENT', 4, 'CN', '{"zh": "呼图壁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-06-04', 'CN-32-06', 'DEPENDENT', 4, 'CN', '{"zh": "吉木萨尔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-06-05', 'CN-32-06', 'DEPENDENT', 4, 'CN', '{"zh": "玛纳斯县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-06-06', 'CN-32-06', 'DEPENDENT', 4, 'CN', '{"zh": "木垒县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-06-07', 'CN-32-06', 'DEPENDENT', 4, 'CN', '{"zh": "奇台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-07-01', 'CN-32-07', 'DEPENDENT', 4, 'CN', '{"zh": "巴里坤县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-07-02', 'CN-32-07', 'DEPENDENT', 4, 'CN', '{"zh": "哈密市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-07-03', 'CN-32-07', 'DEPENDENT', 4, 'CN', '{"zh": "伊吾县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-08-01', 'CN-32-08', 'DEPENDENT', 4, 'CN', '{"zh": "策勒县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-08-02', 'CN-32-08', 'DEPENDENT', 4, 'CN', '{"zh": "和田市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-08-03', 'CN-32-08', 'DEPENDENT', 4, 'CN', '{"zh": "和田县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-08-04', 'CN-32-08', 'DEPENDENT', 4, 'CN', '{"zh": "洛浦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-08-05', 'CN-32-08', 'DEPENDENT', 4, 'CN', '{"zh": "民丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-08-06', 'CN-32-08', 'DEPENDENT', 4, 'CN', '{"zh": "墨玉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-08-07', 'CN-32-08', 'DEPENDENT', 4, 'CN', '{"zh": "皮山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-08-08', 'CN-32-08', 'DEPENDENT', 4, 'CN', '{"zh": "于田县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-01', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "巴楚县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-02', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "伽师县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-03', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "喀什市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-04', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "麦盖提县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-05', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "莎车县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-06', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "疏附县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-07', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "疏勒县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-08', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "塔什库尔干县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-09', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "叶城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-10', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "英吉沙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-11', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "岳普湖县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-09-12', 'CN-32-09', 'DEPENDENT', 4, 'CN', '{"zh": "泽普县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-10-01', 'CN-32-10', 'DEPENDENT', 4, 'CN', '{"zh": "白碱滩区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-10-02', 'CN-32-10', 'DEPENDENT', 4, 'CN', '{"zh": "独山子区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-10-03', 'CN-32-10', 'DEPENDENT', 4, 'CN', '{"zh": "克拉玛依区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-10-04', 'CN-32-10', 'DEPENDENT', 4, 'CN', '{"zh": "乌尔禾区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-11-01', 'CN-32-11', 'DEPENDENT', 4, 'CN', '{"zh": "阿合奇县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-11-02', 'CN-32-11', 'DEPENDENT', 4, 'CN', '{"zh": "阿克陶县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-11-03', 'CN-32-11', 'DEPENDENT', 4, 'CN', '{"zh": "阿图什市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-11-04', 'CN-32-11', 'DEPENDENT', 4, 'CN', '{"zh": "乌恰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-13-01', 'CN-32-13', 'DEPENDENT', 4, 'CN', '{"zh": "额敏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-13-02', 'CN-32-13', 'DEPENDENT', 4, 'CN', '{"zh": "和布克赛尔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-13-03', 'CN-32-13', 'DEPENDENT', 4, 'CN', '{"zh": "沙湾县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-13-04', 'CN-32-13', 'DEPENDENT', 4, 'CN', '{"zh": "塔城市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-13-05', 'CN-32-13', 'DEPENDENT', 4, 'CN', '{"zh": "托里县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-13-06', 'CN-32-13', 'DEPENDENT', 4, 'CN', '{"zh": "乌苏市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-13-07', 'CN-32-13', 'DEPENDENT', 4, 'CN', '{"zh": "裕民县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-15-01', 'CN-32-15', 'DEPENDENT', 4, 'CN', '{"zh": "鄯善县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-15-02', 'CN-32-15', 'DEPENDENT', 4, 'CN', '{"zh": "吐鲁番市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-15-03', 'CN-32-15', 'DEPENDENT', 4, 'CN', '{"zh": "托克逊县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-16-01', 'CN-32-16', 'DEPENDENT', 4, 'CN', '{"zh": "达坂城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-16-02', 'CN-32-16', 'DEPENDENT', 4, 'CN', '{"zh": "米东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-16-03', 'CN-32-16', 'DEPENDENT', 4, 'CN', '{"zh": "沙依巴克区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-16-04', 'CN-32-16', 'DEPENDENT', 4, 'CN', '{"zh": "水磨沟区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-16-05', 'CN-32-16', 'DEPENDENT', 4, 'CN', '{"zh": "天山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-16-06', 'CN-32-16', 'DEPENDENT', 4, 'CN', '{"zh": "头屯河区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-16-07', 'CN-32-16', 'DEPENDENT', 4, 'CN', '{"zh": "乌鲁木齐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-16-08', 'CN-32-16', 'DEPENDENT', 4, 'CN', '{"zh": "新市区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-01', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "察布查尔县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-02', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "巩留县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-03', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "霍城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-04', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "奎屯市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-05', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "尼勒克县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-06', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "特克斯县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-07', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "新源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-08', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "伊宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-09', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "伊宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-32-18-10', 'CN-32-18', 'DEPENDENT', 4, 'CN', '{"zh": "昭苏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-01-01', 'CN-33-01', 'DEPENDENT', 4, 'CN', '{"zh": "昌宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-01-02', 'CN-33-01', 'DEPENDENT', 4, 'CN', '{"zh": "龙陵县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-01-03', 'CN-33-01', 'DEPENDENT', 4, 'CN', '{"zh": "隆阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-01-04', 'CN-33-01', 'DEPENDENT', 4, 'CN', '{"zh": "施甸县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-01-05', 'CN-33-01', 'DEPENDENT', 4, 'CN', '{"zh": "腾冲县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-01', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "楚雄市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-02', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "大姚县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-03', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "禄丰县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-04', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "牟定县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-05', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "南华县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-06', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "双柏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-07', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "武定县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-08', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "姚安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-09', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "永仁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-02-10', 'CN-33-02', 'DEPENDENT', 4, 'CN', '{"zh": "元谋县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-01', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "宾川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-02', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "大理市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-03', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "洱源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-04', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "鹤庆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-05', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "剑川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-06', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "弥渡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-07', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "南涧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-08', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "巍山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-09', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "祥云县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-10', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "漾濞县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-11', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "永平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-03-12', 'CN-33-03', 'DEPENDENT', 4, 'CN', '{"zh": "云龙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-04-01', 'CN-33-04', 'DEPENDENT', 4, 'CN', '{"zh": "梁河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-04-02', 'CN-33-04', 'DEPENDENT', 4, 'CN', '{"zh": "陇川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-04-03', 'CN-33-04', 'DEPENDENT', 4, 'CN', '{"zh": "潞西市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-04-04', 'CN-33-04', 'DEPENDENT', 4, 'CN', '{"zh": "瑞丽市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-04-05', 'CN-33-04', 'DEPENDENT', 4, 'CN', '{"zh": "盈江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-05-01', 'CN-33-05', 'DEPENDENT', 4, 'CN', '{"zh": "德钦县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-05-02', 'CN-33-05', 'DEPENDENT', 4, 'CN', '{"zh": "维西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-05-03', 'CN-33-05', 'DEPENDENT', 4, 'CN', '{"zh": "香格里拉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-01', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "个旧市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-02', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "河口县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-03', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "红河县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-04', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "建水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-05', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "金平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-06', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "开远市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-07', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "泸西县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-08', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "绿春县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-09', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "蒙自市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-10', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "弥勒县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-11', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "屏边县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-12', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "石屏县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-06-13', 'CN-33-06', 'DEPENDENT', 4, 'CN', '{"zh": "元阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-01', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "安宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-02', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "呈贡区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-03', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "东川区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-04', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "富民县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-05', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "官渡区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-06', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "晋宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-07', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "禄劝县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-08', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "盘龙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-09', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "石林县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-10', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "嵩明县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-11', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "五华区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-12', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "西山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-13', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "寻甸县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-07-14', 'CN-33-07', 'DEPENDENT', 4, 'CN', '{"zh": "宜良县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-08-01', 'CN-33-08', 'DEPENDENT', 4, 'CN', '{"zh": "古城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-08-02', 'CN-33-08', 'DEPENDENT', 4, 'CN', '{"zh": "华坪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-08-03', 'CN-33-08', 'DEPENDENT', 4, 'CN', '{"zh": "宁蒗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-08-04', 'CN-33-08', 'DEPENDENT', 4, 'CN', '{"zh": "永胜县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-08-05', 'CN-33-08', 'DEPENDENT', 4, 'CN', '{"zh": "玉龙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-09-01', 'CN-33-09', 'DEPENDENT', 4, 'CN', '{"zh": "沧源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-09-02', 'CN-33-09', 'DEPENDENT', 4, 'CN', '{"zh": "凤庆县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-09-03', 'CN-33-09', 'DEPENDENT', 4, 'CN', '{"zh": "耿马县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-09-04', 'CN-33-09', 'DEPENDENT', 4, 'CN', '{"zh": "临翔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-09-05', 'CN-33-09', 'DEPENDENT', 4, 'CN', '{"zh": "双江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-09-06', 'CN-33-09', 'DEPENDENT', 4, 'CN', '{"zh": "永德县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-09-07', 'CN-33-09', 'DEPENDENT', 4, 'CN', '{"zh": "云县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-09-08', 'CN-33-09', 'DEPENDENT', 4, 'CN', '{"zh": "镇康县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-10-01', 'CN-33-10', 'DEPENDENT', 4, 'CN', '{"zh": "福贡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-10-02', 'CN-33-10', 'DEPENDENT', 4, 'CN', '{"zh": "贡山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-10-03', 'CN-33-10', 'DEPENDENT', 4, 'CN', '{"zh": "兰坪县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-10-04', 'CN-33-10', 'DEPENDENT', 4, 'CN', '{"zh": "泸水县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-01', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "江城县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-02', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "景东县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-03', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "景谷县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-04', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "澜沧县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-05', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "孟连县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-06', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "墨江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-07', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "宁洱县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-08', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "思茅区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-09', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "西盟县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-11-10', 'CN-33-11', 'DEPENDENT', 4, 'CN', '{"zh": "镇沅县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12-01', 'CN-33-12', 'DEPENDENT', 4, 'CN', '{"zh": "富源县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12-02', 'CN-33-12', 'DEPENDENT', 4, 'CN', '{"zh": "会泽县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12-03', 'CN-33-12', 'DEPENDENT', 4, 'CN', '{"zh": "陆良县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12-04', 'CN-33-12', 'DEPENDENT', 4, 'CN', '{"zh": "罗平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12-05', 'CN-33-12', 'DEPENDENT', 4, 'CN', '{"zh": "马龙县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12-06', 'CN-33-12', 'DEPENDENT', 4, 'CN', '{"zh": "麒麟区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12-07', 'CN-33-12', 'DEPENDENT', 4, 'CN', '{"zh": "师宗县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12-08', 'CN-33-12', 'DEPENDENT', 4, 'CN', '{"zh": "宣威市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-12-09', 'CN-33-12', 'DEPENDENT', 4, 'CN', '{"zh": "沾益县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-13-01', 'CN-33-13', 'DEPENDENT', 4, 'CN', '{"zh": "富宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-13-02', 'CN-33-13', 'DEPENDENT', 4, 'CN', '{"zh": "广南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-13-03', 'CN-33-13', 'DEPENDENT', 4, 'CN', '{"zh": "麻栗坡县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-13-04', 'CN-33-13', 'DEPENDENT', 4, 'CN', '{"zh": "马关县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-13-05', 'CN-33-13', 'DEPENDENT', 4, 'CN', '{"zh": "丘北县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-13-06', 'CN-33-13', 'DEPENDENT', 4, 'CN', '{"zh": "文山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-13-07', 'CN-33-13', 'DEPENDENT', 4, 'CN', '{"zh": "西畴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-13-08', 'CN-33-13', 'DEPENDENT', 4, 'CN', '{"zh": "砚山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-14-01', 'CN-33-14', 'DEPENDENT', 4, 'CN', '{"zh": "景洪市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-14-02', 'CN-33-14', 'DEPENDENT', 4, 'CN', '{"zh": "勐海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-14-03', 'CN-33-14', 'DEPENDENT', 4, 'CN', '{"zh": "勐腊县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15-01', 'CN-33-15', 'DEPENDENT', 4, 'CN', '{"zh": "澄江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15-02', 'CN-33-15', 'DEPENDENT', 4, 'CN', '{"zh": "峨山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15-03', 'CN-33-15', 'DEPENDENT', 4, 'CN', '{"zh": "红塔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15-04', 'CN-33-15', 'DEPENDENT', 4, 'CN', '{"zh": "华宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15-05', 'CN-33-15', 'DEPENDENT', 4, 'CN', '{"zh": "江川县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15-06', 'CN-33-15', 'DEPENDENT', 4, 'CN', '{"zh": "通海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15-07', 'CN-33-15', 'DEPENDENT', 4, 'CN', '{"zh": "新平县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15-08', 'CN-33-15', 'DEPENDENT', 4, 'CN', '{"zh": "易门县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-15-09', 'CN-33-15', 'DEPENDENT', 4, 'CN', '{"zh": "元江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-01', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "大关县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-02', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "鲁甸县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-03', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "巧家县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-04', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "水富县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-05', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "绥江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-06', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "威信县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-07', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "盐津县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-08', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "彝良县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-09', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "永善县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-10', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "昭阳区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-33-16-11', 'CN-33-16', 'DEPENDENT', 4, 'CN', '{"zh": "镇雄县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-01', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "滨江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-02', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "淳安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-03', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "富阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-04', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "拱墅区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-05', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "建德市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-06', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "江干区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-07', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "临安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-08', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "上城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-09', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "桐庐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-10', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "西湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-11', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "下城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-12', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "萧山区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-01-13', 'CN-34-01', 'DEPENDENT', 4, 'CN', '{"zh": "余杭区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-02-01', 'CN-34-02', 'DEPENDENT', 4, 'CN', '{"zh": "安吉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-02-02', 'CN-34-02', 'DEPENDENT', 4, 'CN', '{"zh": "德清县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-02-03', 'CN-34-02', 'DEPENDENT', 4, 'CN', '{"zh": "南浔区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-02-04', 'CN-34-02', 'DEPENDENT', 4, 'CN', '{"zh": "吴兴区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-02-05', 'CN-34-02', 'DEPENDENT', 4, 'CN', '{"zh": "长兴县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-03-01', 'CN-34-03', 'DEPENDENT', 4, 'CN', '{"zh": "海宁市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-03-02', 'CN-34-03', 'DEPENDENT', 4, 'CN', '{"zh": "海盐县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-03-03', 'CN-34-03', 'DEPENDENT', 4, 'CN', '{"zh": "嘉善县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-03-04', 'CN-34-03', 'DEPENDENT', 4, 'CN', '{"zh": "南湖区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-03-05', 'CN-34-03', 'DEPENDENT', 4, 'CN', '{"zh": "平湖市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-03-06', 'CN-34-03', 'DEPENDENT', 4, 'CN', '{"zh": "桐乡市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-03-07', 'CN-34-03', 'DEPENDENT', 4, 'CN', '{"zh": "秀洲区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04-01', 'CN-34-04', 'DEPENDENT', 4, 'CN', '{"zh": "东阳市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04-02', 'CN-34-04', 'DEPENDENT', 4, 'CN', '{"zh": "金东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04-03', 'CN-34-04', 'DEPENDENT', 4, 'CN', '{"zh": "兰溪市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04-04', 'CN-34-04', 'DEPENDENT', 4, 'CN', '{"zh": "磐安县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04-05', 'CN-34-04', 'DEPENDENT', 4, 'CN', '{"zh": "浦江县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04-06', 'CN-34-04', 'DEPENDENT', 4, 'CN', '{"zh": "武义县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04-07', 'CN-34-04', 'DEPENDENT', 4, 'CN', '{"zh": "婺城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04-08', 'CN-34-04', 'DEPENDENT', 4, 'CN', '{"zh": "义乌市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-04-09', 'CN-34-04', 'DEPENDENT', 4, 'CN', '{"zh": "永康市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05-01', 'CN-34-05', 'DEPENDENT', 4, 'CN', '{"zh": "缙云县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05-02', 'CN-34-05', 'DEPENDENT', 4, 'CN', '{"zh": "景宁县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05-03', 'CN-34-05', 'DEPENDENT', 4, 'CN', '{"zh": "莲都区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05-04', 'CN-34-05', 'DEPENDENT', 4, 'CN', '{"zh": "龙泉市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05-05', 'CN-34-05', 'DEPENDENT', 4, 'CN', '{"zh": "青田县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05-06', 'CN-34-05', 'DEPENDENT', 4, 'CN', '{"zh": "庆元县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05-07', 'CN-34-05', 'DEPENDENT', 4, 'CN', '{"zh": "松阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05-08', 'CN-34-05', 'DEPENDENT', 4, 'CN', '{"zh": "遂昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-05-09', 'CN-34-05', 'DEPENDENT', 4, 'CN', '{"zh": "云和县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-01', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "北仑区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-02', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "慈溪市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-03', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "奉化市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-04', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "海曙区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-05', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "江北区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-06', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "江东区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-07', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "宁海县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-08', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "象山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-09', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "鄞州区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-10', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "余姚市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-06-11', 'CN-34-06', 'DEPENDENT', 4, 'CN', '{"zh": "镇海区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-07-01', 'CN-34-07', 'DEPENDENT', 4, 'CN', '{"zh": "常山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-07-02', 'CN-34-07', 'DEPENDENT', 4, 'CN', '{"zh": "江山市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-07-03', 'CN-34-07', 'DEPENDENT', 4, 'CN', '{"zh": "开化县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-07-04', 'CN-34-07', 'DEPENDENT', 4, 'CN', '{"zh": "柯城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-07-05', 'CN-34-07', 'DEPENDENT', 4, 'CN', '{"zh": "龙游县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-07-06', 'CN-34-07', 'DEPENDENT', 4, 'CN', '{"zh": "衢江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-08-01', 'CN-34-08', 'DEPENDENT', 4, 'CN', '{"zh": "柯桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-08-02', 'CN-34-08', 'DEPENDENT', 4, 'CN', '{"zh": "上虞市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-08-03', 'CN-34-08', 'DEPENDENT', 4, 'CN', '{"zh": "嵊州市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-08-04', 'CN-34-08', 'DEPENDENT', 4, 'CN', '{"zh": "新昌县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-08-05', 'CN-34-08', 'DEPENDENT', 4, 'CN', '{"zh": "越城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-08-06', 'CN-34-08', 'DEPENDENT', 4, 'CN', '{"zh": "诸暨市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09-01', 'CN-34-09', 'DEPENDENT', 4, 'CN', '{"zh": "黄岩区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09-02', 'CN-34-09', 'DEPENDENT', 4, 'CN', '{"zh": "椒江区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09-03', 'CN-34-09', 'DEPENDENT', 4, 'CN', '{"zh": "临海市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09-04', 'CN-34-09', 'DEPENDENT', 4, 'CN', '{"zh": "路桥区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09-05', 'CN-34-09', 'DEPENDENT', 4, 'CN', '{"zh": "三门县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09-06', 'CN-34-09', 'DEPENDENT', 4, 'CN', '{"zh": "天台县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09-07', 'CN-34-09', 'DEPENDENT', 4, 'CN', '{"zh": "温岭市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09-08', 'CN-34-09', 'DEPENDENT', 4, 'CN', '{"zh": "仙居县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-09-09', 'CN-34-09', 'DEPENDENT', 4, 'CN', '{"zh": "玉环县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-01', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "苍南县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-02', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "洞头县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-03', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "乐清市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-04', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "龙湾区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-05', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "鹿城区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-06', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "瓯海区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-07', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "平阳县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-08', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "瑞安市"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-09', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "泰顺县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-10', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "文成县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-10-11', 'CN-34-10', 'DEPENDENT', 4, 'CN', '{"zh": "永嘉县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-11-01', 'CN-34-11', 'DEPENDENT', 4, 'CN', '{"zh": "岱山县"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-11-02', 'CN-34-11', 'DEPENDENT', 4, 'CN', '{"zh": "定海区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-11-03', 'CN-34-11', 'DEPENDENT', 4, 'CN', '{"zh": "普陀区"}', NULL, true, 'ads', '2026-10-29');
INSERT INTO public.region (code, parent_code, level, depth, country_code, names, pinyin, active, source_id, source_version) VALUES ('CN-34-11-04', 'CN-34-11', 'DEPENDENT', 4, 'CN', '{"zh": "嵊泗县"}', NULL, true, 'ads', '2026-10-29');



INSERT INTO public.address_format (country_code, required, fmt, zip, zipex, state_name_type, has_admin_list, admin_optional, upper, source_id, source_version) VALUES ('CN', 'ACSZ', '%Z%n%S%C%D%n%A%n%O%n%N', '\d{6}', '266033,317204,100096,100808', 'province', true, false, 'S', 'ads', '2026-10-29');
INSERT INTO public.address_format (country_code, required, fmt, zip, zipex, state_name_type, has_admin_list, admin_optional, upper, source_id, source_version) VALUES ('HK', 'AS', '%S%n%C%n%A%n%O%n%N', NULL, NULL, 'area', true, false, 'S', 'ads', '2026-10-29');
INSERT INTO public.address_format (country_code, required, fmt, zip, zipex, state_name_type, has_admin_list, admin_optional, upper, source_id, source_version) VALUES ('MO', 'A', '%A%n%O%n%N', NULL, NULL, 'province', false, true, NULL, 'ads', '2026-10-29');
INSERT INTO public.address_format (country_code, required, fmt, zip, zipex, state_name_type, has_admin_list, admin_optional, upper, source_id, source_version) VALUES ('TW', 'ACSZ', '%Z%n%S%C%n%A%n%O%n%N', '\d{3}(?:\d{2,3})?', '104,106,10603,40867', 'county', true, false, NULL, 'ads', '2026-10-29');
INSERT INTO public.address_format (country_code, required, fmt, zip, zipex, state_name_type, has_admin_list, admin_optional, upper, source_id, source_version) VALUES ('JP', 'ASZ', '〒%Z%n%S%n%A%n%O%n%N', '\d{3}-?\d{4}', '154-0023,350-1106,951-8073,112-0001,208-0032,231-0012', 'prefecture', true, false, 'S', 'ads', '2026-10-29');
INSERT INTO public.address_format (country_code, required, fmt, zip, zipex, state_name_type, has_admin_list, admin_optional, upper, source_id, source_version) VALUES ('US', 'ACSZ', '%N%n%O%n%A%n%C, %S %Z', '(\d{5})(?:[ \-](\d{4}))?', '95014,22162-1010', 'state', true, false, 'CS', 'ads', '2026-10-29');
INSERT INTO public.address_format (country_code, required, fmt, zip, zipex, state_name_type, has_admin_list, admin_optional, upper, source_id, source_version) VALUES ('SG', 'AZ', '%N%n%O%n%A%nSINGAPORE %Z', '\d{6}', '546080,308125,408600', 'province', false, true, NULL, 'ads', '2026-10-29');
INSERT INTO public.address_format (country_code, required, fmt, zip, zipex, state_name_type, has_admin_list, admin_optional, upper, source_id, source_version) VALUES ('MY', 'ACZ', '%N%n%O%n%A%n%D%n%Z %C%n%S', '\d{5}', '43000,50754,88990,50670', 'state', true, true, 'CS', 'ads', '2026-10-29');
INSERT INTO public.address_format (country_code, required, fmt, zip, zipex, state_name_type, has_admin_list, admin_optional, upper, source_id, source_version) VALUES ('TH', NULL, '%N%n%O%n%A%n%D %C%n%S %Z', '\d{5}', '10150,10210', 'province', true, true, 'S', 'ads', '2026-10-29');



INSERT INTO public.market (code, name, currency, active, created_at, country_code, tax_jurisdiction, price_includes_tax, timezone, legal_entity_name, sort) VALUES ('CN', '中国大陆', 'CNY', true, '2026-10-30 03:46:56.009015+00', 'CN', 'CN', true, 'Asia/Shanghai', NULL, 0);
































































































INSERT INTO public.brand (id, code, name, sort_order, created_at) VALUES ('bb000000-0000-0000-0000-000000000001', 'BRAND-DEMO-1', '演示品牌甲', 10, '2026-10-30 03:46:47.609248+00');
INSERT INTO public.brand (id, code, name, sort_order, created_at) VALUES ('bb000000-0000-0000-0000-000000000002', 'BRAND-DEMO-2', '演示品牌乙', 20, '2026-10-30 03:46:47.609248+00');















INSERT INTO public.coupon (id, code, name, discount_type, discount_value, min_spend_amount, max_discount_amount, member_id, currency, status, valid_from, valid_to, locked_at, used_at, sale_id, created_at) VALUES ('c1000000-0000-0000-0000-000000000003', 'MILK5', '鲜奶立减 5 元', 'FIXED', 500, 0, NULL, NULL, 'CNY', 'ISSUED', '2026-10-29 03:46:38.483267+00', '2027-10-30 03:46:38.483267+00', NULL, NULL, NULL, '2026-10-30 03:46:38.483267+00');
























INSERT INTO public.feature_flag (code, enabled, updated_by, updated_at, reason) VALUES ('supplier.enabled', true, 'migration:V22', '2026-10-30 03:46:40.96996+00', '初始值：供应商模块开启');
INSERT INTO public.feature_flag (code, enabled, updated_by, updated_at, reason) VALUES ('pos.sale.enabled', true, 'migration:V22', '2026-10-30 03:46:40.96996+00', '初始值：POS 卖货开启');
INSERT INTO public.feature_flag (code, enabled, updated_by, updated_at, reason) VALUES ('pos.pickup.enabled', true, 'migration:V22', '2026-10-30 03:46:40.96996+00', '初始值：POS 自提核销开启');








































































INSERT INTO public.market_destination (market_code, destination_country, deliverable) VALUES ('CN', 'CN', true);









INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'company_name', '演示商城（中国）有限公司', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'icp_number', '京ICP备00000000号-1', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'public_security_record', '京公网安备 00000000000000号', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'service_phone', '400-000-0000', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'service_email', 'support@example.com', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'default_currency', 'CNY', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'storage_provider', 'mock', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'sms_provider', 'mock', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'payment_provider', 'mock', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'logistics_provider', 'mock', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'business_license_no', '', '2026-10-30 03:46:56.045667+00');
INSERT INTO public.market_setting (market_code, setting_key, setting_value, updated_at) VALUES ('CN', 'enabled_locales', 'en,ja', '2026-10-30 03:46:56.045667+00');


















INSERT INTO public.member_level (code, name, discount_rate, points_multiplier, free_shipping, birthday_points, min_growth, sort_order) VALUES ('STANDARD', '普通会员', 100, 1, false, 100, 0, 10);
INSERT INTO public.member_level (code, name, discount_rate, points_multiplier, free_shipping, birthday_points, min_growth, sort_order) VALUES ('SILVER', '银卡会员', 98, 1, false, 200, 1000, 20);
INSERT INTO public.member_level (code, name, discount_rate, points_multiplier, free_shipping, birthday_points, min_growth, sort_order) VALUES ('GOLD', '金卡会员', 95, 2, true, 500, 5000, 30);
INSERT INTO public.member_level (code, name, discount_rate, points_multiplier, free_shipping, birthday_points, min_growth, sort_order) VALUES ('PLATINUM', '白金会员', 92, 2, true, 1000, 20000, 40);




































INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('dashboard.view', '查看经营总览', '工作台', 10);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('product.manage', '商品管理', '商品', 20);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('order.manage', '订单管理', '交易', 30);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('customer.view', '客户与会员', '客户', 40);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('inventory.view', '库存与批次', '库存', 50);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('warehouse.manage', '仓储作业', '库存', 60);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('pos.manage', '门店收银', '门店', 70);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('supplier.manage', '供应商与凭证', '供应商', 80);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('notification.view', '通知中心', '运营', 90);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('theme.manage', '主题配置', '运营', 100);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('staff.manage', '员工与权限', '系统', 110);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('settings.manage', '系统设置', '系统', 120);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('permission.manage', '权限矩阵', '系统', 130);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('promotion.manage', '促销与分销', '营销', 65);
INSERT INTO public.permission (code, label, group_name, sort_order) VALUES ('withdrawal.review', '提现审核与打款', '财务', 70);


























































































INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'dashboard.view');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'product.manage');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'order.manage');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'customer.view');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'inventory.view');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'warehouse.manage');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'pos.manage');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'supplier.manage');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'notification.view');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'theme.manage');
INSERT INTO public.role_permission (role, permission_code) VALUES ('OPERATOR', 'promotion.manage');
INSERT INTO public.role_permission (role, permission_code) VALUES ('ADMIN', 'withdrawal.review');









INSERT INTO public.shipping_rate (id, market_code, destination_country, min_weight_grams, max_weight_grams, fee_amount, currency, free_over_amount, created_at) VALUES ('fa431c4c-59ff-43ee-8461-0e8cd232dd4c', 'CN', 'CN', 0, NULL, 0, 'CNY', NULL, '2026-10-30 03:46:56.956078+00');


























































































INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('site_name', 'Full Mall', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('company_name', '演示商城（中国）有限公司', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('icp_number', '京ICP备00000000号-1', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('public_security_record', '京公网安备 00000000000000号', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('service_phone', '400-000-0000', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('service_email', 'support@example.com', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('site_status', 'OPEN', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('maintenance_message', '系统维护中，请稍后再试。', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('default_locale', 'zh-CN', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('default_currency', 'CNY', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('storage_provider', 'mock', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('sms_provider', 'mock', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('payment_provider', 'mock', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('logistics_provider', 'mock', '2026-10-30 03:46:37.903646+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('site_domain', '', '2026-10-30 03:46:53.51779+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('business_license_no', '', '2026-10-30 03:46:53.608578+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('site_scheme', 'https', '2026-10-30 03:46:53.811829+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('default_market', 'CN', '2026-10-30 03:46:53.897605+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('site_tagline', '全品类商城', '2026-10-30 03:46:53.897605+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('branding_attribution', 'SHOW', '2026-10-30 03:46:53.897605+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('enabled_locales', 'en,ja', '2026-10-30 03:46:54.100077+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('app.timezone', 'Asia/Shanghai', '2026-10-30 03:46:54.18345+00');
INSERT INTO public.system_setting (setting_key, setting_value, updated_at) VALUES ('media_public_base_url', '', '2026-10-30 03:46:54.618501+00');















INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.home', '首页', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.products', '全部商品', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.cart', '购物车', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.orders', '我的订单', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.addToCart', '加入购物车', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.outOfStock', '暂时缺货', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.pay', '去支付', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'review.submit', '提交评价', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'review.pending', '评价审核中，通过后展示', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'common.loading', '加载中', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.home', 'Home', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.products', 'Products', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.cart', 'Cart', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.orders', 'Orders', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.addToCart', 'Add to cart', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.outOfStock', 'Out of stock', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.pay', 'Checkout', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'review.submit', 'Submit review', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'review.pending', 'Your review is under review', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'common.loading', 'Loading', '2026-10-30 03:46:50.424534+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'topbar.welcome', '欢迎来到 Full Mall，正品保障 · 门店自提 · 七天无理由', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'topbar.welcome', 'Welcome to Full Mall — authentic products, pickup and 7-day returns', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.account', '我的订单', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.account', 'My orders', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.favorites', '我的收藏', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.favorites', 'Favorites', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.history', '浏览足迹', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.history', 'Recently viewed', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.notifications', '消息中心', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.notifications', 'Notifications', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.help', '帮助中心', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.help', 'Help center', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.allProducts', '全部商品', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.allProducts', 'All products', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.promotions', '促销活动', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.promotions', 'Promotions', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'nav.pickup', '门店自提', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'nav.pickup', 'Store pickup', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'search.placeholder', '搜索商品、品牌、工业型号', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'search.placeholder', 'Search products, brands or model numbers', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'search.button', '搜索', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'search.button', 'Search', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.eyebrow', 'FULL MALL · 品质生活', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.eyebrow', 'FULL MALL · Everyday quality', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.title', '好物直达，门店自提更快', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.title', 'Quality goods, faster store pickup', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.copy', '覆盖生鲜、数码、家居、美妆、运动和工业品。支持多仓发货、门店自提、供应商代发与批次效期管理。', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.copy', 'Fresh food, electronics, home, beauty, sports and industrial supplies — with multi-warehouse shipping, pickup, dropshipping and batch/expiry tracking.', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.shop', '立即选购', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.shop', 'Shop now', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.pickup', '查看自提规则', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.pickup', 'Pickup policy', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.stat.pickup', '门店自提', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.stat.pickup', 'store pickup', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.stat.returns', '无理由退货', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.stat.returns', 'day returns', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.stat.auth', '正品保障', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.stat.auth', 'authentic', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.deals', '今日特惠', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.deals', 'Today deals', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.dealsMore', '查看全部 →', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.dealsMore', 'View all →', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.hot', '大家都在买', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.hot', 'Popular now', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.hotMore', '更多商品 →', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.hotMore', 'More products →', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.fresh', '生鲜与批次效期', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.fresh', 'Fresh food and batch expiry', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.industrial', '工业品与设备选型', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.industrial', 'Industrial supplies', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.industrialCopy', '步进电机、法兰、标准件，支持批量采购和供应商直发。', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.industrialCopy', 'Stepper motors, flanges and standard parts with bulk purchase and supplier shipping.', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.industrialCta', '进入工业品专区', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.industrialCta', 'Explore industrial', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'home.empty', '暂无商品，请确认后端已启动并完成 seed。', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'home.empty', 'No products yet. Check that the API is running and seeded.', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.badge.deal', '限时价', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.badge.deal', 'Deal', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.badge.batch', '批次/效期', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.badge.batch', 'Batch/expiry', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.badge.self', '自营', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.badge.self', 'Official', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.tag.freeShipping', '包邮', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.tag.freeShipping', 'Free shipping', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.tag.returns', '七天无理由', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.tag.returns', '7-day returns', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.tag.pickup', '门店自提', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.tag.pickup', 'Store pickup', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.sold', '已售', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.sold', 'sold', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.view', '查看详情', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.view', 'View details', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.auth.title', '正品保障', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.auth.title', 'Authentic', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.auth.desc', '品牌直供，品质可追溯', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.auth.desc', 'Direct from brands, traceable quality', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.refund.title', '极速退款', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.refund.title', 'Fast refunds', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.refund.desc', '售后审核通过后原路退回', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.refund.desc', 'Refunded to the original method after approval', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.pickup.title', '门店自提', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.favorited', 'Saved', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.pickup.title', 'Store pickup', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.pickup.desc', '就近门店，扫码核销', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.pickup.desc', 'Pick up nearby and scan to redeem', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.returns.title', '七天无理由', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.returns.title', '7-day returns', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.returns.desc', '符合条件的商品支持', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.returns.desc', 'Available for eligible items', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.company', '运营主体', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.company', 'Operated by', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.contact', '客服', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.contact', 'Support', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.icp', 'ICP备案', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.icp', 'ICP filing', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.police', '公网安备', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.police', 'Public security filing', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.license', 'Business licence', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.legal.terms', '服务条款', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.legal.terms', 'Terms', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.legal.privacy', '隐私政策', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.legal.privacy', 'Privacy', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.legal.refund', '退换货政策', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.legal.refund', 'Refunds', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.legal.pickup', '自提规则', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.legal.pickup', 'Pickup policy', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.note', '金额单位：分 · 演示数据不产生真实交易', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.note', 'Amounts in cents · Demo data, no real transactions', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'maintenance.title', '系统维护中', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'maintenance.title', 'Under maintenance', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'maintenance.fallback', '系统维护中，请稍后再试。', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'maintenance.fallback', 'We are performing maintenance. Please try again later.', '2026-10-30 03:46:53.619005+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.license', '营业执照 / 经营编号', '2026-10-30 03:46:53.719879+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.note.prod', '价格含税 · 门店自提 · 七天无理由', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.note.prod', 'Tax included · Store pickup · 7-day returns', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.login.title', '登录后继续结算', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.login.title', 'Sign in to continue', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.login.desc', '登录或注册后将回到本页，购物车和填写内容都会保留。', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.login.desc', 'Sign in or register and you will return here. Your cart and entries are kept.', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.account.auto', '未注册的手机号验证后将自动创建账号，并同时同意《服务条款》与《隐私政策》。', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.account.auto', 'A new account is created automatically after phone verification, and you agree to the Terms and Privacy Policy.', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'footer.attribution', 'Powered by Full Mall', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'footer.attribution', 'Powered by Full Mall', '2026-10-30 03:46:53.906348+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'category.BEAUTY', '美妆个护', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'category.BEAUTY', 'Beauty & care', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'category.DIGITAL', '数码家电', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'category.DIGITAL', 'Electronics', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'category.FRESH', '生鲜食品', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'category.FRESH', 'Fresh food', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'category.HOME', '家居日用', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'category.HOME', 'Home & living', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'category.INDUSTRIAL', '工业品', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'category.INDUSTRIAL', 'Industrial', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'category.SPORTS', '运动户外', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'category.SPORTS', 'Sports & outdoors', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'site.tagline', '全品类商城', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'site.tagline', 'All-category mall', '2026-10-30 03:46:54.263937+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.current', '现行版本', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.current', 'Current version', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.upcoming', '即将生效', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.upcoming', 'Effective soon', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.expired', '已失效', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.expired', 'Expired', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.revision', '修订版本', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.revision', 'Revision', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.effectiveFrom', '{date} 起生效', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.effectiveFrom', 'Effective from {date}', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.effectiveOn', '将于 {date} 生效', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.effectiveOn', 'Effective on {date}', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.validUntil', '有效期至', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.validUntil', 'Valid until', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.fallback', '该语言暂无译本，当前展示简体中文版本。', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.fallback', 'No translation is available; showing the Simplified Chinese version.', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.footnote', '条款更新以标注的生效时间为准；新版本生效前，仍以现行版本为准。', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.footnote', 'Updates take effect on the stated date; until then the current version applies.', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.type.TERMS', '服务条款', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.type.TERMS', 'Terms of service', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.type.PRIVACY', '隐私政策', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.type.PRIVACY', 'Privacy policy', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.type.REFUND', '退款与售后政策', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.type.REFUND', 'Refund & returns policy', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'legal.type.PICKUP', '门店自提规则', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'legal.type.PICKUP', 'Store pickup rules', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.tab.detail', '商品详情', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.tab.detail', 'Details', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.tab.specs', '规格参数', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.tab.specs', 'Specifications', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.tab.reviews', '用户评价', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.tab.reviews', 'Reviews', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.tab.service', '售后保障', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.tab.service', 'Service', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.quantity', '数量', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.quantity', 'Qty', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.buyNow', '立即购买', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.buyNow', 'Buy now', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.favorite', '收藏', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'product.favorite', 'Save', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'product.favorited', '已收藏', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'list.end.prefix', '已展示全部', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'list.end.prefix', 'All', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'list.end.noun', '件商品', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'list.end.noun', 'items shown', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'list.end.reset', '清空筛选', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'list.end.reset', 'Clear filters', '2026-10-30 03:46:54.349798+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.title', '结算', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.title', 'Checkout', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.items', '个商品', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.items', 'items', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.fulfillment', '履约方式', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.fulfillment', 'Fulfillment', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.ship', '快递配送', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.ship', 'Shipping', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.pickup', '门店自提', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.pickup', 'Store pickup', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.store', '自提门店', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.store', 'Pickup store', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.address', '收货地址', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.address', 'Address', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.addressPlaceholder', '请输入详细地址', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.addressPlaceholder', 'Enter your address', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.contact', '联系人', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.contact', 'Contact', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.contactPlaceholder', '请输入收货人姓名', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.contactPlaceholder', 'Recipient name', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.phone', '手机号', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.phone', 'Phone', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.summary', '订单摘要', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.summary', 'Order summary', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.summaryHint', '秒杀、买赠与拼团价格由服务端结算时校验，最终应付以订单为准。', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.summaryHint', 'Flash sale, gift and group-buy prices are verified at checkout; the order total applies.', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.total', '合计', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.total', 'Total', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.agree', '我已阅读并同意', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.agree', 'I have read and agree to', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.submit', '提交订单', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.submit', 'Place order', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.submitting', '提交中…', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.submitting', 'Submitting…', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.loginToOrder', '登录后下单', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.loginToOrder', 'Sign in to order', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.demoResult', '查看演示结果', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.demoResult', 'View demo result', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'checkout.cartEmpty', '购物车为空', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'checkout.cartEmpty', 'Your cart is empty', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'login.title', '手机号登录 / 注册', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'login.title', 'Sign in / Register', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'login.phone', '手机号', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'login.phone', 'Phone', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'login.code', '短信验证码', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'login.code', 'Verification code', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'login.sendCode', '获取验证码', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'login.sendCode', 'Send code', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'login.sending', '发送中…', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'login.sending', 'Sending…', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'login.submit', '登录 / 注册', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'login.submit', 'Sign in / Register', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'login.submitting', '登录中…', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'login.submitting', 'Signing in…', '2026-10-30 03:46:54.434033+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.country', '国家/地区', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.country', 'Country/region', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.state', '省/州', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.state', 'State/province', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.stateType.province', '省', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.stateType.province', 'Province', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.stateType.state', '州', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.stateType.state', 'State', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.stateType.prefecture', '都道府县', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.stateType.prefecture', 'Prefecture', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.stateType.emirate', '酋长国', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.stateType.emirate', 'Emirate', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.stateType.county', '县/市', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.stateType.county', 'County/city', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.stateType.area', '地区', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.stateType.area', 'Area', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.stateType.do_si', '道/市', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.stateType.do_si', 'Do/Si', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.city', '城市', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.city', 'City', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.district', '区/镇', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.district', 'District', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.postalCode', '邮政编码', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.postalCode', 'Postal code', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.postalInvalid', '邮政编码格式不正确', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.postalInvalid', 'Postal code format is invalid', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.select', '请选择', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.select', 'Please select', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.required', '此项必填', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.required', 'This field is required', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.noRegionData', '该地区暂无区划列表，可直接填写文字', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.noRegionData', 'No region list for this country yet — type the name instead', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.formatMissing', '该国家暂不支持配送，请更换国家', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.formatMissing', 'Delivery to this country is not supported yet', '2026-10-30 03:46:56.595009+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('zh-CN', 'address.notDeliverable', '暂不支持配送到该国家/地区，请更换国家或联系客服', '2026-10-30 03:46:57.394353+00');
INSERT INTO public.ui_translation (locale, text_key, text_value, updated_at) VALUES ('en', 'address.notDeliverable', 'Delivery to this country/region is not available yet', '2026-10-30 03:46:57.394353+00');












SELECT pg_catalog.setval('public.jv_commit_pk_seq', 1, false);



SELECT pg_catalog.setval('public.jv_global_id_pk_seq', 1, false);



SELECT pg_catalog.setval('public.jv_snapshot_pk_seq', 1, false);




INSERT INTO public.role (code, label, editable, sort_order) VALUES
('ADMIN', '超级管理员', false, 10);
INSERT INTO public.role (code, label, editable, sort_order) VALUES
('OPERATOR', '运营人员', true, 20);
INSERT INTO public.role (code, label, editable, sort_order) VALUES
('STORE', '门店人员', true, 30);
INSERT INTO public.role (code, label, editable, sort_order) VALUES
('CS', '客服人员', true, 40);
INSERT INTO public.role (code, label, editable, sort_order) VALUES
('SUPPLIER', '供应商', false, 50);
SET session_replication_role = DEFAULT;
