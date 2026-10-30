-- 基线 schema：由 scripts/squash-migrations.sh 从仅用旧迁移建出的库导出（项目未上线，一次性初始化）。
-- 结构改动请直接改本文件；上线后再用增量迁移（V3 起）。seed 数据见 V2__seed.sql。





CREATE FUNCTION public.coupon_template_sync_scope() RETURNS trigger
    LANGUAGE plpgsql
    AS $_$
BEGIN
    DELETE FROM coupon_template_scope WHERE template_id = NEW.id;
    IF NEW.scope IN ('SKU', 'CATEGORY', 'BRAND') THEN
        INSERT INTO coupon_template_scope (template_id, scope_type, scope_id)
        SELECT NEW.id, NEW.scope, ids.value::uuid
          FROM jsonb_array_elements_text(NEW.scope_ids) AS ids(value)
         WHERE ids.value ~ '^[0-9a-fA-F-]{36}$'
        ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
END;
$_$;



CREATE FUNCTION public.promotion_sync_rule_columns() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.rules IS NOT NULL THEN
        NEW.threshold_amount := NULLIF(NEW.rules->>'thresholdAmount', '')::bigint;
        NEW.rule_value       := NULLIF(NEW.rules->>'value', '')::bigint;
        NEW.channel          := COALESCE(NULLIF(NEW.rules->>'channel', ''), 'ONLINE');
        NEW.exclusive_group  := NULLIF(NEW.rules->>'exclusiveGroup', '');
    END IF;
    RETURN NEW;
END;
$$;



CREATE FUNCTION public.sku_shelf_life_sync() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.shelf_life_policy_json IS NOT NULL THEN
        NEW.shelf_batch_tracking     := COALESCE((NEW.shelf_life_policy_json->>'batchTracking')::boolean, FALSE);
        NEW.shelf_picking_strategy   := COALESCE(NULLIF(NEW.shelf_life_policy_json->>'pickingStrategy', ''), 'NONE');
        NEW.shelf_min_remaining_days := COALESCE((NEW.shelf_life_policy_json->>'minRemainingShelfLifeDays')::int, 0);
        NEW.shelf_near_expiry_days   := COALESCE((NEW.shelf_life_policy_json->>'nearExpiryThresholdDays')::int, 0);
        NEW.shelf_near_expiry_action := COALESCE(NULLIF(NEW.shelf_life_policy_json->>'nearExpiryAction', ''), 'NORMAL');
        NEW.shelf_auto_block_expired := COALESCE((NEW.shelf_life_policy_json->>'autoBlockExpired')::boolean, TRUE);
    END IF;
    RETURN NEW;
END;
$$;



CREATE FUNCTION public.touch_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at := GREATEST(now(), OLD.updated_at + interval '1 millisecond');
    RETURN NEW;
END;
$$;





CREATE TABLE public.address_format (
    country_code character(2) NOT NULL,
    required character varying(16),
    fmt character varying(64) NOT NULL,
    zip character varying(512),
    zipex character varying(128),
    state_name_type character varying(32),
    has_admin_list boolean NOT NULL,
    admin_optional boolean NOT NULL,
    upper character varying(16),
    source_id character varying(32) NOT NULL,
    source_version character varying(32) NOT NULL
);



CREATE TABLE public.after_sale (
    id uuid NOT NULL,
    after_sale_no character varying(64) NOT NULL,
    order_id uuid NOT NULL,
    type character varying(16) NOT NULL,
    status character varying(24) NOT NULL,
    reason character varying(255) NOT NULL,
    description text,
    contact_phone character varying(32) NOT NULL,
    refund_amount bigint DEFAULT 0 NOT NULL,
    currency character(3) NOT NULL,
    admin_note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    approved_at timestamp with time zone,
    received_at timestamp with time zone,
    refunded_at timestamp with time zone,
    rejected_at timestamp with time zone,
    exchange_carrier_code character varying(32),
    exchange_tracking_no character varying(128),
    exchange_shipped_at timestamp with time zone,
    auto_complete_at timestamp with time zone,
    disputed_at timestamp with time zone,
    auto_completed boolean DEFAULT false NOT NULL,
    quality_result character varying(16),
    quality_note character varying(500),
    quality_checked_at timestamp with time zone,
    CONSTRAINT after_sale_quality_check CHECK (((quality_result IS NULL) OR ((quality_result)::text = ANY ((ARRAY['PASS'::character varying, 'FAIL'::character varying])::text[]))))
);



COMMENT ON COLUMN public.after_sale.status IS 'REQUESTED/APPROVED/RECEIVED/REFUNDED/REJECTED/EXCHANGE_SHIPPED';



CREATE TABLE public.after_sale_event (
    id uuid NOT NULL,
    after_sale_id uuid NOT NULL,
    event_type character varying(32) NOT NULL,
    note text,
    operator_name character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.after_sale_line (
    id uuid NOT NULL,
    after_sale_id uuid NOT NULL,
    order_line_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    quantity integer NOT NULL,
    CONSTRAINT after_sale_line_quantity_positive CHECK ((quantity > 0))
);



CREATE TABLE public.ai_event (
    id uuid NOT NULL,
    event_type character varying(32) NOT NULL,
    member_id uuid,
    anonymous_id character varying(64),
    sku_id uuid,
    keyword character varying(128),
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ai_event_type_check CHECK (((event_type)::text = ANY ((ARRAY['VIEW'::character varying, 'ADD_TO_CART'::character varying, 'SEARCH'::character varying, 'CLICK'::character varying, 'PURCHASE'::character varying])::text[])))
);



CREATE TABLE public.ai_gen_job (
    id uuid NOT NULL,
    kind character varying(16) NOT NULL,
    provider_id uuid NOT NULL,
    product_id uuid,
    sku_id uuid,
    purpose character varying(24) NOT NULL,
    prompt text NOT NULL,
    negative text,
    params jsonb DEFAULT '{}'::jsonb NOT NULL,
    source_asset_ids jsonb DEFAULT '[]'::jsonb NOT NULL,
    status character varying(16) DEFAULT 'PENDING'::character varying NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    error text,
    asset_id uuid,
    created_by character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ai_gen_job_kind_check CHECK (((kind)::text = ANY ((ARRAY['IMAGE'::character varying, 'VIDEO'::character varying, 'TEXT'::character varying])::text[]))),
    CONSTRAINT ai_gen_job_purpose_check CHECK (((purpose)::text = ANY ((ARRAY['MAIN'::character varying, 'SPEC'::character varying, 'DETAIL'::character varying, 'VIDEO'::character varying, 'COPY'::character varying])::text[]))),
    CONSTRAINT ai_gen_job_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'RUNNING'::character varying, 'SUCCEEDED'::character varying, 'FAILED'::character varying])::text[])))
);



COMMENT ON TABLE public.ai_gen_job IS '异步生成任务；结果落到资产库后回填 asset_id';



CREATE TABLE public.ai_prompt_history (
    id uuid NOT NULL,
    kind character varying(16) NOT NULL,
    purpose character varying(24) NOT NULL,
    product_id uuid,
    sku_id uuid,
    prompt text NOT NULL,
    negative text,
    provider_code character varying(48),
    asset_id uuid,
    created_by character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



COMMENT ON TABLE public.ai_prompt_history IS '历史提示词，可直接上屏复用以减少重复输入';



CREATE TABLE public.ai_provider (
    id uuid NOT NULL,
    kind character varying(16) NOT NULL,
    code character varying(48) NOT NULL,
    name character varying(96) NOT NULL,
    protocol character varying(32) NOT NULL,
    endpoint character varying(500) NOT NULL,
    api_key text,
    model character varying(96) NOT NULL,
    extra jsonb DEFAULT '{}'::jsonb NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    priority integer DEFAULT 100 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    vision boolean DEFAULT false NOT NULL,
    CONSTRAINT ai_provider_kind_check CHECK (((kind)::text = ANY ((ARRAY['IMAGE'::character varying, 'VIDEO'::character varying, 'TEXT'::character varying])::text[]))),
    CONSTRAINT ai_provider_protocol_check CHECK (((protocol)::text = ANY ((ARRAY['openai-images'::character varying, 'dashscope-image'::character varying, 'dashscope-video'::character varying, 'dashscope-multimodal'::character varying, 'ark-image'::character varying, 'ark-video'::character varying, 'minimax-video'::character varying, 'openai-chat'::character varying, 'openai-embedding'::character varying, 'clip-http'::character varying])::text[])))
);



COMMENT ON TABLE public.ai_provider IS 'AI 供应商（同类可多家，按 priority 选择；使用时也可显式指定）';



CREATE TABLE public.ai_storyboard (
    id uuid NOT NULL,
    title character varying(160) NOT NULL,
    product_id uuid,
    sku_id uuid,
    created_by character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.ai_storyboard_shot (
    id uuid NOT NULL,
    storyboard_id uuid NOT NULL,
    sort_order integer NOT NULL,
    prompt text NOT NULL,
    guide_asset_id uuid,
    duration_sec integer DEFAULT 5 NOT NULL,
    job_id uuid,
    asset_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    guide_asset_ids jsonb DEFAULT '[]'::jsonb NOT NULL
);



COMMENT ON COLUMN public.ai_storyboard_shot.guide_asset_id IS '分镜引导图（首帧）素材 id，来自素材库';



COMMENT ON COLUMN public.ai_storyboard_shot.guide_asset_ids IS '引导图素材 id 列表；第一张作为首帧，其余作为参考图（SUBJECT 由前端 role 决定）';



CREATE TABLE public.analytics_event (
    id uuid NOT NULL,
    event_type character varying(64) NOT NULL,
    occurred_at timestamp with time zone DEFAULT now() NOT NULL,
    market_code character varying(8),
    channel character varying(32),
    store_id uuid,
    warehouse_id uuid,
    supplier_id uuid,
    order_id uuid,
    sku_id uuid,
    batch_id uuid,
    properties_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    schema_version integer DEFAULT 1 NOT NULL
);



CREATE TABLE public.article (
    id uuid NOT NULL,
    category_id uuid,
    slug character varying(128) NOT NULL,
    type character varying(24) NOT NULL,
    status character varying(16) DEFAULT 'DRAFT'::character varying NOT NULL,
    published_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT article_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT article_type_check CHECK (((type)::text = ANY ((ARRAY['ANNOUNCEMENT'::character varying, 'HELP'::character varying, 'PAGE'::character varying])::text[])))
);



CREATE TABLE public.article_category (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.article_translation (
    article_id uuid NOT NULL,
    locale character varying(16) NOT NULL,
    title character varying(255) NOT NULL,
    summary character varying(512),
    content text NOT NULL,
    seo_title character varying(255),
    seo_description character varying(512),
    status character varying(16) DEFAULT 'DRAFT'::character varying NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT article_translation_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'MACHINE'::character varying, 'REVIEWED'::character varying, 'PUBLISHED'::character varying, 'OUTDATED'::character varying])::text[])))
);



CREATE TABLE public.asset (
    id uuid NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    name character varying(200) NOT NULL,
    url character varying(1024) NOT NULL,
    kind character varying(32) NOT NULL,
    owner_username character varying(64) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_by uuid,
    deleted_at timestamp with time zone,
    purpose character varying(24),
    tags text[] DEFAULT '{}'::text[] NOT NULL,
    caption text,
    indexed_at timestamp with time zone,
    CONSTRAINT asset_purpose_check CHECK (((purpose)::text = ANY ((ARRAY['PRODUCT'::character varying, 'SPEC'::character varying, 'VIDEO'::character varying, 'AI_GENERATED'::character varying, 'OTHER_SYSTEM'::character varying])::text[]))),
    source character varying(24) DEFAULT 'UPLOAD'::character varying NOT NULL,
    sha256 character varying(64),
    status character varying(16) DEFAULT 'ACTIVE'::character varying NOT NULL
);



COMMENT ON COLUMN public.asset.purpose IS 'PRODUCT/SPEC/VIDEO/AI_GENERATED 进素材库；OTHER_SYSTEM 给审批附件等非商品素材，不在素材库展示';



CREATE TABLE public.asset_image_embedding (
    asset_id uuid NOT NULL,
    vector double precision[] NOT NULL,
    model text NOT NULL,
    provider text,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



COMMENT ON TABLE public.asset_image_embedding IS '素材图像向量（多模态/CLIP），用于"用一句话搜图片内容"；维度由模型决定，与 knowledge_embedding 分开';



CREATE TABLE public.asset_locale_variant (
    asset_id uuid NOT NULL,
    locale text NOT NULL,
    url text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



COMMENT ON TABLE public.asset_locale_variant IS '素材语言变体：asset_id+locale → 本地化图片 URL；商品详情渲染时按语言替换 img 的 src';



CREATE TABLE public.audit_event (
    id uuid NOT NULL,
    event_type character varying(64) NOT NULL,
    actor character varying(128) NOT NULL,
    resource character varying(64) NOT NULL,
    resource_id character varying(64),
    detail character varying(1024),
    request_id character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.audit_log (
    id uuid NOT NULL,
    actor_id uuid,
    action character varying(64) NOT NULL,
    resource_type character varying(64) NOT NULL,
    resource_id uuid,
    before_json jsonb,
    after_json jsonb,
    ip character varying(64),
    user_agent text,
    occurred_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.audit_outbox (
    id uuid NOT NULL,
    entity_type character varying(255) NOT NULL,
    entity_id character varying(128) NOT NULL,
    author character varying(128) NOT NULL,
    payload jsonb NOT NULL,
    status character varying(16) DEFAULT 'PENDING'::character varying NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    last_error character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    processed_at timestamp with time zone,
    CONSTRAINT audit_outbox_attempts_non_negative CHECK ((attempts >= 0)),
    CONSTRAINT audit_outbox_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'DONE'::character varying, 'FAILED'::character varying])::text[])))
);



CREATE TABLE public.bargain_activity (
    id uuid NOT NULL,
    sku_id uuid NOT NULL,
    floor_price bigint NOT NULL,
    currency character(3) NOT NULL,
    max_cut bigint NOT NULL,
    min_helpers integer NOT NULL,
    cut_min bigint NOT NULL,
    cut_max bigint NOT NULL,
    expires_hours integer NOT NULL,
    allow_below_cost boolean DEFAULT false NOT NULL,
    below_cost_reason character varying(255),
    status character varying(16) NOT NULL,
    starts_at timestamp with time zone NOT NULL,
    ends_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT bargain_activity_cut_check CHECK (((cut_min > 0) AND (cut_max >= cut_min))),
    CONSTRAINT bargain_activity_expiry_check CHECK (((expires_hours >= 12) AND (expires_hours <= 72))),
    CONSTRAINT bargain_activity_floor_check CHECK ((floor_price > 0)),
    CONSTRAINT bargain_activity_helpers_check CHECK (((min_helpers >= 3) AND (min_helpers <= 30))),
    CONSTRAINT bargain_activity_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'ENDED'::character varying])::text[])))
);



CREATE TABLE public.bargain_help (
    id uuid NOT NULL,
    session_id uuid NOT NULL,
    helper_member_id uuid NOT NULL,
    cut_amount bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT bargain_help_cut_check CHECK ((cut_amount > 0))
);



CREATE TABLE public.bargain_session (
    id uuid NOT NULL,
    activity_id uuid NOT NULL,
    member_id uuid NOT NULL,
    current_price bigint NOT NULL,
    helper_count integer DEFAULT 0 NOT NULL,
    status character varying(16) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    order_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT bargain_session_price_check CHECK ((current_price > 0)),
    CONSTRAINT bargain_session_status_check CHECK (((status)::text = ANY ((ARRAY['ONGOING'::character varying, 'READY'::character varying, 'EXPIRED'::character varying, 'ORDERED'::character varying])::text[])))
);



CREATE TABLE public.brand (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.cart (
    id uuid NOT NULL,
    owner_key character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.cart_item (
    id uuid NOT NULL,
    cart_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    quantity integer NOT NULL,
    added_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT cart_item_quantity_positive CHECK ((quantity > 0))
);



CREATE TABLE public.category (
    id uuid NOT NULL,
    parent_id uuid,
    code character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.commission_ledger (
    id uuid NOT NULL,
    commission_id uuid NOT NULL,
    from_status character varying(16) NOT NULL,
    to_status character varying(16) NOT NULL,
    amount_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    reason character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.coupon (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    discount_type character varying(16) NOT NULL,
    discount_value bigint NOT NULL,
    min_spend_amount bigint DEFAULT 0 NOT NULL,
    max_discount_amount bigint,
    member_id uuid,
    currency character(3) DEFAULT 'CNY'::bpchar NOT NULL,
    status character varying(16) DEFAULT 'ISSUED'::character varying NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone NOT NULL,
    locked_at timestamp with time zone,
    used_at timestamp with time zone,
    sale_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT coupon_discount_value_positive CHECK ((discount_value > 0))
);



CREATE TABLE public.coupon_redemption (
    id uuid NOT NULL,
    coupon_id uuid NOT NULL,
    sale_id uuid NOT NULL,
    amount_amount bigint NOT NULL,
    operator_name character varying(64),
    rolled_back_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.coupon_template (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    title character varying(128) NOT NULL,
    type character varying(16) NOT NULL,
    value bigint NOT NULL,
    min_order_amount bigint DEFAULT 0 NOT NULL,
    total_quantity integer NOT NULL,
    issued_quantity integer DEFAULT 0 NOT NULL,
    per_member_limit integer DEFAULT 1 NOT NULL,
    validity_days integer NOT NULL,
    starts_at timestamp with time zone NOT NULL,
    ends_at timestamp with time zone NOT NULL,
    status character varying(16) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    scope character varying(16) DEFAULT 'ALL'::character varying NOT NULL,
    scope_ids jsonb DEFAULT '[]'::jsonb NOT NULL,
    stackable boolean,
    CONSTRAINT coupon_template_limit_positive CHECK ((per_member_limit > 0)),
    CONSTRAINT coupon_template_min_order_non_negative CHECK ((min_order_amount >= 0)),
    CONSTRAINT coupon_template_percent_range CHECK ((((type)::text <> 'PERCENT'::text) OR ((value >= 1) AND (value <= 99)))),
    CONSTRAINT coupon_template_quantity_check CHECK (((total_quantity > 0) AND (issued_quantity >= 0) AND (issued_quantity <= total_quantity))),
    CONSTRAINT coupon_template_scope_check CHECK (((scope)::text = ANY ((ARRAY['ALL'::character varying, 'SKU'::character varying, 'CATEGORY'::character varying, 'BRAND'::character varying])::text[]))),
    CONSTRAINT coupon_template_scope_ids_check CHECK ((((scope)::text = 'ALL'::text) OR (jsonb_array_length(scope_ids) > 0))),
    CONSTRAINT coupon_template_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'ENDED'::character varying, 'DISABLED'::character varying])::text[]))),
    CONSTRAINT coupon_template_type_check CHECK (((type)::text = ANY ((ARRAY['AMOUNT'::character varying, 'PERCENT'::character varying])::text[]))),
    CONSTRAINT coupon_template_validity_positive CHECK ((validity_days > 0)),
    CONSTRAINT coupon_template_value_positive CHECK ((value > 0)),
    CONSTRAINT coupon_template_window_check CHECK ((ends_at > starts_at))
);



COMMENT ON COLUMN public.coupon_template.stackable IS 'NULL 跟随 app.promotion.coupon.mode；TRUE 与促销叠加；FALSE 与促销互斥（取更优）';



CREATE TABLE public.coupon_template_scope (
    template_id uuid NOT NULL,
    scope_type character varying(16) NOT NULL,
    scope_id uuid NOT NULL,
    CONSTRAINT coupon_template_scope_type_check CHECK (((scope_type)::text = ANY ((ARRAY['SKU'::character varying, 'CATEGORY'::character varying, 'BRAND'::character varying])::text[])))
);



CREATE TABLE public.distribution_commission (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    promoter_staff_id uuid NOT NULL,
    order_amount bigint NOT NULL,
    commission_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    status character varying(16) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    settled_at timestamp with time zone,
    frozen_until timestamp with time zone,
    available_at timestamp with time zone,
    reversed_at timestamp with time zone,
    reverse_reason character varying(255),
    level integer DEFAULT 1 NOT NULL,
    rate_mode character varying(16) DEFAULT 'GROSS'::character varying NOT NULL,
    rate_value bigint DEFAULT 5 NOT NULL,
    promoter_type character varying(16) DEFAULT 'STAFF'::character varying NOT NULL,
    consumed_amount bigint DEFAULT 0 NOT NULL,
    payee_channel character varying(16) DEFAULT 'WALLET'::character varying NOT NULL,
    reversed_amount bigint DEFAULT 0 NOT NULL,
    hold_after_sale_id uuid,
    hold_amount bigint DEFAULT 0 NOT NULL,
    CONSTRAINT distribution_commission_consumed_check CHECK (((consumed_amount >= 0) AND (consumed_amount <= commission_amount))),
    CONSTRAINT distribution_commission_level_check CHECK ((level = ANY (ARRAY[1, 2]))),
    CONSTRAINT distribution_commission_payee_channel_check CHECK (((payee_channel)::text = ANY ((ARRAY['WALLET'::character varying, 'PAYROLL'::character varying])::text[]))),
    CONSTRAINT distribution_commission_promoter_type_check CHECK (((promoter_type)::text = ANY ((ARRAY['STAFF'::character varying, 'MEMBER'::character varying])::text[]))),
    CONSTRAINT distribution_commission_rate_mode_check CHECK (((rate_mode)::text = ANY ((ARRAY['GROSS'::character varying, 'FIXED'::character varying])::text[]))),
    CONSTRAINT distribution_commission_reversed_check CHECK (((reversed_amount >= 0) AND ((reversed_amount + consumed_amount) <= commission_amount))),
    CONSTRAINT distribution_commission_status_new_check CHECK (((status)::text = ANY ((ARRAY['FROZEN'::character varying, 'AVAILABLE'::character varying, 'WITHDRAWN'::character varying, 'REVERSED'::character varying])::text[])))
);



CREATE TABLE public.export_job (
    id uuid NOT NULL,
    requester character varying(128) NOT NULL,
    resource character varying(64) NOT NULL,
    projection character varying(32) NOT NULL,
    params jsonb DEFAULT '{}'::jsonb NOT NULL,
    status character varying(16) DEFAULT 'PENDING'::character varying NOT NULL,
    row_count integer,
    file_path character varying(1024),
    error character varying(1024),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    expires_at timestamp with time zone,
    CONSTRAINT export_job_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'RUNNING'::character varying, 'SUCCEEDED'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'EXPIRED'::character varying])::text[])))
);



CREATE TABLE public.export_task (
    id uuid NOT NULL,
    resource character varying(64) NOT NULL,
    scope_kind character varying(16) NOT NULL,
    actor character varying(128) NOT NULL,
    row_count integer NOT NULL,
    columns character varying(512) NOT NULL,
    request_id character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT export_task_row_count_non_negative CHECK ((row_count >= 0))
);



CREATE TABLE public.faq_entry (
    id uuid NOT NULL,
    question character varying(300) NOT NULL,
    answer character varying(2000) NOT NULL,
    keywords text DEFAULT ''::text NOT NULL,
    status character varying(16) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT faq_entry_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);



CREATE TABLE public.faq_miss (
    id uuid NOT NULL,
    question character varying(300) NOT NULL,
    member_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.feature_flag (
    code character varying(64) NOT NULL,
    enabled boolean NOT NULL,
    updated_by character varying(128) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    reason character varying(512) NOT NULL
);



CREATE TABLE public.fulfillment_line (
    id uuid NOT NULL,
    fulfillment_order_id uuid NOT NULL,
    order_line_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    quantity integer NOT NULL
);



CREATE TABLE public.fulfillment_order (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    type character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    source_id uuid,
    carrier_code character varying(32),
    tracking_no character varying(128),
    pickup_code character varying(16),
    pickup_expires_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    supplier_accepted_at timestamp with time zone,
    supplier_note text
);



CREATE TABLE public.goods_receipt (
    id uuid NOT NULL,
    receipt_no character varying(64) NOT NULL,
    location_type character varying(16) NOT NULL,
    location_id uuid NOT NULL,
    supplier_id uuid,
    status character varying(16) NOT NULL,
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    confirmed_at timestamp with time zone
);



CREATE TABLE public.goods_receipt_line (
    id uuid NOT NULL,
    receipt_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    batch_no character varying(64) NOT NULL,
    production_date date,
    expiry_date date,
    quantity integer NOT NULL,
    unit_cost_amount bigint,
    currency character(3),
    CONSTRAINT goods_receipt_line_quantity_positive CHECK ((quantity > 0))
);



CREATE TABLE public.group_buy (
    id uuid NOT NULL,
    code character varying(16) NOT NULL,
    promotion_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    price_amount bigint NOT NULL,
    currency character varying(3) NOT NULL,
    group_size integer NOT NULL,
    creator_phone character varying(32) NOT NULL,
    status character varying(16) DEFAULT 'FORMING'::character varying NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    completed_at timestamp with time zone,
    CONSTRAINT group_buy_status_check CHECK (((status)::text = ANY ((ARRAY['FORMING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying])::text[])))
);



CREATE TABLE public.group_buy_member (
    id uuid NOT NULL,
    group_buy_id uuid NOT NULL,
    order_id uuid NOT NULL,
    phone character varying(32) NOT NULL,
    role character varying(16) NOT NULL,
    joined_at timestamp with time zone DEFAULT now() NOT NULL,
    paid_at timestamp with time zone,
    refunded_at timestamp with time zone,
    CONSTRAINT group_buy_member_role_check CHECK (((role)::text = ANY ((ARRAY['CREATOR'::character varying, 'MEMBER'::character varying])::text[])))
);



CREATE TABLE public.growth_ledger (
    id uuid NOT NULL,
    member_id uuid NOT NULL,
    sale_id uuid,
    entry_type character varying(16) NOT NULL,
    growth bigint NOT NULL,
    balance_after bigint NOT NULL,
    reason character varying(255),
    business_key character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.idempotency_record (
    id uuid NOT NULL,
    scope character varying(32) NOT NULL,
    idempotency_key character varying(128) NOT NULL,
    request_hash character varying(64) NOT NULL,
    response_order_id uuid,
    status character varying(16) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.integration_config (
    capability character varying(32) NOT NULL,
    provider character varying(32) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    config_enc bytea NOT NULL,
    config_hint jsonb DEFAULT '{}'::jsonb NOT NULL,
    updated_by character varying(64),
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.inventory_batch (
    id uuid NOT NULL,
    sku_id uuid NOT NULL,
    location_type character varying(16) NOT NULL,
    location_id uuid NOT NULL,
    batch_no character varying(64) NOT NULL,
    production_date date,
    expiry_date date,
    quantity integer NOT NULL,
    reserved_quantity integer DEFAULT 0 NOT NULL,
    status character varying(16) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    unit_cost_amount bigint,
    unit_cost_currency character(3),
    received_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT inventory_batch_quantity_non_negative CHECK ((quantity >= 0)),
    CONSTRAINT inventory_batch_reserved_non_negative CHECK ((reserved_quantity >= 0)),
    CONSTRAINT inventory_batch_reserved_not_above_quantity CHECK ((reserved_quantity <= quantity))
);



CREATE SEQUENCE public.jv_commit_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE public.jv_commit (
    commit_pk bigint DEFAULT nextval('public.jv_commit_pk_seq'::regclass) NOT NULL,
    author character varying(200),
    commit_date timestamp without time zone,
    commit_date_instant character varying(30),
    commit_id numeric
);



CREATE TABLE public.jv_commit_property (
    commit_fk bigint NOT NULL,
    property_name character varying(191) NOT NULL,
    property_value character varying(600)
);



CREATE SEQUENCE public.jv_global_id_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE public.jv_global_id (
    global_id_pk bigint DEFAULT nextval('public.jv_global_id_pk_seq'::regclass) NOT NULL,
    local_id character varying(191),
    fragment character varying(200),
    type_name character varying(200),
    owner_id_fk bigint
);



CREATE SEQUENCE public.jv_snapshot_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE public.jv_snapshot (
    snapshot_pk bigint DEFAULT nextval('public.jv_snapshot_pk_seq'::regclass) NOT NULL,
    type character varying(200),
    version bigint,
    state text,
    changed_properties text,
    managed_type character varying(200),
    global_id_fk bigint,
    commit_fk bigint
);



CREATE TABLE public.knowledge_chunk (
    id uuid NOT NULL,
    source_type character varying(32) NOT NULL,
    source_id character varying(128) NOT NULL,
    title character varying(300) NOT NULL,
    content text NOT NULL,
    version character varying(64) NOT NULL,
    embedding bytea,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.knowledge_embedding (
    id uuid NOT NULL,
    kind character varying(32) NOT NULL,
    source_id character varying(64) NOT NULL,
    content text NOT NULL,
    embedding double precision[] NOT NULL,
    provider character varying(48) NOT NULL,
    model character varying(96) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.knowledge_sync_state (
    sku_id uuid NOT NULL,
    synced_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.legal_acceptance (
    id uuid NOT NULL,
    user_id uuid,
    document_version_id uuid NOT NULL,
    locale character varying(16) NOT NULL,
    accepted_at timestamp with time zone NOT NULL,
    ip character varying(64),
    user_agent text,
    method character varying(16) NOT NULL,
    evidence_hash character varying(128) NOT NULL,
    order_id uuid,
    session_id character varying(64)
);



CREATE TABLE public.legal_document (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    type character varying(32) NOT NULL,
    market_code character varying(8) NOT NULL,
    legal_entity character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.legal_document_localization (
    id uuid NOT NULL,
    version_id uuid NOT NULL,
    locale character varying(16) NOT NULL,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    content_hash character varying(128) NOT NULL,
    review_status character varying(16) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.legal_document_version (
    id uuid NOT NULL,
    document_id uuid NOT NULL,
    version character varying(32) NOT NULL,
    status character varying(16) NOT NULL,
    effective_from timestamp with time zone NOT NULL,
    effective_to timestamp with time zone,
    content_hash character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.license_state (
    id integer NOT NULL,
    last_seen_at timestamp with time zone NOT NULL,
    license_id character varying(64),
    customer character varying(200),
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.market (
    code character varying(8) NOT NULL,
    name character varying(64) NOT NULL,
    currency character(3) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    country_code character(2),
    tax_jurisdiction character varying(32),
    price_includes_tax boolean,
    timezone character varying(64),
    legal_entity_name character varying(128),
    sort integer DEFAULT 0 NOT NULL
);



COMMENT ON COLUMN public.market.tax_jurisdiction IS '税务辖区代码，可≠country_code（如 EU-OSS 跨国申报）；税率表按此挂载';



COMMENT ON COLUMN public.market.price_includes_tax IS 'TRUE=标价含税（大陆习惯）；FALSE=净价另计（美加习惯）';



CREATE TABLE public.market_destination (
    market_code character varying(8) NOT NULL,
    destination_country character(2) NOT NULL,
    deliverable boolean DEFAULT true NOT NULL
);



CREATE TABLE public.market_integration (
    market_code character varying(8) NOT NULL,
    capability character varying(32) NOT NULL,
    provider character varying(32) NOT NULL,
    enabled boolean DEFAULT false NOT NULL
);



CREATE TABLE public.market_locale (
    market_code character varying(8) NOT NULL,
    locale character varying(16) NOT NULL,
    is_primary boolean DEFAULT false NOT NULL
);



COMMENT ON COLUMN public.market_locale.is_primary IS '该市场的默认语言；用户实际界面语言是独立维度（ADR-0014）';



CREATE TABLE public.market_setting (
    market_code character varying(8) NOT NULL,
    setting_key character varying(64) NOT NULL,
    setting_value text DEFAULT ''::text NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.member (
    id uuid NOT NULL,
    member_no character varying(32) NOT NULL,
    name character varying(64) NOT NULL,
    phone character varying(32) NOT NULL,
    tier character varying(16) DEFAULT 'STANDARD'::character varying NOT NULL,
    points_balance bigint DEFAULT 0 NOT NULL,
    stored_value_balance bigint DEFAULT 0 NOT NULL,
    currency character(3) DEFAULT 'CNY'::bpchar NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    birthday date,
    growth_value bigint DEFAULT 0 NOT NULL,
    birthday_gift_year integer,
    timezone character varying(64) DEFAULT NULL::character varying,
    email character varying(254),
    CONSTRAINT member_points_non_negative CHECK ((points_balance >= 0)),
    CONSTRAINT member_stored_value_non_negative CHECK ((stored_value_balance >= 0))
);



CREATE TABLE public.member_address (
    id uuid NOT NULL,
    owner_key character varying(128) NOT NULL,
    recipient character varying(64) NOT NULL,
    phone character varying(32) NOT NULL,
    region_code character varying(16),
    province character varying(64),
    city character varying(64),
    district character varying(64),
    detail character varying(255) NOT NULL,
    postal_code character varying(16),
    is_default boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    country_code character(2)
);



COMMENT ON COLUMN public.member_address.country_code IS '收货地国家（ISO 3166-1）；决定地址格式/运费/税/报关，不是市场';



CREATE TABLE public.member_browse_history (
    member_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    viewed_at timestamp with time zone DEFAULT now() NOT NULL,
    view_count integer DEFAULT 1 NOT NULL
);



CREATE TABLE public.member_card_ledger (
    id uuid NOT NULL,
    member_id uuid NOT NULL,
    sale_id uuid,
    entry_type character varying(16) NOT NULL,
    amount_amount bigint NOT NULL,
    balance_after_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    reason character varying(255),
    business_key character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.member_coupon (
    id uuid NOT NULL,
    template_id uuid NOT NULL,
    member_id uuid NOT NULL,
    code character varying(32) NOT NULL,
    status character varying(16) DEFAULT 'UNUSED'::character varying NOT NULL,
    issued_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    order_id uuid,
    CONSTRAINT member_coupon_status_check CHECK (((status)::text = ANY ((ARRAY['UNUSED'::character varying, 'USED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT member_coupon_used_at_check CHECK ((((status)::text = 'USED'::text) = (used_at IS NOT NULL)))
);



CREATE TABLE public.member_favorite (
    member_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.member_level (
    code character varying(16) NOT NULL,
    name character varying(64) NOT NULL,
    discount_rate integer NOT NULL,
    points_multiplier integer DEFAULT 1 NOT NULL,
    free_shipping boolean DEFAULT false NOT NULL,
    birthday_points bigint DEFAULT 0 NOT NULL,
    min_growth bigint DEFAULT 0 NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    CONSTRAINT member_level_discount_valid CHECK (((discount_rate >= 1) AND (discount_rate <= 100)))
);



CREATE TABLE public.member_otp (
    id uuid NOT NULL,
    phone character varying(32),
    code_hash character varying(255) NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    consumed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    email character varying(254),
    CONSTRAINT member_otp_identifier_check CHECK (((phone IS NOT NULL) OR (email IS NOT NULL)))
);



CREATE TABLE public.member_session (
    token_hash character varying(64) NOT NULL,
    member_id uuid NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.notification (
    id uuid NOT NULL,
    recipient_type character varying(16) NOT NULL,
    recipient_id character varying(128) NOT NULL,
    event_type character varying(64) NOT NULL,
    channel character varying(16) NOT NULL,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    status character varying(16) NOT NULL,
    read_at timestamp with time zone,
    sent_at timestamp with time zone,
    error_message character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.order_coupon_allocation (
    order_id uuid NOT NULL,
    order_line_id uuid NOT NULL,
    coupon_id uuid NOT NULL,
    amount bigint NOT NULL,
    CONSTRAINT order_coupon_allocation_amount_check CHECK ((amount >= 0))
);



CREATE TABLE public.order_line (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    title_snapshot character varying(255) NOT NULL,
    sku_code_snapshot character varying(64) NOT NULL,
    quantity integer NOT NULL,
    unit_price_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    fulfillment_type character varying(32) NOT NULL,
    promotion_id uuid,
    line_type character varying(16) DEFAULT 'NORMAL'::character varying NOT NULL,
    snapshot jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    batch_id uuid,
    CONSTRAINT order_line_quantity_positive CHECK ((quantity > 0)),
    CONSTRAINT order_line_type_check CHECK (((line_type)::text = ANY ((ARRAY['NORMAL'::character varying, 'PROMOTION'::character varying, 'GIFT'::character varying])::text[])))
);



COMMENT ON COLUMN public.order_line.snapshot IS '下单时的商品快照（服务端生成，用户不可输入）：image/attributes/brand/category/listPrice/cost/locale 等，字段可增不改表';



CREATE TABLE public.order_line_refund (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    order_line_id uuid NOT NULL,
    quantity integer,
    amount_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    reason character varying(255),
    refunded_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT order_line_refund_amount_positive CHECK ((amount_amount > 0)),
    CONSTRAINT order_line_refund_quantity_check CHECK (((quantity IS NULL) OR (quantity > 0)))
);



CREATE TABLE public.order_tax_line (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    jurisdiction character varying(32) NOT NULL,
    name character varying(64) NOT NULL,
    rate_bps integer NOT NULL,
    price_includes_tax boolean NOT NULL,
    taxable_amount bigint NOT NULL,
    tax_amount bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



COMMENT ON TABLE public.order_tax_line IS '订单税行：下单时快照税率与税基，事后改税率不影响历史订单';



CREATE TABLE public.orders (
    id uuid NOT NULL,
    order_no character varying(64) NOT NULL,
    status character varying(32) NOT NULL,
    currency character(3) NOT NULL,
    subtotal_amount bigint NOT NULL,
    shipping_fee_amount bigint DEFAULT 0 NOT NULL,
    discount_amount bigint DEFAULT 0 NOT NULL,
    total_amount bigint NOT NULL,
    contact_name character varying(64) NOT NULL,
    contact_phone character varying(32) NOT NULL,
    contact_email character varying(128),
    address_json jsonb,
    fulfillment_mode character varying(16) NOT NULL,
    store_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    referrer_staff_id uuid,
    referral_mode character varying(16),
    locale character varying(16),
    member_id uuid,
    placed_by character varying(16) DEFAULT 'CUSTOMER'::character varying NOT NULL,
    placed_by_operator character varying(128),
    fulfilled_at timestamp with time zone,
    access_token_hash character varying(64),
    market_code character varying(8),
    total_weight_grams integer,
    CONSTRAINT orders_operator_required_check CHECK ((((placed_by)::text = 'CUSTOMER'::text) OR (placed_by_operator IS NOT NULL))),
    CONSTRAINT orders_placed_by_check CHECK (((placed_by)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'CASHIER'::character varying, 'ADMIN'::character varying])::text[])))
);



COMMENT ON COLUMN public.orders.market_code IS '下单时的市场快照；历史行回填 CN 是一次性迁移假设';



COMMENT ON COLUMN public.orders.total_weight_grams IS '下单时按 SKU 净重汇总的重量快照；缺重量数据的商品按 0 计并可在报关单里看到警告';



CREATE TABLE public.payment (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    provider character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    amount_amount bigint NOT NULL,
    amount_currency character(3) NOT NULL,
    provider_transaction_id character varying(128),
    paid_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    config_version_id uuid,
    channel character varying(32),
    mode character varying(16),
    sp_mch_id character varying(64),
    sub_mch_id character varying(64),
    refunded_amount bigint DEFAULT 0 NOT NULL,
    CONSTRAINT payment_refunded_check CHECK (((refunded_amount >= 0) AND (refunded_amount <= amount_amount)))
);



COMMENT ON COLUMN public.payment.config_version_id IS '下单时使用的支付配置版本；退款必须按此路由，不能按当前配置';



CREATE TABLE public.payment_event (
    id uuid NOT NULL,
    payment_id uuid NOT NULL,
    event_type character varying(64) NOT NULL,
    payload_json jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.payment_provider_config (
    id uuid NOT NULL,
    channel character varying(32) NOT NULL,
    mode character varying(16) NOT NULL,
    market_code character varying(16) NOT NULL,
    display_name character varying(128) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    current_version integer DEFAULT 1 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT payment_provider_config_mode_check CHECK (((mode)::text = ANY ((ARRAY['DIRECT'::character varying, 'SUB_MERCHANT'::character varying])::text[])))
);



CREATE TABLE public.payment_provider_config_version (
    id uuid NOT NULL,
    config_id uuid NOT NULL,
    version_no integer NOT NULL,
    channel character varying(32) NOT NULL,
    mode character varying(16) NOT NULL,
    created_by character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    settings jsonb,
    CONSTRAINT payment_provider_config_version_mode_check CHECK (((mode)::text = ANY ((ARRAY['DIRECT'::character varying, 'SUB_MERCHANT'::character varying])::text[])))
);



COMMENT ON COLUMN public.payment_provider_config_version.settings IS '渠道配置（服务商/商户号/AppID/回调等在 JSON 中；新增渠道参数不改表）';



CREATE TABLE public.payment_reconciliation (
    id uuid NOT NULL,
    bill_date date NOT NULL,
    order_no character varying(64) NOT NULL,
    kind character varying(24) NOT NULL,
    bill_fen bigint,
    local_fen bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT payment_reconciliation_kind_check CHECK (((kind)::text = ANY ((ARRAY['MATCHED'::character varying, 'BILL_ONLY'::character varying, 'LOCAL_ONLY'::character varying, 'AMOUNT_MISMATCH'::character varying])::text[])))
);



CREATE TABLE public.permission (
    code character varying(64) NOT NULL,
    label character varying(128) NOT NULL,
    group_name character varying(64) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);



CREATE TABLE public.pick_line_order_line (
    pick_line_id uuid NOT NULL,
    order_line_id uuid NOT NULL,
    quantity integer NOT NULL,
    CONSTRAINT pick_line_order_line_quantity_check CHECK ((quantity > 0))
);



CREATE TABLE public.pick_task (
    id uuid NOT NULL,
    fulfillment_order_id uuid NOT NULL,
    store_id uuid,
    status character varying(16) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.pick_task_line (
    id uuid NOT NULL,
    pick_task_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    quantity integer NOT NULL,
    suggested_batch_id uuid,
    suggested_expiry_date date,
    picked_quantity integer DEFAULT 0 NOT NULL,
    order_line_id uuid,
    CONSTRAINT pick_task_line_quantity_positive CHECK ((quantity > 0))
);



CREATE TABLE public.pickup (
    id uuid NOT NULL,
    fulfillment_order_id uuid NOT NULL,
    store_id uuid NOT NULL,
    pickup_code character varying(16) NOT NULL,
    status character varying(16) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    completed_at timestamp with time zone,
    contact_phone character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    total_uses integer DEFAULT 1 NOT NULL,
    used_count integer DEFAULT 0 NOT NULL,
    CONSTRAINT pickup_total_uses_positive CHECK ((total_uses > 0)),
    CONSTRAINT pickup_used_count_range CHECK (((used_count >= 0) AND (used_count <= total_uses)))
);



CREATE TABLE public.points_ledger (
    id uuid NOT NULL,
    member_id uuid NOT NULL,
    sale_id uuid,
    entry_type character varying(16) NOT NULL,
    points bigint NOT NULL,
    balance_after bigint NOT NULL,
    reason character varying(255),
    business_key character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    remaining_points bigint DEFAULT 0 NOT NULL
);



CREATE TABLE public.pos_cash_movement (
    id uuid NOT NULL,
    shift_id uuid NOT NULL,
    sale_id uuid,
    movement_type character varying(24) NOT NULL,
    amount_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    reason character varying(255),
    operator_name character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.pos_reconciliation (
    id uuid NOT NULL,
    store_id uuid NOT NULL,
    business_date date NOT NULL,
    tender_type character varying(16) NOT NULL,
    expected_amount bigint NOT NULL,
    actual_amount bigint NOT NULL,
    variance_amount bigint NOT NULL,
    status character varying(16) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.pos_refund (
    id uuid NOT NULL,
    refund_no character varying(64) NOT NULL,
    sale_id uuid NOT NULL,
    amount_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    reason character varying(255) NOT NULL,
    operator_name character varying(64) NOT NULL,
    status character varying(16) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    refund_type character varying(16) DEFAULT 'FULL'::character varying NOT NULL
);



CREATE TABLE public.pos_refund_allocation (
    id uuid NOT NULL,
    refund_line_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    quantity integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pos_refund_allocation_quantity_positive CHECK ((quantity > 0))
);



CREATE TABLE public.pos_refund_line (
    id uuid NOT NULL,
    refund_id uuid NOT NULL,
    sale_line_id uuid NOT NULL,
    quantity integer NOT NULL,
    amount_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pos_refund_line_amount_positive CHECK ((amount_amount >= 0)),
    CONSTRAINT pos_refund_line_quantity_positive CHECK ((quantity > 0))
);



CREATE TABLE public.pos_refund_tender (
    id uuid NOT NULL,
    refund_id uuid NOT NULL,
    tender_id uuid,
    tender_type character varying(16) NOT NULL,
    amount_amount bigint NOT NULL,
    provider character varying(32),
    provider_trade_no character varying(128),
    status character varying(16) NOT NULL
);



CREATE TABLE public.pos_sale (
    id uuid NOT NULL,
    sale_no character varying(64) NOT NULL,
    store_id uuid NOT NULL,
    shift_id uuid NOT NULL,
    member_id uuid,
    currency character(3) NOT NULL,
    subtotal_amount bigint NOT NULL,
    promotion_discount_amount bigint DEFAULT 0 NOT NULL,
    coupon_discount_amount bigint DEFAULT 0 NOT NULL,
    member_discount_amount bigint DEFAULT 0 NOT NULL,
    points_discount_amount bigint DEFAULT 0 NOT NULL,
    payable_amount bigint NOT NULL,
    cash_tendered_amount bigint DEFAULT 0 NOT NULL,
    change_amount bigint DEFAULT 0 NOT NULL,
    status character varying(32) NOT NULL,
    coupon_id uuid,
    points_redeemed bigint DEFAULT 0 NOT NULL,
    points_earned bigint DEFAULT 0 NOT NULL,
    refunded_amount bigint DEFAULT 0 NOT NULL,
    note text,
    idempotency_key character varying(128),
    request_hash character varying(64),
    operator_name character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    paid_at timestamp with time zone,
    voided_at timestamp with time zone,
    points_reversed bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pos_sale_payable_non_negative CHECK ((payable_amount >= 0))
);



CREATE TABLE public.pos_sale_allocation (
    id uuid NOT NULL,
    sale_line_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    quantity integer NOT NULL,
    unit_cost_amount bigint,
    currency character(3),
    refunded_quantity integer DEFAULT 0 NOT NULL,
    CONSTRAINT pos_sale_allocation_quantity_positive CHECK ((quantity > 0)),
    CONSTRAINT pos_sale_allocation_refunded_quantity_valid CHECK (((refunded_quantity >= 0) AND (refunded_quantity <= quantity)))
);



CREATE TABLE public.pos_sale_line (
    id uuid NOT NULL,
    sale_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    title_snapshot character varying(255) NOT NULL,
    sku_code_snapshot character varying(64) NOT NULL,
    quantity integer NOT NULL,
    unit_price_amount bigint NOT NULL,
    line_total_amount bigint NOT NULL,
    refunded_quantity integer DEFAULT 0 NOT NULL,
    currency character(3) NOT NULL,
    refunded_amount bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pos_sale_line_quantity_positive CHECK ((quantity > 0)),
    CONSTRAINT pos_sale_line_refunded_amount_valid CHECK (((refunded_amount >= 0) AND (refunded_amount <= line_total_amount))),
    CONSTRAINT pos_sale_line_refunded_quantity_valid CHECK (((refunded_quantity >= 0) AND (refunded_quantity <= quantity)))
);



CREATE TABLE public.pos_shift (
    id uuid NOT NULL,
    store_id uuid NOT NULL,
    cashier_id uuid,
    cashier_name character varying(64) NOT NULL,
    status character varying(16) NOT NULL,
    currency character(3) NOT NULL,
    opening_float_amount bigint DEFAULT 0 NOT NULL,
    expected_cash_amount bigint,
    actual_cash_amount bigint,
    variance_amount bigint,
    variance_reason text,
    opened_at timestamp with time zone DEFAULT now() NOT NULL,
    closed_at timestamp with time zone,
    approved_by uuid,
    approved_at timestamp with time zone
);



CREATE TABLE public.pos_tender (
    id uuid NOT NULL,
    sale_id uuid NOT NULL,
    tender_type character varying(16) NOT NULL,
    amount_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    provider character varying(32) NOT NULL,
    provider_trade_no character varying(128),
    status character varying(16) NOT NULL,
    cash_tendered_amount bigint,
    change_amount bigint,
    idempotency_key character varying(128),
    paid_at timestamp with time zone,
    reversal_status character varying(16),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    refunded_amount bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pos_tender_amount_positive CHECK ((amount_amount > 0)),
    CONSTRAINT pos_tender_refunded_amount_valid CHECK (((refunded_amount >= 0) AND (refunded_amount <= amount_amount)))
);



CREATE TABLE public.product (
    id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    category_id uuid,
    brand_id uuid,
    status character varying(32) NOT NULL,
    inventory_tracking_level character varying(16) DEFAULT 'VARIANT'::character varying NOT NULL,
    shelf_life_managed boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    sku_matrix_template_id uuid,
    detail_html text
);



CREATE TABLE public.product_media (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    sku_id uuid,
    asset_id uuid NOT NULL,
    role character varying(16) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    source character varying(16) DEFAULT 'UPLOAD'::character varying NOT NULL,
    created_by character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT product_media_role_check CHECK (((role)::text = ANY ((ARRAY['MAIN'::character varying, 'GALLERY'::character varying, 'DETAIL'::character varying, 'VIDEO'::character varying, 'SPEC'::character varying])::text[]))),
    CONSTRAINT product_media_source_check CHECK (((source)::text = ANY ((ARRAY['UPLOAD'::character varying, 'AI'::character varying])::text[])))
);



CREATE TABLE public.product_option (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    name character varying(64) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);



CREATE TABLE public.product_option_value (
    id uuid NOT NULL,
    option_id uuid NOT NULL,
    name character varying(64) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);



CREATE TABLE public.product_review (
    id uuid NOT NULL,
    sku_id uuid NOT NULL,
    member_id uuid NOT NULL,
    order_id uuid NOT NULL,
    rating integer NOT NULL,
    content character varying(1000),
    status character varying(16) DEFAULT 'PUBLISHED'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    media_urls text[] DEFAULT '{}'::text[] NOT NULL,
    CONSTRAINT product_review_media_urls_check CHECK ((cardinality(media_urls) <= 9)),
    CONSTRAINT product_review_rating_check CHECK (((rating >= 1) AND (rating <= 5))),
    CONSTRAINT product_review_status_check CHECK (((status)::text = ANY ((ARRAY['PUBLISHED'::character varying, 'HIDDEN'::character varying, 'PENDING'::character varying])::text[])))
);



CREATE TABLE public.product_translation (
    product_id uuid NOT NULL,
    locale character varying(16) NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    slug character varying(255),
    seo_title character varying(255),
    seo_description text,
    status character varying(16) DEFAULT 'PUBLISHED'::character varying NOT NULL,
    source_hash character varying(64),
    reviewed_by character varying(128),
    reviewed_at timestamp with time zone,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    image_url character varying(1024),
    detail_html text,
    CONSTRAINT product_translation_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'MACHINE'::character varying, 'REVIEWED'::character varying, 'PUBLISHED'::character varying, 'OUTDATED'::character varying])::text[])))
);



CREATE TABLE public.promoter_link (
    promoter_type character varying(16) NOT NULL,
    promoter_id uuid NOT NULL,
    upline_type character varying(16) NOT NULL,
    upline_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT promoter_link_type_check CHECK ((((promoter_type)::text = ANY ((ARRAY['STAFF'::character varying, 'MEMBER'::character varying])::text[])) AND ((upline_type)::text = ANY ((ARRAY['STAFF'::character varying, 'MEMBER'::character varying])::text[]))))
);



CREATE TABLE public.promotion (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    type character varying(24) NOT NULL,
    title character varying(200) NOT NULL,
    description character varying(1000),
    status character varying(16) DEFAULT 'DRAFT'::character varying NOT NULL,
    starts_at timestamp with time zone NOT NULL,
    ends_at timestamp with time zone NOT NULL,
    sold_quantity integer DEFAULT 0 NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    rules jsonb,
    sku_id uuid GENERATED ALWAYS AS (((rules ->> 'skuId'::text))::uuid) STORED,
    promo_price_amount bigint GENERATED ALWAYS AS (((rules ->> 'promoPriceAmount'::text))::bigint) STORED,
    gift_sku_id uuid GENERATED ALWAYS AS (((rules ->> 'giftSkuId'::text))::uuid) STORED,
    buy_quantity integer GENERATED ALWAYS AS (((rules ->> 'buyQuantity'::text))::integer) STORED,
    gift_quantity integer GENERATED ALWAYS AS (((rules ->> 'giftQuantity'::text))::integer) STORED,
    max_gift_per_order integer GENERATED ALWAYS AS (((rules ->> 'maxGiftPerOrder'::text))::integer) STORED,
    stock_limit integer GENERATED ALWAYS AS (((rules ->> 'stockLimit'::text))::integer) STORED,
    per_user_limit integer GENERATED ALWAYS AS (((rules ->> 'perUserLimit'::text))::integer) STORED,
    group_size integer GENERATED ALWAYS AS (((rules ->> 'groupSize'::text))::integer) STORED,
    group_duration_hours integer GENERATED ALWAYS AS (((rules ->> 'groupDurationHours'::text))::integer) STORED,
    threshold_amount bigint,
    rule_value bigint,
    channel character varying(16),
    exclusive_group character varying(64),
    CONSTRAINT promotion_flash_rules_check CHECK ((((type)::text <> 'FLASH_SALE'::text) OR (((rules ->> 'skuId'::text) IS NOT NULL) AND (((rules ->> 'promoPriceAmount'::text))::bigint > 0) AND (((rules ->> 'stockLimit'::text))::integer > 0)))),
    CONSTRAINT promotion_gift_rules_check CHECK ((((type)::text <> 'BUY_GIFT'::text) OR (((rules ->> 'giftSkuId'::text) IS NOT NULL) AND (((rules ->> 'buyQuantity'::text))::integer > 0) AND (((rules ->> 'giftQuantity'::text))::integer > 0)))),
    CONSTRAINT promotion_group_rules_check CHECK ((((type)::text <> 'GROUP_BUY'::text) OR ((((rules ->> 'promoPriceAmount'::text))::bigint > 0) AND (((rules ->> 'groupSize'::text))::integer >= 2) AND (((rules ->> 'groupDurationHours'::text))::integer > 0)))),
    CONSTRAINT promotion_rules_required CHECK ((rules IS NOT NULL)),
    CONSTRAINT promotion_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'ENDED'::character varying])::text[]))),
    CONSTRAINT promotion_type_check CHECK (((type)::text = ANY ((ARRAY['FLASH_SALE'::character varying, 'BUY_GIFT'::character varying, 'GROUP_BUY'::character varying, 'FULL_REDUCTION'::character varying, 'FULL_DISCOUNT'::character varying])::text[]))),
    CONSTRAINT promotion_window_check CHECK ((ends_at > starts_at))
);



COMMENT ON COLUMN public.promotion.rules IS '促销类型专属配置（服务端生成）；旧列为其生成列，新增促销类型无需改表';



COMMENT ON COLUMN public.promotion.threshold_amount IS '门槛（分）；由 rules.thresholdAmount 投影，写入时同步';



COMMENT ON COLUMN public.promotion.rule_value IS '满减额或折扣百分比；由 rules.value 投影';



CREATE TABLE public.promotion_ladder (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    kind character varying(16) NOT NULL,
    threshold_fen bigint DEFAULT 0 NOT NULL,
    gift_sku_id uuid,
    gift_quantity integer DEFAULT 0 NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    starts_at timestamp with time zone DEFAULT now() NOT NULL,
    ends_at timestamp with time zone,
    CONSTRAINT promotion_ladder_gift_check CHECK ((((kind)::text <> 'GIFT'::text) OR ((gift_sku_id IS NOT NULL) AND (gift_quantity > 0)))),
    CONSTRAINT promotion_ladder_kind_check CHECK (((kind)::text = ANY ((ARRAY['GIFT'::character varying, 'SECOND_HALF'::character varying])::text[])))
);



CREATE TABLE public.promotion_redemption (
    id uuid NOT NULL,
    promotion_id uuid NOT NULL,
    order_id uuid NOT NULL,
    member_phone character varying(32) NOT NULL,
    sale_quantity integer DEFAULT 0 NOT NULL,
    gift_quantity integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    released_at timestamp with time zone
);



CREATE TABLE public.purchase_order (
    id uuid NOT NULL,
    order_no character varying(64) NOT NULL,
    supplier_id uuid,
    status character varying(16) NOT NULL,
    currency character(3) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    received_at timestamp with time zone,
    CONSTRAINT purchase_order_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PLACED'::character varying, 'RECEIVED'::character varying, 'CANCELLED'::character varying])::text[])))
);



CREATE TABLE public.purchase_order_line (
    id uuid NOT NULL,
    purchase_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    quantity bigint NOT NULL,
    unit_cost bigint NOT NULL,
    received_qty bigint DEFAULT 0 NOT NULL,
    CONSTRAINT purchase_line_cost_check CHECK ((unit_cost >= 0)),
    CONSTRAINT purchase_line_quantity_check CHECK ((quantity > 0)),
    CONSTRAINT purchase_line_received_check CHECK (((received_qty >= 0) AND (received_qty <= quantity)))
);



CREATE TABLE public.referral_binding (
    id uuid NOT NULL,
    customer_phone character varying(32) NOT NULL,
    promoter_staff_id uuid NOT NULL,
    mode character varying(16) NOT NULL,
    source character varying(32),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone
);



CREATE TABLE public.region (
    code character varying(16) NOT NULL,
    parent_code character varying(16),
    level character varying(16) NOT NULL,
    depth smallint NOT NULL,
    country_code character(2) NOT NULL,
    names jsonb NOT NULL,
    pinyin character varying(128),
    active boolean DEFAULT true NOT NULL,
    source_id character varying(32) NOT NULL,
    source_version character varying(32) NOT NULL,
    CONSTRAINT region_depth_check CHECK (((depth >= 1) AND (depth <= 4))),
    CONSTRAINT region_level_check CHECK (((level)::text = ANY ((ARRAY['COUNTRY'::character varying, 'ADMIN'::character varying, 'LOCALITY'::character varying, 'DEPENDENT'::character varying])::text[]))),
    CONSTRAINT region_root_check CHECK ((((level)::text = 'COUNTRY'::text) = (parent_code IS NULL)))
);



COMMENT ON COLUMN public.region.active IS 'FALSE=已撤销/并入其他区划，仍可被历史数据引用；列表与级联不再返回';



CREATE TABLE public.reservation (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    order_line_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    quantity integer NOT NULL,
    status character varying(16) NOT NULL,
    expires_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT reservation_quantity_positive CHECK ((quantity > 0))
);



CREATE TABLE public.risk_alert (
    id uuid NOT NULL,
    rule_code character varying(64) NOT NULL,
    severity character varying(16) NOT NULL,
    subject character varying(128) NOT NULL,
    detail character varying(1024),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    acknowledged_at timestamp with time zone,
    CONSTRAINT risk_alert_severity_check CHECK (((severity)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[])))
);



CREATE TABLE public.role_permission (
    role character varying(32) NOT NULL,
    permission_code character varying(64) NOT NULL
);



CREATE TABLE public.scheduled_task (
    code character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    cron character varying(64) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    last_run_at timestamp with time zone,
    last_status character varying(16),
    last_error character varying(500),
    last_duration_ms bigint,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT scheduled_task_last_status_check CHECK (((last_status IS NULL) OR ((last_status)::text = ANY ((ARRAY['SUCCESS'::character varying, 'FAILED'::character varying, 'SKIPPED'::character varying])::text[])))),
    CONSTRAINT scheduled_task_status_check CHECK (((last_status IS NULL) OR ((last_status)::text = ANY ((ARRAY['SUCCESS'::character varying, 'FAILED'::character varying, 'SKIPPED'::character varying, 'RUNNING'::character varying])::text[]))))
);



CREATE TABLE public.scheduled_task_lease (
    code character varying(64) NOT NULL,
    holder character varying(128) NOT NULL,
    acquired_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone NOT NULL
);



CREATE TABLE public.shipping_rate (
    id uuid NOT NULL,
    market_code character varying(8) NOT NULL,
    destination_country character(2),
    min_weight_grams integer DEFAULT 0 NOT NULL,
    max_weight_grams integer,
    fee_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    free_over_amount bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT shipping_rate_band_check CHECK (((max_weight_grams IS NULL) OR (max_weight_grams > min_weight_grams))),
    CONSTRAINT shipping_rate_fee_amount_check CHECK ((fee_amount >= 0)),
    CONSTRAINT shipping_rate_min_weight_grams_check CHECK ((min_weight_grams >= 0))
);



COMMENT ON TABLE public.shipping_rate IS '运费阶梯：按 市场 × 目的地 × 重量档 计费；dest=market 国家且无规则时按"国内默认免运费"处理，跨国无规则则明确报错';



CREATE TABLE public.sku (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    sku_code character varying(64) NOT NULL,
    barcode character varying(64),
    price_amount bigint NOT NULL,
    price_currency character(3) NOT NULL,
    compare_at_amount bigint,
    compare_at_currency character(3),
    options_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    image_url text,
    shelf_life_policy_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    cost_amount bigint,
    combination_key character varying(255),
    shelf_batch_tracking boolean DEFAULT false NOT NULL,
    shelf_picking_strategy character varying(24) DEFAULT 'NONE'::character varying NOT NULL,
    shelf_min_remaining_days integer DEFAULT 0 NOT NULL,
    shelf_near_expiry_days integer DEFAULT 0 NOT NULL,
    shelf_near_expiry_action character varying(24) DEFAULT 'NORMAL'::character varying NOT NULL,
    shelf_auto_block_expired boolean DEFAULT true NOT NULL,
    hs_code character varying(16),
    origin_country character(2),
    net_weight_grams integer,
    gross_weight_grams integer,
    CONSTRAINT sku_cost_non_negative CHECK (((cost_amount IS NULL) OR (cost_amount >= 0))),
    CONSTRAINT sku_price_positive CHECK ((price_amount > 0)),
    CONSTRAINT sku_shelf_days_check CHECK (((shelf_min_remaining_days >= 0) AND (shelf_near_expiry_days >= 0))),
    CONSTRAINT sku_weight_non_negative CHECK ((((net_weight_grams IS NULL) OR (net_weight_grams >= 0)) AND ((gross_weight_grams IS NULL) OR (gross_weight_grams >= 0))))
);



COMMENT ON COLUMN public.sku.hs_code IS 'HS 编码（报关用）；为空时报关单导出会明确标注"缺失"而不是留空行';



COMMENT ON COLUMN public.sku.origin_country IS '原产地（ISO 3166-1 alpha-2），跨境报关必填项之一';



CREATE TABLE public.sku_cost (
    sku_id uuid NOT NULL,
    quantity bigint DEFAULT 0 NOT NULL,
    amount bigint DEFAULT 0 NOT NULL,
    avg_cost_amount bigint DEFAULT 0 NOT NULL,
    stale boolean DEFAULT false NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT sku_cost_non_negative CHECK (((quantity >= 0) AND (amount >= 0) AND (avg_cost_amount >= 0)))
);



CREATE TABLE public.sku_matrix_dimension (
    id uuid NOT NULL,
    template_id uuid NOT NULL,
    name character varying(64) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);



CREATE TABLE public.sku_matrix_dimension_value (
    id uuid NOT NULL,
    dimension_id uuid NOT NULL,
    name character varying(64) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);



CREATE TABLE public.sku_matrix_disabled (
    template_id uuid NOT NULL,
    combination_json jsonb NOT NULL
);



CREATE TABLE public.sku_matrix_template (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.sku_option_value (
    sku_id uuid NOT NULL,
    option_id uuid NOT NULL,
    option_value_id uuid NOT NULL
);



CREATE TABLE public.sku_price (
    sku_id uuid NOT NULL,
    market_code character varying(8) NOT NULL,
    currency character(3) NOT NULL,
    amount bigint NOT NULL,
    compare_at_amount bigint,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT sku_price_amount_check CHECK ((amount > 0))
);



CREATE TABLE public.sku_serial (
    id uuid NOT NULL,
    sku_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    serial_no character varying(64) NOT NULL,
    status character varying(16) DEFAULT 'IN_STOCK'::character varying NOT NULL,
    order_line_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT sku_serial_status_check CHECK (((status)::text = ANY ((ARRAY['IN_STOCK'::character varying, 'RESERVED'::character varying, 'SOLD'::character varying, 'RETURNED'::character varying, 'SCRAPPED'::character varying])::text[])))
);



CREATE TABLE public.staff_session (
    token_hash character varying(64) NOT NULL,
    staff_id uuid NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.staff_store (
    staff_id uuid NOT NULL,
    store_id uuid NOT NULL,
    assigned_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.role (
    code character varying(32) NOT NULL,
    label character varying(64) NOT NULL,
    editable boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (code);

CREATE TABLE public.staff_user (
    id uuid NOT NULL,
    username character varying(64) NOT NULL,
    password_hash character varying(255) NOT NULL,
    display_name character varying(64) NOT NULL,
    role character varying(32) NOT NULL,
    status character varying(16) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    pin_hash character varying(255)
);



CREATE TABLE public.stock_cost_movement (
    id uuid NOT NULL,
    sku_id uuid NOT NULL,
    movement_type character varying(16) NOT NULL,
    quantity bigint NOT NULL,
    unit_cost bigint NOT NULL,
    amount bigint NOT NULL,
    reference_type character varying(32),
    reference_id character varying(64),
    occurred_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT stock_cost_movement_quantity_check CHECK ((quantity <> 0)),
    CONSTRAINT stock_cost_movement_type_check CHECK (((movement_type)::text = ANY ((ARRAY['RECEIPT'::character varying, 'ISSUE'::character varying, 'RETURN'::character varying, 'ADJUST'::character varying, 'WRITE_OFF'::character varying])::text[])))
);



CREATE TABLE public.stock_move (
    id uuid NOT NULL,
    batch_id uuid NOT NULL,
    sku_id uuid,
    location_type character varying(16),
    location_id uuid,
    move_type character varying(32) NOT NULL,
    quantity integer NOT NULL,
    reference_type character varying(32),
    reference_id uuid,
    occurred_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.stock_transfer (
    id uuid NOT NULL,
    transfer_no character varying(64) NOT NULL,
    from_type character varying(16) NOT NULL,
    from_id uuid NOT NULL,
    to_type character varying(16) NOT NULL,
    to_id uuid NOT NULL,
    status character varying(16) NOT NULL,
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    shipped_at timestamp with time zone,
    received_at timestamp with time zone
);



CREATE TABLE public.stock_transfer_allocation (
    id uuid NOT NULL,
    transfer_line_id uuid NOT NULL,
    source_batch_id uuid NOT NULL,
    batch_no character varying(64) NOT NULL,
    production_date date,
    expiry_date date,
    quantity integer NOT NULL,
    unit_cost_amount bigint,
    currency character(3),
    CONSTRAINT stock_transfer_allocation_quantity_positive CHECK ((quantity > 0))
);



CREATE TABLE public.stock_transfer_line (
    id uuid NOT NULL,
    transfer_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    quantity integer NOT NULL,
    CONSTRAINT stock_transfer_line_quantity_positive CHECK ((quantity > 0))
);



CREATE TABLE public.stocktake (
    id uuid NOT NULL,
    stocktake_no character varying(64) NOT NULL,
    location_type character varying(16) NOT NULL,
    location_id uuid NOT NULL,
    status character varying(16) NOT NULL,
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    confirmed_at timestamp with time zone,
    approval_status character varying(16) DEFAULT 'NONE'::character varying NOT NULL,
    approved_at timestamp with time zone,
    approved_by character varying(64),
    CONSTRAINT stocktake_approval_check CHECK (((approval_status)::text = ANY ((ARRAY['NONE'::character varying, 'PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);



CREATE TABLE public.stocktake_line (
    id uuid NOT NULL,
    stocktake_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    system_quantity integer NOT NULL,
    counted_quantity integer,
    variance integer,
    reason character varying(255)
);



CREATE TABLE public.store (
    id uuid NOT NULL,
    market_code character varying(8) NOT NULL,
    name character varying(128) NOT NULL,
    address character varying(255) NOT NULL,
    latitude numeric(9,6),
    longitude numeric(9,6),
    pickup_enabled boolean DEFAULT true NOT NULL,
    active boolean DEFAULT true NOT NULL,
    phone character varying(32),
    opening_hours character varying(128),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.stored_value_ledger (
    id uuid NOT NULL,
    member_id uuid NOT NULL,
    entry_type character varying(16) NOT NULL,
    amount bigint NOT NULL,
    balance_after bigint NOT NULL,
    business_key character varying(128) NOT NULL,
    note character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT stored_value_ledger_amount_check CHECK ((amount <> 0)),
    CONSTRAINT stored_value_ledger_balance_check CHECK ((balance_after >= 0)),
    CONSTRAINT stored_value_ledger_type_check CHECK (((entry_type)::text = ANY ((ARRAY['TOPUP'::character varying, 'CONSUME'::character varying, 'REFUND'::character varying, 'ADJUST'::character varying])::text[])))
);



CREATE TABLE public.supplier (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    contact_name character varying(64),
    contact_phone character varying(32),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.supplier_credential (
    id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    access_key character varying(64) NOT NULL,
    secret_encrypted text NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    rotated_at timestamp with time zone,
    ip_allowlist character varying(512),
    auth_mode character varying(16) DEFAULT 'HMAC_ONLY'::character varying NOT NULL,
    token_hash character varying(64),
    token_expires_at timestamp with time zone,
    token_revoked_at timestamp with time zone,
    CONSTRAINT supplier_credential_auth_mode_check CHECK (((auth_mode)::text = ANY ((ARRAY['HMAC_ONLY'::character varying, 'TOKEN_ONLY'::character varying, 'BOTH'::character varying])::text[])))
);



CREATE TABLE public.supplier_nonce (
    access_key character varying(64) NOT NULL,
    nonce character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.supplier_rate_window (
    access_key character varying(64) NOT NULL,
    window_start timestamp with time zone NOT NULL,
    request_count integer NOT NULL,
    CONSTRAINT supplier_rate_window_count_positive CHECK ((request_count > 0))
);



CREATE TABLE public.supplier_return (
    id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    order_id uuid NOT NULL,
    reason character varying(255) NOT NULL,
    amount_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    status character varying(16) DEFAULT 'REQUESTED'::character varying NOT NULL,
    note character varying(512),
    created_by character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    closed_at timestamp with time zone,
    CONSTRAINT supplier_return_amount_check CHECK ((amount_amount >= 0)),
    CONSTRAINT supplier_return_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'ACCEPTED'::character varying, 'REJECTED'::character varying, 'SETTLED'::character varying])::text[])))
);



CREATE TABLE public.supplier_settlement (
    id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    period_start timestamp with time zone NOT NULL,
    period_end timestamp with time zone NOT NULL,
    order_count integer NOT NULL,
    gross_amount bigint NOT NULL,
    refund_amount bigint DEFAULT 0 NOT NULL,
    commission_amount bigint DEFAULT 0 NOT NULL,
    payable_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    status character varying(16) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT supplier_settlement_amounts_non_negative CHECK (((gross_amount >= 0) AND (refund_amount >= 0) AND (commission_amount >= 0))),
    CONSTRAINT supplier_settlement_payable_check CHECK ((payable_amount = ((gross_amount - refund_amount) - commission_amount))),
    CONSTRAINT supplier_settlement_period_check CHECK ((period_end > period_start)),
    CONSTRAINT supplier_settlement_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'CONFIRMED'::character varying, 'PAID'::character varying, 'DISPUTED'::character varying])::text[])))
);



CREATE TABLE public.supplier_sku (
    supplier_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    supplier_sku_code character varying(64) NOT NULL,
    price_amount bigint,
    price_currency character(3),
    active boolean DEFAULT true NOT NULL
);



CREATE TABLE public.supplier_stock_feed (
    id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    supplier_sku_code character varying(64) NOT NULL,
    available_quantity integer NOT NULL,
    price_amount bigint,
    price_currency character(3),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT supplier_stock_feed_quantity_non_negative CHECK ((available_quantity >= 0))
);



CREATE TABLE public.supplier_webhook (
    id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    url character varying(500) NOT NULL,
    secret character varying(128) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT supplier_webhook_url_check CHECK (((url)::text ~~ 'http%'::text))
);



CREATE TABLE public.support_conversation (
    id uuid NOT NULL,
    customer_id uuid,
    agent_id uuid,
    order_id uuid,
    status character varying(16) NOT NULL,
    locale character varying(16) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    subject character varying(255),
    customer_name character varying(64),
    customer_phone character varying(32),
    assigned_at timestamp with time zone,
    last_message_at timestamp with time zone
);



CREATE TABLE public.support_message (
    id uuid NOT NULL,
    conversation_id uuid NOT NULL,
    sender_type character varying(16) NOT NULL,
    sender_id uuid,
    message_type character varying(16) NOT NULL,
    content text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    metadata_json jsonb
);



CREATE TABLE public.support_transfer (
    id uuid NOT NULL,
    conversation_id uuid NOT NULL,
    from_agent_id uuid,
    to_agent_id uuid NOT NULL,
    reason character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.system_setting (
    setting_key character varying(64) NOT NULL,
    setting_value text DEFAULT ''::text NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.tax_rule (
    id uuid NOT NULL,
    jurisdiction character varying(32) NOT NULL,
    name character varying(64) NOT NULL,
    rate_bps integer NOT NULL,
    price_includes_tax boolean NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT tax_rule_rate_bps_check CHECK (((rate_bps >= 0) AND (rate_bps <= 10000)))
);



COMMENT ON COLUMN public.tax_rule.rate_bps IS '税率基点：1300 = 13%；10000 = 100%';



CREATE TABLE public.theme_config (
    id uuid NOT NULL,
    channel character varying(16) NOT NULL,
    market_code character varying(8) NOT NULL,
    config_json jsonb NOT NULL,
    version integer DEFAULT 1 NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.translation_glossary (
    id uuid NOT NULL,
    locale character varying(16) NOT NULL,
    source_term character varying(200) NOT NULL,
    target_term character varying(200) NOT NULL,
    version integer DEFAULT 1 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.translation_memory (
    source_hash character varying(64) NOT NULL,
    locale character varying(16) NOT NULL,
    glossary_version integer NOT NULL,
    translated_text text NOT NULL,
    provider character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.ui_translation (
    locale character varying(8) NOT NULL,
    text_key character varying(128) NOT NULL,
    text_value character varying(500) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.warehouse (
    id uuid NOT NULL,
    market_code character varying(8) NOT NULL,
    name character varying(128) NOT NULL,
    type character varying(32) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.webhook_delivery (
    id uuid NOT NULL,
    webhook_id uuid NOT NULL,
    event_type character varying(64) NOT NULL,
    payload jsonb NOT NULL,
    status character varying(16) DEFAULT 'PENDING'::character varying NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    next_attempt_at timestamp with time zone DEFAULT now() NOT NULL,
    last_error text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT webhook_delivery_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCEEDED'::character varying, 'FAILED'::character varying])::text[])))
);



CREATE TABLE public.withdrawal (
    id uuid NOT NULL,
    promoter_id uuid NOT NULL,
    amount_amount bigint NOT NULL,
    currency character(3) NOT NULL,
    status character varying(16) DEFAULT 'PENDING'::character varying NOT NULL,
    requested_at timestamp with time zone DEFAULT now() NOT NULL,
    reviewed_by character varying(128),
    reviewed_at timestamp with time zone,
    reject_reason character varying(255),
    paid_at timestamp with time zone,
    payment_ref character varying(128),
    payout_method character varying(16),
    payout_account character varying(128),
    CONSTRAINT withdrawal_amount_positive CHECK ((amount_amount > 0)),
    CONSTRAINT withdrawal_payout_method_check CHECK (((payout_method IS NULL) OR ((payout_method)::text = ANY ((ARRAY['ALIPAY'::character varying, 'BANK'::character varying, 'WECHAT'::character varying])::text[])))),
    CONSTRAINT withdrawal_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'PAID'::character varying, 'FAILED'::character varying])::text[])))
);



ALTER TABLE ONLY public.address_format
    ADD CONSTRAINT address_format_pkey PRIMARY KEY (country_code);



ALTER TABLE ONLY public.after_sale
    ADD CONSTRAINT after_sale_after_sale_no_key UNIQUE (after_sale_no);



ALTER TABLE ONLY public.after_sale_event
    ADD CONSTRAINT after_sale_event_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.after_sale_line
    ADD CONSTRAINT after_sale_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.after_sale
    ADD CONSTRAINT after_sale_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ai_event
    ADD CONSTRAINT ai_event_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ai_gen_job
    ADD CONSTRAINT ai_gen_job_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ai_prompt_history
    ADD CONSTRAINT ai_prompt_history_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ai_provider
    ADD CONSTRAINT ai_provider_code_key UNIQUE (code);



ALTER TABLE ONLY public.ai_provider
    ADD CONSTRAINT ai_provider_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ai_storyboard
    ADD CONSTRAINT ai_storyboard_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ai_storyboard_shot
    ADD CONSTRAINT ai_storyboard_shot_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ai_storyboard_shot
    ADD CONSTRAINT ai_storyboard_shot_unique UNIQUE (storyboard_id, sort_order);



ALTER TABLE ONLY public.analytics_event
    ADD CONSTRAINT analytics_event_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.article_category
    ADD CONSTRAINT article_category_code_key UNIQUE (code);



ALTER TABLE ONLY public.article_category
    ADD CONSTRAINT article_category_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.article
    ADD CONSTRAINT article_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.article
    ADD CONSTRAINT article_slug_key UNIQUE (slug);



ALTER TABLE ONLY public.article_translation
    ADD CONSTRAINT article_translation_pkey PRIMARY KEY (article_id, locale);



ALTER TABLE ONLY public.asset_image_embedding
    ADD CONSTRAINT asset_image_embedding_pkey PRIMARY KEY (asset_id);



ALTER TABLE ONLY public.asset_locale_variant
    ADD CONSTRAINT asset_locale_variant_pkey PRIMARY KEY (asset_id, locale);



ALTER TABLE ONLY public.asset
    ADD CONSTRAINT asset_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.audit_event
    ADD CONSTRAINT audit_event_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.audit_outbox
    ADD CONSTRAINT audit_outbox_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.bargain_activity
    ADD CONSTRAINT bargain_activity_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.bargain_help
    ADD CONSTRAINT bargain_help_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.bargain_help
    ADD CONSTRAINT bargain_help_unique UNIQUE (session_id, helper_member_id);



ALTER TABLE ONLY public.bargain_session
    ADD CONSTRAINT bargain_session_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.bargain_session
    ADD CONSTRAINT bargain_session_unique UNIQUE (activity_id, member_id);



ALTER TABLE ONLY public.brand
    ADD CONSTRAINT brand_code_key UNIQUE (code);



ALTER TABLE ONLY public.brand
    ADD CONSTRAINT brand_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.cart_item
    ADD CONSTRAINT cart_item_cart_id_sku_id_key UNIQUE (cart_id, sku_id);



ALTER TABLE ONLY public.cart_item
    ADD CONSTRAINT cart_item_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.cart
    ADD CONSTRAINT cart_owner_key_key UNIQUE (owner_key);



ALTER TABLE ONLY public.cart
    ADD CONSTRAINT cart_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.category
    ADD CONSTRAINT category_code_key UNIQUE (code);



ALTER TABLE ONLY public.category
    ADD CONSTRAINT category_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.commission_ledger
    ADD CONSTRAINT commission_ledger_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.coupon
    ADD CONSTRAINT coupon_code_key UNIQUE (code);



ALTER TABLE ONLY public.coupon
    ADD CONSTRAINT coupon_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.coupon_redemption
    ADD CONSTRAINT coupon_redemption_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.coupon_template
    ADD CONSTRAINT coupon_template_code_key UNIQUE (code);



ALTER TABLE ONLY public.coupon_template
    ADD CONSTRAINT coupon_template_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.coupon_template_scope
    ADD CONSTRAINT coupon_template_scope_pkey PRIMARY KEY (template_id, scope_type, scope_id);



ALTER TABLE ONLY public.distribution_commission
    ADD CONSTRAINT distribution_commission_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.distribution_commission
    ADD CONSTRAINT distribution_commission_unique UNIQUE (order_id, promoter_staff_id, level);



ALTER TABLE ONLY public.export_job
    ADD CONSTRAINT export_job_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.export_task
    ADD CONSTRAINT export_task_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.faq_entry
    ADD CONSTRAINT faq_entry_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.faq_entry
    ADD CONSTRAINT faq_entry_question_unique UNIQUE (question);



ALTER TABLE ONLY public.faq_miss
    ADD CONSTRAINT faq_miss_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.feature_flag
    ADD CONSTRAINT feature_flag_pkey PRIMARY KEY (code);



ALTER TABLE ONLY public.fulfillment_line
    ADD CONSTRAINT fulfillment_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.fulfillment_order
    ADD CONSTRAINT fulfillment_order_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.goods_receipt_line
    ADD CONSTRAINT goods_receipt_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.goods_receipt
    ADD CONSTRAINT goods_receipt_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.goods_receipt
    ADD CONSTRAINT goods_receipt_receipt_no_key UNIQUE (receipt_no);



ALTER TABLE ONLY public.group_buy
    ADD CONSTRAINT group_buy_code_key UNIQUE (code);



ALTER TABLE ONLY public.group_buy_member
    ADD CONSTRAINT group_buy_member_order_unique UNIQUE (order_id);



ALTER TABLE ONLY public.group_buy_member
    ADD CONSTRAINT group_buy_member_phone_unique UNIQUE (group_buy_id, phone);



ALTER TABLE ONLY public.group_buy_member
    ADD CONSTRAINT group_buy_member_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.group_buy
    ADD CONSTRAINT group_buy_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.growth_ledger
    ADD CONSTRAINT growth_ledger_business_key_key UNIQUE (business_key);



ALTER TABLE ONLY public.growth_ledger
    ADD CONSTRAINT growth_ledger_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.idempotency_record
    ADD CONSTRAINT idempotency_record_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.idempotency_record
    ADD CONSTRAINT idempotency_record_scope_idempotency_key_key UNIQUE (scope, idempotency_key);



ALTER TABLE ONLY public.integration_config
    ADD CONSTRAINT integration_config_pkey PRIMARY KEY (capability, provider);



ALTER TABLE ONLY public.inventory_batch
    ADD CONSTRAINT inventory_batch_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.inventory_batch
    ADD CONSTRAINT inventory_batch_sku_id_location_type_location_id_batch_no_key UNIQUE (sku_id, location_type, location_id, batch_no);



ALTER TABLE ONLY public.jv_commit
    ADD CONSTRAINT jv_commit_pkey PRIMARY KEY (commit_pk);



ALTER TABLE ONLY public.jv_commit_property
    ADD CONSTRAINT jv_commit_property_pkey PRIMARY KEY (commit_fk, property_name);



ALTER TABLE ONLY public.jv_global_id
    ADD CONSTRAINT jv_global_id_pkey PRIMARY KEY (global_id_pk);



ALTER TABLE ONLY public.jv_snapshot
    ADD CONSTRAINT jv_snapshot_pkey PRIMARY KEY (snapshot_pk);



ALTER TABLE ONLY public.knowledge_chunk
    ADD CONSTRAINT knowledge_chunk_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.knowledge_embedding
    ADD CONSTRAINT knowledge_embedding_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.knowledge_embedding
    ADD CONSTRAINT knowledge_embedding_unique UNIQUE (kind, source_id);



ALTER TABLE ONLY public.knowledge_sync_state
    ADD CONSTRAINT knowledge_sync_state_pkey PRIMARY KEY (sku_id);



ALTER TABLE ONLY public.legal_acceptance
    ADD CONSTRAINT legal_acceptance_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.legal_document
    ADD CONSTRAINT legal_document_code_key UNIQUE (code);



ALTER TABLE ONLY public.legal_document_localization
    ADD CONSTRAINT legal_document_localization_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.legal_document_localization
    ADD CONSTRAINT legal_document_localization_version_id_locale_key UNIQUE (version_id, locale);



ALTER TABLE ONLY public.legal_document
    ADD CONSTRAINT legal_document_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.legal_document_version
    ADD CONSTRAINT legal_document_version_document_id_version_key UNIQUE (document_id, version);



ALTER TABLE ONLY public.legal_document_version
    ADD CONSTRAINT legal_document_version_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.license_state
    ADD CONSTRAINT license_state_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.market_destination
    ADD CONSTRAINT market_destination_pkey PRIMARY KEY (market_code, destination_country);



ALTER TABLE ONLY public.market_integration
    ADD CONSTRAINT market_integration_pkey PRIMARY KEY (market_code, capability, provider);



ALTER TABLE ONLY public.market_locale
    ADD CONSTRAINT market_locale_pkey PRIMARY KEY (market_code, locale);



ALTER TABLE ONLY public.market
    ADD CONSTRAINT market_pkey PRIMARY KEY (code);



ALTER TABLE ONLY public.market_setting
    ADD CONSTRAINT market_setting_pkey PRIMARY KEY (market_code, setting_key);



ALTER TABLE ONLY public.member_address
    ADD CONSTRAINT member_address_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.member_browse_history
    ADD CONSTRAINT member_browse_history_pkey PRIMARY KEY (member_id, sku_id);



ALTER TABLE ONLY public.member_card_ledger
    ADD CONSTRAINT member_card_ledger_business_key_key UNIQUE (business_key);



ALTER TABLE ONLY public.member_card_ledger
    ADD CONSTRAINT member_card_ledger_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.member_coupon
    ADD CONSTRAINT member_coupon_code_key UNIQUE (code);



ALTER TABLE ONLY public.member_coupon
    ADD CONSTRAINT member_coupon_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.member_favorite
    ADD CONSTRAINT member_favorite_pkey PRIMARY KEY (member_id, sku_id);



ALTER TABLE ONLY public.member_level
    ADD CONSTRAINT member_level_pkey PRIMARY KEY (code);



ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_member_no_key UNIQUE (member_no);



ALTER TABLE ONLY public.member_otp
    ADD CONSTRAINT member_otp_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_phone_key UNIQUE (phone);



ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.member_session
    ADD CONSTRAINT member_session_pkey PRIMARY KEY (token_hash);



ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.order_coupon_allocation
    ADD CONSTRAINT order_coupon_allocation_pkey PRIMARY KEY (order_line_id);



ALTER TABLE ONLY public.order_line
    ADD CONSTRAINT order_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.order_line_refund
    ADD CONSTRAINT order_line_refund_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.order_tax_line
    ADD CONSTRAINT order_tax_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_order_no_key UNIQUE (order_no);



ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.payment_event
    ADD CONSTRAINT payment_event_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.payment_provider_config
    ADD CONSTRAINT payment_provider_config_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.payment_provider_config_version
    ADD CONSTRAINT payment_provider_config_version_config_id_version_no_key UNIQUE (config_id, version_no);



ALTER TABLE ONLY public.payment_provider_config_version
    ADD CONSTRAINT payment_provider_config_version_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.payment_reconciliation
    ADD CONSTRAINT payment_reconciliation_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.payment_reconciliation
    ADD CONSTRAINT payment_reconciliation_unique UNIQUE (bill_date, order_no);



ALTER TABLE ONLY public.permission
    ADD CONSTRAINT permission_pkey PRIMARY KEY (code);



ALTER TABLE ONLY public.pick_line_order_line
    ADD CONSTRAINT pick_line_order_line_pkey PRIMARY KEY (pick_line_id, order_line_id);



ALTER TABLE ONLY public.pick_task_line
    ADD CONSTRAINT pick_task_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pick_task
    ADD CONSTRAINT pick_task_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pickup
    ADD CONSTRAINT pickup_pickup_code_key UNIQUE (pickup_code);



ALTER TABLE ONLY public.pickup
    ADD CONSTRAINT pickup_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.points_ledger
    ADD CONSTRAINT points_ledger_business_key_key UNIQUE (business_key);



ALTER TABLE ONLY public.points_ledger
    ADD CONSTRAINT points_ledger_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_cash_movement
    ADD CONSTRAINT pos_cash_movement_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_reconciliation
    ADD CONSTRAINT pos_reconciliation_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_reconciliation
    ADD CONSTRAINT pos_reconciliation_store_id_business_date_tender_type_key UNIQUE (store_id, business_date, tender_type);



ALTER TABLE ONLY public.pos_refund_allocation
    ADD CONSTRAINT pos_refund_allocation_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_refund_line
    ADD CONSTRAINT pos_refund_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_refund
    ADD CONSTRAINT pos_refund_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_refund
    ADD CONSTRAINT pos_refund_refund_no_key UNIQUE (refund_no);



ALTER TABLE ONLY public.pos_refund_tender
    ADD CONSTRAINT pos_refund_tender_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_sale_allocation
    ADD CONSTRAINT pos_sale_allocation_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_sale_line
    ADD CONSTRAINT pos_sale_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_sale
    ADD CONSTRAINT pos_sale_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_sale
    ADD CONSTRAINT pos_sale_sale_no_key UNIQUE (sale_no);



ALTER TABLE ONLY public.pos_shift
    ADD CONSTRAINT pos_shift_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pos_tender
    ADD CONSTRAINT pos_tender_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.product_media
    ADD CONSTRAINT product_media_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.product_media
    ADD CONSTRAINT product_media_unique UNIQUE (product_id, sku_id, asset_id, role);



ALTER TABLE ONLY public.product_option
    ADD CONSTRAINT product_option_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.product_option
    ADD CONSTRAINT product_option_unique UNIQUE (product_id, name);



ALTER TABLE ONLY public.product_option_value
    ADD CONSTRAINT product_option_value_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.product_option_value
    ADD CONSTRAINT product_option_value_unique UNIQUE (option_id, name);



ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.product_review
    ADD CONSTRAINT product_review_order_id_sku_id_member_id_key UNIQUE (order_id, sku_id, member_id);



ALTER TABLE ONLY public.product_review
    ADD CONSTRAINT product_review_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.product_translation
    ADD CONSTRAINT product_translation_pkey PRIMARY KEY (product_id, locale);



ALTER TABLE ONLY public.promoter_link
    ADD CONSTRAINT promoter_link_pkey PRIMARY KEY (promoter_type, promoter_id);



ALTER TABLE ONLY public.promotion
    ADD CONSTRAINT promotion_code_key UNIQUE (code);



ALTER TABLE ONLY public.promotion_ladder
    ADD CONSTRAINT promotion_ladder_code_key UNIQUE (code);



ALTER TABLE ONLY public.promotion_ladder
    ADD CONSTRAINT promotion_ladder_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.promotion
    ADD CONSTRAINT promotion_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.promotion_redemption
    ADD CONSTRAINT promotion_redemption_order_unique UNIQUE (promotion_id, order_id);



ALTER TABLE ONLY public.promotion_redemption
    ADD CONSTRAINT promotion_redemption_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.purchase_order_line
    ADD CONSTRAINT purchase_order_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.purchase_order
    ADD CONSTRAINT purchase_order_order_no_key UNIQUE (order_no);



ALTER TABLE ONLY public.purchase_order
    ADD CONSTRAINT purchase_order_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.referral_binding
    ADD CONSTRAINT referral_binding_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.region
    ADD CONSTRAINT region_pkey PRIMARY KEY (code);



ALTER TABLE ONLY public.reservation
    ADD CONSTRAINT reservation_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.risk_alert
    ADD CONSTRAINT risk_alert_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.role_permission
    ADD CONSTRAINT role_permission_pkey PRIMARY KEY (role, permission_code);



ALTER TABLE ONLY public.scheduled_task_lease
    ADD CONSTRAINT scheduled_task_lease_pkey PRIMARY KEY (code);



ALTER TABLE ONLY public.scheduled_task
    ADD CONSTRAINT scheduled_task_pkey PRIMARY KEY (code);



ALTER TABLE ONLY public.shipping_rate
    ADD CONSTRAINT shipping_rate_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.sku_cost
    ADD CONSTRAINT sku_cost_pkey PRIMARY KEY (sku_id);



ALTER TABLE ONLY public.sku_matrix_dimension
    ADD CONSTRAINT sku_matrix_dimension_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.sku_matrix_dimension
    ADD CONSTRAINT sku_matrix_dimension_unique UNIQUE (template_id, name);



ALTER TABLE ONLY public.sku_matrix_dimension_value
    ADD CONSTRAINT sku_matrix_dimension_value_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.sku_matrix_disabled
    ADD CONSTRAINT sku_matrix_disabled_pkey PRIMARY KEY (template_id, combination_json);



ALTER TABLE ONLY public.sku_matrix_template
    ADD CONSTRAINT sku_matrix_template_code_key UNIQUE (code);



ALTER TABLE ONLY public.sku_matrix_template
    ADD CONSTRAINT sku_matrix_template_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.sku_matrix_dimension_value
    ADD CONSTRAINT sku_matrix_value_unique UNIQUE (dimension_id, name);



ALTER TABLE ONLY public.sku_option_value
    ADD CONSTRAINT sku_option_value_pkey PRIMARY KEY (sku_id, option_id);



ALTER TABLE ONLY public.sku
    ADD CONSTRAINT sku_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.sku_price
    ADD CONSTRAINT sku_price_pkey PRIMARY KEY (sku_id, market_code);



ALTER TABLE ONLY public.sku
    ADD CONSTRAINT sku_product_code_key UNIQUE (product_id, sku_code);



ALTER TABLE ONLY public.sku_serial
    ADD CONSTRAINT sku_serial_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.sku_serial
    ADD CONSTRAINT sku_serial_serial_no_key UNIQUE (serial_no);



ALTER TABLE ONLY public.staff_session
    ADD CONSTRAINT staff_session_pkey PRIMARY KEY (token_hash);



ALTER TABLE ONLY public.staff_store
    ADD CONSTRAINT staff_store_pkey PRIMARY KEY (staff_id, store_id);



ALTER TABLE ONLY public.staff_user
    ADD CONSTRAINT staff_user_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.staff_user
    ADD CONSTRAINT staff_user_username_key UNIQUE (username);



ALTER TABLE ONLY public.stock_cost_movement
    ADD CONSTRAINT stock_cost_movement_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.stock_move
    ADD CONSTRAINT stock_move_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.stock_transfer_allocation
    ADD CONSTRAINT stock_transfer_allocation_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.stock_transfer_line
    ADD CONSTRAINT stock_transfer_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.stock_transfer
    ADD CONSTRAINT stock_transfer_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.stock_transfer
    ADD CONSTRAINT stock_transfer_transfer_no_key UNIQUE (transfer_no);



ALTER TABLE ONLY public.stocktake_line
    ADD CONSTRAINT stocktake_line_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.stocktake
    ADD CONSTRAINT stocktake_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.stocktake
    ADD CONSTRAINT stocktake_stocktake_no_key UNIQUE (stocktake_no);



ALTER TABLE ONLY public.store
    ADD CONSTRAINT store_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.stored_value_ledger
    ADD CONSTRAINT stored_value_ledger_business_key_key UNIQUE (business_key);



ALTER TABLE ONLY public.stored_value_ledger
    ADD CONSTRAINT stored_value_ledger_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.supplier
    ADD CONSTRAINT supplier_code_key UNIQUE (code);



ALTER TABLE ONLY public.supplier_credential
    ADD CONSTRAINT supplier_credential_access_key_key UNIQUE (access_key);



ALTER TABLE ONLY public.supplier_credential
    ADD CONSTRAINT supplier_credential_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.supplier_credential
    ADD CONSTRAINT supplier_credential_supplier_id_key UNIQUE (supplier_id);



ALTER TABLE ONLY public.supplier_nonce
    ADD CONSTRAINT supplier_nonce_pkey PRIMARY KEY (access_key, nonce);



ALTER TABLE ONLY public.supplier
    ADD CONSTRAINT supplier_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.supplier_rate_window
    ADD CONSTRAINT supplier_rate_window_pkey PRIMARY KEY (access_key, window_start);



ALTER TABLE ONLY public.supplier_return
    ADD CONSTRAINT supplier_return_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.supplier_settlement
    ADD CONSTRAINT supplier_settlement_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.supplier_sku
    ADD CONSTRAINT supplier_sku_pkey PRIMARY KEY (supplier_id, sku_id);



ALTER TABLE ONLY public.supplier_stock_feed
    ADD CONSTRAINT supplier_stock_feed_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.supplier_stock_feed
    ADD CONSTRAINT supplier_stock_feed_supplier_id_sku_id_key UNIQUE (supplier_id, sku_id);



ALTER TABLE ONLY public.supplier_webhook
    ADD CONSTRAINT supplier_webhook_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.support_conversation
    ADD CONSTRAINT support_conversation_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.support_message
    ADD CONSTRAINT support_message_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.support_transfer
    ADD CONSTRAINT support_transfer_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.system_setting
    ADD CONSTRAINT system_setting_pkey PRIMARY KEY (setting_key);



ALTER TABLE ONLY public.tax_rule
    ADD CONSTRAINT tax_rule_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.theme_config
    ADD CONSTRAINT theme_config_channel_market_code_key UNIQUE (channel, market_code);



ALTER TABLE ONLY public.theme_config
    ADD CONSTRAINT theme_config_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.translation_glossary
    ADD CONSTRAINT translation_glossary_locale_source_term_key UNIQUE (locale, source_term);



ALTER TABLE ONLY public.translation_glossary
    ADD CONSTRAINT translation_glossary_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.translation_memory
    ADD CONSTRAINT translation_memory_pkey PRIMARY KEY (source_hash, locale, glossary_version);



ALTER TABLE ONLY public.ui_translation
    ADD CONSTRAINT ui_translation_pkey PRIMARY KEY (locale, text_key);



ALTER TABLE ONLY public.warehouse
    ADD CONSTRAINT warehouse_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.webhook_delivery
    ADD CONSTRAINT webhook_delivery_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.withdrawal
    ADD CONSTRAINT withdrawal_pkey PRIMARY KEY (id);



CREATE INDEX after_sale_auto_complete_idx ON public.after_sale USING btree (auto_complete_at) WHERE ((status)::text = 'EXCHANGE_SHIPPED'::text);



CREATE INDEX ai_event_keyword_idx ON public.ai_event USING btree (keyword);



CREATE INDEX ai_event_sku_idx ON public.ai_event USING btree (sku_id);



CREATE INDEX ai_event_type_time_idx ON public.ai_event USING btree (event_type, created_at DESC);



CREATE INDEX ai_gen_job_product_idx ON public.ai_gen_job USING btree (product_id, created_at DESC);



CREATE INDEX ai_gen_job_queue_idx ON public.ai_gen_job USING btree (status, created_at);



CREATE INDEX ai_prompt_history_scope_idx ON public.ai_prompt_history USING btree (product_id, sku_id, created_at DESC);



CREATE INDEX ai_provider_pick_idx ON public.ai_provider USING btree (kind, enabled, priority);



CREATE INDEX ai_storyboard_shot_idx ON public.ai_storyboard_shot USING btree (storyboard_id, sort_order);



CREATE INDEX article_published_idx ON public.article USING btree (type, status, published_at DESC);



CREATE INDEX article_translation_locale_idx ON public.article_translation USING btree (locale, status);



CREATE INDEX asset_kind_idx ON public.asset USING btree (kind);



CREATE INDEX asset_owner_idx ON public.asset USING btree (owner_username, deleted_at);



CREATE INDEX asset_tags_gin ON public.asset USING gin (tags);



CREATE INDEX audit_event_actor_created_idx ON public.audit_event USING btree (actor, created_at DESC);



CREATE INDEX audit_event_type_created_idx ON public.audit_event USING btree (event_type, created_at DESC);



CREATE INDEX audit_log_occurred_idx ON public.audit_log USING btree (occurred_at DESC);



CREATE INDEX audit_outbox_pending_idx ON public.audit_outbox USING btree (status, created_at);



CREATE INDEX bargain_activity_sku_idx ON public.bargain_activity USING btree (sku_id, status);



CREATE INDEX bargain_session_member_idx ON public.bargain_session USING btree (member_id, status);



CREATE UNIQUE INDEX brand_name_unique ON public.brand USING btree (lower(btrim((name)::text)));



CREATE INDEX cart_item_cart_idx ON public.cart_item USING btree (cart_id, added_at);



CREATE INDEX commission_ledger_commission_idx ON public.commission_ledger USING btree (commission_id, created_at);



CREATE INDEX coupon_redemption_coupon_created_idx ON public.coupon_redemption USING btree (coupon_id, created_at DESC);



CREATE INDEX coupon_template_scope_target_idx ON public.coupon_template_scope USING btree (scope_type, scope_id);



CREATE INDEX distribution_commission_due_idx ON public.distribution_commission USING btree (status, frozen_until);



CREATE INDEX distribution_commission_hold_idx ON public.distribution_commission USING btree (hold_after_sale_id) WHERE (hold_after_sale_id IS NOT NULL);



CREATE INDEX distribution_commission_order_idx ON public.distribution_commission USING btree (order_id, level);



CREATE INDEX distribution_commission_payroll_idx ON public.distribution_commission USING btree (payee_channel, created_at DESC);



CREATE INDEX export_job_pending_idx ON public.export_job USING btree (status, created_at);



CREATE INDEX export_job_requester_idx ON public.export_job USING btree (requester, created_at DESC);



CREATE INDEX export_task_actor_idx ON public.export_task USING btree (actor, created_at DESC);



CREATE INDEX export_task_created_idx ON public.export_task USING btree (created_at DESC);



CREATE INDEX faq_entry_question_trgm_idx ON public.faq_entry USING btree (lower((question)::text));



CREATE INDEX faq_miss_created_idx ON public.faq_miss USING btree (created_at DESC);



CREATE INDEX fk_after_sale_line_order_line_id_idx ON public.after_sale_line USING btree (order_line_id);



CREATE INDEX fk_article_category_id_idx ON public.article USING btree (category_id);



CREATE INDEX fk_bargain_help_helper_member_id_idx ON public.bargain_help USING btree (helper_member_id);



CREATE INDEX fk_cart_item_sku_id_idx ON public.cart_item USING btree (sku_id);



CREATE INDEX fk_category_parent_id_idx ON public.category USING btree (parent_id);



CREATE INDEX fk_coupon_redemption_coupon_id_idx ON public.coupon_redemption USING btree (coupon_id);



CREATE INDEX fk_goods_receipt_line_sku_id_idx ON public.goods_receipt_line USING btree (sku_id);



CREATE INDEX fk_goods_receipt_supplier_id_idx ON public.goods_receipt USING btree (supplier_id);



CREATE INDEX fk_group_buy_promotion_id_idx ON public.group_buy USING btree (promotion_id);



CREATE INDEX fk_group_buy_sku_id_idx ON public.group_buy USING btree (sku_id);



CREATE INDEX fk_legal_document_market_code_idx ON public.legal_document USING btree (market_code);



CREATE INDEX fk_member_browse_history_sku_id_idx ON public.member_browse_history USING btree (sku_id);



CREATE INDEX fk_member_favorite_sku_id_idx ON public.member_favorite USING btree (sku_id);



CREATE INDEX fk_orders_store_id_idx ON public.orders USING btree (store_id);



CREATE INDEX fk_payment_config_version_id_idx ON public.payment USING btree (config_version_id);



CREATE INDEX fk_payment_event_payment_id_idx ON public.payment_event USING btree (payment_id);



CREATE INDEX fk_pick_task_fulfillment_order_id_idx ON public.pick_task USING btree (fulfillment_order_id);



CREATE INDEX fk_pick_task_line_order_line_id_idx ON public.pick_task_line USING btree (order_line_id);



CREATE INDEX fk_pick_task_line_suggested_batch_id_idx ON public.pick_task_line USING btree (suggested_batch_id);



CREATE INDEX fk_pickup_fulfillment_order_id_idx ON public.pickup USING btree (fulfillment_order_id);



CREATE INDEX fk_pickup_store_id_idx ON public.pickup USING btree (store_id);



CREATE INDEX fk_pos_refund_allocation_batch_id_idx ON public.pos_refund_allocation USING btree (batch_id);



CREATE INDEX fk_pos_refund_tender_tender_id_idx ON public.pos_refund_tender USING btree (tender_id);



CREATE INDEX fk_pos_sale_allocation_batch_id_idx ON public.pos_sale_allocation USING btree (batch_id);



CREATE INDEX fk_pos_sale_coupon_id_idx ON public.pos_sale USING btree (coupon_id);



CREATE INDEX fk_pos_sale_member_id_idx ON public.pos_sale USING btree (member_id);



CREATE INDEX fk_product_category_id_idx ON public.product USING btree (category_id);



CREATE INDEX fk_product_review_member_id_idx ON public.product_review USING btree (member_id);



CREATE INDEX fk_purchase_order_line_sku_id_idx ON public.purchase_order_line USING btree (sku_id);



CREATE INDEX fk_purchase_order_supplier_id_idx ON public.purchase_order USING btree (supplier_id);



CREATE INDEX fk_reservation_batch_id_idx ON public.reservation USING btree (batch_id);



CREATE INDEX fk_role_permission_permission_code_idx ON public.role_permission USING btree (permission_code);



CREATE INDEX fk_sku_option_value_option_id_idx ON public.sku_option_value USING btree (option_id);



CREATE INDEX fk_sku_serial_order_line_id_idx ON public.sku_serial USING btree (order_line_id);



CREATE INDEX fk_staff_store_store_id_idx ON public.staff_store USING btree (store_id);



CREATE INDEX fk_stock_transfer_allocation_source_batch_id_idx ON public.stock_transfer_allocation USING btree (source_batch_id);



CREATE INDEX fk_stock_transfer_line_sku_id_idx ON public.stock_transfer_line USING btree (sku_id);



CREATE INDEX fk_stocktake_line_batch_id_idx ON public.stocktake_line USING btree (batch_id);



CREATE INDEX fk_stocktake_line_sku_id_idx ON public.stocktake_line USING btree (sku_id);



CREATE INDEX fk_store_market_code_idx ON public.store USING btree (market_code);



CREATE INDEX fk_supplier_return_order_id_idx ON public.supplier_return USING btree (order_id);



CREATE INDEX fk_supplier_sku_sku_id_idx ON public.supplier_sku USING btree (sku_id);



CREATE INDEX fk_supplier_stock_feed_sku_id_idx ON public.supplier_stock_feed USING btree (sku_id);



CREATE INDEX fk_warehouse_market_code_idx ON public.warehouse USING btree (market_code);



CREATE INDEX fk_webhook_delivery_webhook_id_idx ON public.webhook_delivery USING btree (webhook_id);



CREATE INDEX group_buy_status_idx ON public.group_buy USING btree (status, expires_at);



CREATE INDEX idx_after_sale_event_sale ON public.after_sale_event USING btree (after_sale_id, created_at);



CREATE INDEX idx_after_sale_line_sale ON public.after_sale_line USING btree (after_sale_id);



CREATE INDEX idx_after_sale_order ON public.after_sale USING btree (order_id, created_at DESC);



CREATE INDEX idx_after_sale_status ON public.after_sale USING btree (status, created_at DESC);



CREATE INDEX idx_analytics_event_type_time ON public.analytics_event USING btree (event_type, occurred_at);



CREATE INDEX idx_audit_log_resource ON public.audit_log USING btree (resource_type, resource_id, occurred_at);



CREATE INDEX idx_coupon_member_status ON public.coupon USING btree (member_id, status, valid_to);



CREATE INDEX idx_coupon_redemption_sale ON public.coupon_redemption USING btree (sale_id);



CREATE INDEX idx_distribution_commission_promoter ON public.distribution_commission USING btree (promoter_staff_id, created_at DESC);



CREATE INDEX idx_fulfillment_line_order ON public.fulfillment_line USING btree (fulfillment_order_id);



CREATE INDEX idx_fulfillment_order_order ON public.fulfillment_order USING btree (order_id);



CREATE INDEX idx_goods_receipt_line_receipt ON public.goods_receipt_line USING btree (receipt_id);



CREATE INDEX idx_growth_ledger_member ON public.growth_ledger USING btree (member_id, created_at DESC);



CREATE INDEX idx_idempotency_record_created ON public.idempotency_record USING btree (created_at);



CREATE INDEX idx_inventory_batch_lookup ON public.inventory_batch USING btree (sku_id, location_type, location_id, expiry_date, status);



CREATE INDEX idx_legal_acceptance_version ON public.legal_acceptance USING btree (document_version_id, accepted_at);



CREATE INDEX idx_member_card_ledger_member ON public.member_card_ledger USING btree (member_id, created_at DESC);



CREATE INDEX idx_notification_created ON public.notification USING btree (created_at);



CREATE INDEX idx_notification_recipient ON public.notification USING btree (recipient_type, recipient_id, created_at);



CREATE INDEX idx_notification_unread ON public.notification USING btree (recipient_type, read_at);



CREATE INDEX idx_order_line_order ON public.order_line USING btree (order_id);



CREATE INDEX idx_orders_status_created ON public.orders USING btree (status, created_at);



CREATE INDEX idx_payment_order ON public.payment USING btree (order_id, created_at);



CREATE INDEX idx_pick_task_line_task ON public.pick_task_line USING btree (pick_task_id);



CREATE INDEX idx_pick_task_store_status ON public.pick_task USING btree (store_id, status);



CREATE INDEX idx_pickup_status ON public.pickup USING btree (status, expires_at);



CREATE INDEX idx_points_ledger_member ON public.points_ledger USING btree (member_id, created_at DESC);



CREATE INDEX idx_pos_cash_movement_shift ON public.pos_cash_movement USING btree (shift_id, created_at);



CREATE INDEX idx_pos_refund_allocation_line ON public.pos_refund_allocation USING btree (refund_line_id);



CREATE INDEX idx_pos_refund_line_refund ON public.pos_refund_line USING btree (refund_id);



CREATE INDEX idx_pos_refund_line_sale_line ON public.pos_refund_line USING btree (sale_line_id);



CREATE INDEX idx_pos_refund_sale ON public.pos_refund USING btree (sale_id);



CREATE INDEX idx_pos_refund_tender_refund ON public.pos_refund_tender USING btree (refund_id);



CREATE INDEX idx_pos_sale_allocation_line ON public.pos_sale_allocation USING btree (sale_line_id);



CREATE INDEX idx_pos_sale_line_sale ON public.pos_sale_line USING btree (sale_id);



CREATE INDEX idx_pos_sale_shift ON public.pos_sale USING btree (shift_id, created_at DESC);



CREATE INDEX idx_pos_sale_store_created ON public.pos_sale USING btree (store_id, created_at DESC);



CREATE INDEX idx_pos_shift_store ON public.pos_shift USING btree (store_id, opened_at DESC);



CREATE INDEX idx_pos_tender_sale ON public.pos_tender USING btree (sale_id);



CREATE INDEX idx_referral_binding_phone ON public.referral_binding USING btree (customer_phone, active, created_at DESC);



CREATE INDEX idx_reservation_expiry ON public.reservation USING btree (status, expires_at);



CREATE INDEX idx_reservation_order ON public.reservation USING btree (order_id);



CREATE INDEX idx_staff_session_expiry ON public.staff_session USING btree (expires_at);



CREATE INDEX idx_staff_session_staff ON public.staff_session USING btree (staff_id);



CREATE INDEX idx_stock_move_batch ON public.stock_move USING btree (batch_id, occurred_at);



CREATE INDEX idx_stock_move_reference ON public.stock_move USING btree (reference_type, reference_id);



CREATE INDEX idx_stock_transfer_allocation_line ON public.stock_transfer_allocation USING btree (transfer_line_id);



CREATE INDEX idx_stock_transfer_line_transfer ON public.stock_transfer_line USING btree (transfer_id);



CREATE INDEX idx_stocktake_line_stocktake ON public.stocktake_line USING btree (stocktake_id);



CREATE INDEX idx_supplier_stock_feed_supplier ON public.supplier_stock_feed USING btree (supplier_id, updated_at DESC);



CREATE INDEX idx_support_conversation_agent ON public.support_conversation USING btree (agent_id, status, last_message_at DESC);



CREATE INDEX idx_support_conversation_status ON public.support_conversation USING btree (status, created_at);



CREATE INDEX idx_support_message_conversation ON public.support_message USING btree (conversation_id, created_at);



CREATE INDEX idx_support_transfer_conversation ON public.support_transfer USING btree (conversation_id, created_at DESC);



CREATE UNIQUE INDEX integration_config_one_enabled ON public.integration_config USING btree (capability) WHERE enabled;



CREATE INDEX jv_commit_commit_id_idx ON public.jv_commit USING btree (commit_id);



CREATE INDEX jv_commit_property_commit_fk_idx ON public.jv_commit_property USING btree (commit_fk);



CREATE INDEX jv_commit_property_property_name_property_value_idx ON public.jv_commit_property USING btree (property_name, property_value);



CREATE INDEX jv_global_id_local_id_idx ON public.jv_global_id USING btree (local_id);



CREATE INDEX jv_global_id_owner_id_fk_idx ON public.jv_global_id USING btree (owner_id_fk);



CREATE INDEX jv_snapshot_commit_fk_idx ON public.jv_snapshot USING btree (commit_fk);



CREATE INDEX jv_snapshot_global_id_fk_idx ON public.jv_snapshot USING btree (global_id_fk);



CREATE INDEX knowledge_chunk_source_idx ON public.knowledge_chunk USING btree (source_type, source_id);



CREATE INDEX knowledge_chunk_updated_idx ON public.knowledge_chunk USING btree (updated_at DESC);



CREATE INDEX knowledge_embedding_kind_idx ON public.knowledge_embedding USING btree (kind, created_at DESC);



CREATE UNIQUE INDEX market_integration_one_enabled ON public.market_integration USING btree (market_code, capability) WHERE enabled;



CREATE UNIQUE INDEX market_locale_primary_idx ON public.market_locale USING btree (market_code) WHERE is_primary;



CREATE UNIQUE INDEX member_address_default_idx ON public.member_address USING btree (owner_key) WHERE (is_default AND (deleted_at IS NULL));



CREATE INDEX member_address_owner_idx ON public.member_address USING btree (owner_key, deleted_at);



CREATE INDEX member_birthday_idx ON public.member USING btree (birthday) WHERE (birthday IS NOT NULL);



CREATE INDEX member_browse_history_recent_idx ON public.member_browse_history USING btree (member_id, viewed_at DESC);



CREATE INDEX member_coupon_member_idx ON public.member_coupon USING btree (member_id, status, expires_at);



CREATE INDEX member_coupon_member_status_idx ON public.member_coupon USING btree (member_id, status, expires_at);



CREATE INDEX member_coupon_template_idx ON public.member_coupon USING btree (template_id, member_id);



CREATE UNIQUE INDEX member_email_idx ON public.member USING btree (lower((email)::text)) WHERE (email IS NOT NULL);



CREATE INDEX member_favorite_created_idx ON public.member_favorite USING btree (member_id, created_at DESC);



CREATE INDEX member_otp_email_idx ON public.member_otp USING btree (lower((email)::text), created_at DESC) WHERE (email IS NOT NULL);



CREATE INDEX member_otp_phone_idx ON public.member_otp USING btree (phone, created_at DESC);



CREATE INDEX member_session_member_idx ON public.member_session USING btree (member_id, expires_at);



CREATE INDEX order_coupon_allocation_order_idx ON public.order_coupon_allocation USING btree (order_id);



CREATE INDEX order_line_batch_idx ON public.order_line USING btree (batch_id) WHERE (batch_id IS NOT NULL);



CREATE INDEX order_line_created_idx ON public.order_line USING btree (order_id, created_at);



CREATE INDEX order_line_refund_line_idx ON public.order_line_refund USING btree (order_line_id);



CREATE INDEX order_line_refund_order_idx ON public.order_line_refund USING btree (order_id, refunded_at);



CREATE INDEX order_tax_line_order_idx ON public.order_tax_line USING btree (order_id);



CREATE INDEX orders_access_token_idx ON public.orders USING btree (access_token_hash) WHERE (access_token_hash IS NOT NULL);



CREATE INDEX orders_market_created_idx ON public.orders USING btree (market_code, created_at);



CREATE INDEX orders_member_idx ON public.orders USING btree (member_id, created_at DESC);



CREATE INDEX orders_status_created_id_idx ON public.orders USING btree (status, created_at DESC, id DESC);



CREATE UNIQUE INDEX payment_provider_config_active_idx ON public.payment_provider_config USING btree (channel, market_code) WHERE active;



CREATE INDEX payment_reconciliation_kind_idx ON public.payment_reconciliation USING btree (bill_date, kind);



CREATE INDEX pick_line_order_line_order_idx ON public.pick_line_order_line USING btree (order_line_id);



CREATE INDEX points_ledger_fifo_idx ON public.points_ledger USING btree (member_id, created_at) WHERE (((entry_type)::text = 'EARN'::text) AND (remaining_points > 0));



CREATE INDEX product_media_product_idx ON public.product_media USING btree (product_id, role, sort_order);



CREATE UNIQUE INDEX product_media_single_main_idx ON public.product_media USING btree (product_id) WHERE (((role)::text = 'MAIN'::text) AND (sku_id IS NULL));



CREATE INDEX product_media_sku_idx ON public.product_media USING btree (sku_id, role, sort_order);



CREATE INDEX product_review_sku_idx ON public.product_review USING btree (sku_id, created_at DESC);



CREATE INDEX product_review_status_created_idx ON public.product_review USING btree (status, created_at DESC);



CREATE INDEX product_sku_matrix_template_idx ON public.product USING btree (sku_matrix_template_id);



CREATE INDEX promotion_active_channel_idx ON public.promotion USING btree (channel, status, starts_at, ends_at);



CREATE INDEX promotion_active_idx ON public.promotion USING btree (type, status, starts_at, ends_at);



CREATE INDEX promotion_ladder_active_idx ON public.promotion_ladder USING btree (active, kind, priority);



CREATE INDEX promotion_redemption_phone_idx ON public.promotion_redemption USING btree (promotion_id, member_phone);



CREATE INDEX promotion_redemption_release_idx ON public.promotion_redemption USING btree (order_id, released_at);



CREATE INDEX purchase_order_line_purchase_idx ON public.purchase_order_line USING btree (purchase_id);



CREATE INDEX region_lookup_idx ON public.region USING btree (country_code, parent_code, level) WHERE active;



CREATE INDEX risk_alert_created_idx ON public.risk_alert USING btree (created_at DESC);



CREATE INDEX risk_alert_rule_subject_idx ON public.risk_alert USING btree (rule_code, subject, created_at DESC);



CREATE UNIQUE INDEX shipping_rate_band_idx ON public.shipping_rate USING btree (market_code, COALESCE(destination_country, '*'::bpchar), min_weight_grams);



CREATE UNIQUE INDEX sku_combination_unique ON public.sku USING btree (product_id, combination_key) WHERE (combination_key IS NOT NULL);



CREATE INDEX sku_option_value_value_idx ON public.sku_option_value USING btree (option_value_id);



CREATE INDEX sku_price_market_idx ON public.sku_price USING btree (market_code) WHERE active;



CREATE INDEX sku_serial_batch_idx ON public.sku_serial USING btree (batch_id);



CREATE INDEX sku_serial_sku_idx ON public.sku_serial USING btree (sku_id, status);



CREATE INDEX sku_shelf_expiry_idx ON public.sku USING btree (shelf_batch_tracking, shelf_near_expiry_days);



CREATE INDEX stock_cost_movement_sku_idx ON public.stock_cost_movement USING btree (sku_id, occurred_at DESC);



CREATE INDEX stored_value_ledger_member_idx ON public.stored_value_ledger USING btree (member_id, created_at DESC);



CREATE INDEX supplier_nonce_created_idx ON public.supplier_nonce USING btree (created_at);



CREATE INDEX supplier_rate_window_start_idx ON public.supplier_rate_window USING btree (window_start);



CREATE INDEX supplier_return_status_idx ON public.supplier_return USING btree (status, created_at DESC);



CREATE INDEX supplier_return_supplier_idx ON public.supplier_return USING btree (supplier_id, created_at DESC);



CREATE INDEX supplier_settlement_supplier_idx ON public.supplier_settlement USING btree (supplier_id, period_end DESC);



CREATE INDEX supplier_webhook_supplier_idx ON public.supplier_webhook USING btree (supplier_id) WHERE active;



CREATE UNIQUE INDEX tax_rule_jurisdiction_idx ON public.tax_rule USING btree (jurisdiction) WHERE active;



CREATE UNIQUE INDEX uq_pos_sale_idempotency ON public.pos_sale USING btree (store_id, idempotency_key) WHERE (idempotency_key IS NOT NULL);



CREATE UNIQUE INDEX uq_pos_shift_open ON public.pos_shift USING btree (store_id, cashier_name) WHERE ((status)::text = 'OPEN'::text);



CREATE UNIQUE INDEX uq_pos_tender_idempotency ON public.pos_tender USING btree (sale_id, idempotency_key) WHERE (idempotency_key IS NOT NULL);



CREATE INDEX webhook_delivery_due_idx ON public.webhook_delivery USING btree (status, next_attempt_at);



CREATE INDEX withdrawal_promoter_idx ON public.withdrawal USING btree (promoter_id, requested_at DESC);



CREATE INDEX withdrawal_status_idx ON public.withdrawal USING btree (status, requested_at);



CREATE TRIGGER coupon_template_sync_scope_trg AFTER INSERT OR UPDATE OF scope, scope_ids ON public.coupon_template FOR EACH ROW EXECUTE FUNCTION public.coupon_template_sync_scope();



CREATE TRIGGER product_touch_updated_at BEFORE UPDATE ON public.product FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();



CREATE TRIGGER promotion_sync_rule_columns_trg BEFORE INSERT OR UPDATE OF rules ON public.promotion FOR EACH ROW EXECUTE FUNCTION public.promotion_sync_rule_columns();



CREATE TRIGGER sku_shelf_life_sync_trg BEFORE INSERT OR UPDATE OF shelf_life_policy_json ON public.sku FOR EACH ROW EXECUTE FUNCTION public.sku_shelf_life_sync();



CREATE TRIGGER sku_touch_updated_at BEFORE UPDATE ON public.sku FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();



ALTER TABLE ONLY public.address_format
    ADD CONSTRAINT address_format_country_code_fkey FOREIGN KEY (country_code) REFERENCES public.region(code);



ALTER TABLE ONLY public.after_sale_event
    ADD CONSTRAINT after_sale_event_after_sale_id_fkey FOREIGN KEY (after_sale_id) REFERENCES public.after_sale(id);



ALTER TABLE ONLY public.after_sale_line
    ADD CONSTRAINT after_sale_line_after_sale_id_fkey FOREIGN KEY (after_sale_id) REFERENCES public.after_sale(id);



ALTER TABLE ONLY public.after_sale_line
    ADD CONSTRAINT after_sale_line_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES public.order_line(id);



ALTER TABLE ONLY public.after_sale
    ADD CONSTRAINT after_sale_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.ai_gen_job
    ADD CONSTRAINT ai_gen_job_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.product(id);



ALTER TABLE ONLY public.ai_gen_job
    ADD CONSTRAINT ai_gen_job_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES public.ai_provider(id);



ALTER TABLE ONLY public.ai_gen_job
    ADD CONSTRAINT ai_gen_job_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.ai_storyboard
    ADD CONSTRAINT ai_storyboard_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.product(id);



ALTER TABLE ONLY public.ai_storyboard_shot
    ADD CONSTRAINT ai_storyboard_shot_storyboard_id_fkey FOREIGN KEY (storyboard_id) REFERENCES public.ai_storyboard(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.ai_storyboard
    ADD CONSTRAINT ai_storyboard_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.article
    ADD CONSTRAINT article_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.article_category(id);



ALTER TABLE ONLY public.article_translation
    ADD CONSTRAINT article_translation_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.article(id);



ALTER TABLE ONLY public.asset_image_embedding
    ADD CONSTRAINT asset_image_embedding_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.asset(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.asset_locale_variant
    ADD CONSTRAINT asset_locale_variant_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.asset(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.bargain_activity
    ADD CONSTRAINT bargain_activity_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.bargain_help
    ADD CONSTRAINT bargain_help_helper_member_id_fkey FOREIGN KEY (helper_member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.bargain_help
    ADD CONSTRAINT bargain_help_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.bargain_session(id);



ALTER TABLE ONLY public.bargain_session
    ADD CONSTRAINT bargain_session_activity_id_fkey FOREIGN KEY (activity_id) REFERENCES public.bargain_activity(id);



ALTER TABLE ONLY public.bargain_session
    ADD CONSTRAINT bargain_session_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.cart_item
    ADD CONSTRAINT cart_item_cart_id_fkey FOREIGN KEY (cart_id) REFERENCES public.cart(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.cart_item
    ADD CONSTRAINT cart_item_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.category
    ADD CONSTRAINT category_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.category(id);



ALTER TABLE ONLY public.commission_ledger
    ADD CONSTRAINT commission_ledger_commission_id_fkey FOREIGN KEY (commission_id) REFERENCES public.distribution_commission(id);



ALTER TABLE ONLY public.coupon
    ADD CONSTRAINT coupon_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.coupon_redemption
    ADD CONSTRAINT coupon_redemption_coupon_id_fkey FOREIGN KEY (coupon_id) REFERENCES public.coupon(id);



ALTER TABLE ONLY public.coupon_template_scope
    ADD CONSTRAINT coupon_template_scope_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.coupon_template(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.distribution_commission
    ADD CONSTRAINT distribution_commission_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.fulfillment_line
    ADD CONSTRAINT fulfillment_line_fulfillment_order_id_fkey FOREIGN KEY (fulfillment_order_id) REFERENCES public.fulfillment_order(id);



ALTER TABLE ONLY public.fulfillment_order
    ADD CONSTRAINT fulfillment_order_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.goods_receipt_line
    ADD CONSTRAINT goods_receipt_line_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.goods_receipt(id);



ALTER TABLE ONLY public.goods_receipt_line
    ADD CONSTRAINT goods_receipt_line_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.goods_receipt
    ADD CONSTRAINT goods_receipt_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.supplier(id);



ALTER TABLE ONLY public.group_buy_member
    ADD CONSTRAINT group_buy_member_group_buy_id_fkey FOREIGN KEY (group_buy_id) REFERENCES public.group_buy(id);



ALTER TABLE ONLY public.group_buy_member
    ADD CONSTRAINT group_buy_member_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.group_buy
    ADD CONSTRAINT group_buy_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES public.promotion(id);



ALTER TABLE ONLY public.group_buy
    ADD CONSTRAINT group_buy_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.growth_ledger
    ADD CONSTRAINT growth_ledger_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.inventory_batch
    ADD CONSTRAINT inventory_batch_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.legal_acceptance
    ADD CONSTRAINT legal_acceptance_document_version_id_fkey FOREIGN KEY (document_version_id) REFERENCES public.legal_document_version(id);



ALTER TABLE ONLY public.legal_document_localization
    ADD CONSTRAINT legal_document_localization_version_id_fkey FOREIGN KEY (version_id) REFERENCES public.legal_document_version(id);



ALTER TABLE ONLY public.legal_document
    ADD CONSTRAINT legal_document_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code);



ALTER TABLE ONLY public.legal_document_version
    ADD CONSTRAINT legal_document_version_document_id_fkey FOREIGN KEY (document_id) REFERENCES public.legal_document(id);



ALTER TABLE ONLY public.market_destination
    ADD CONSTRAINT market_destination_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code) ON DELETE CASCADE;



ALTER TABLE ONLY public.market_integration
    ADD CONSTRAINT market_integration_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code) ON DELETE CASCADE;



ALTER TABLE ONLY public.market_locale
    ADD CONSTRAINT market_locale_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code) ON DELETE CASCADE;



ALTER TABLE ONLY public.market_setting
    ADD CONSTRAINT market_setting_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code) ON DELETE CASCADE;



ALTER TABLE ONLY public.member_browse_history
    ADD CONSTRAINT member_browse_history_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.member_browse_history
    ADD CONSTRAINT member_browse_history_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.member_card_ledger
    ADD CONSTRAINT member_card_ledger_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.member_coupon
    ADD CONSTRAINT member_coupon_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.member_coupon
    ADD CONSTRAINT member_coupon_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.coupon_template(id);



ALTER TABLE ONLY public.member_favorite
    ADD CONSTRAINT member_favorite_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.member_favorite
    ADD CONSTRAINT member_favorite_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.member_session
    ADD CONSTRAINT member_session_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.order_coupon_allocation
    ADD CONSTRAINT order_coupon_allocation_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.order_coupon_allocation
    ADD CONSTRAINT order_coupon_allocation_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES public.order_line(id);



ALTER TABLE ONLY public.order_line
    ADD CONSTRAINT order_line_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.inventory_batch(id);



ALTER TABLE ONLY public.order_line
    ADD CONSTRAINT order_line_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.order_line_refund
    ADD CONSTRAINT order_line_refund_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.order_line_refund
    ADD CONSTRAINT order_line_refund_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES public.order_line(id);



ALTER TABLE ONLY public.order_tax_line
    ADD CONSTRAINT order_tax_line_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code);



ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_store_id_fkey FOREIGN KEY (store_id) REFERENCES public.store(id);



ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_config_version_id_fkey FOREIGN KEY (config_version_id) REFERENCES public.payment_provider_config_version(id);



ALTER TABLE ONLY public.payment_event
    ADD CONSTRAINT payment_event_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payment(id);



ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.payment_provider_config_version
    ADD CONSTRAINT payment_provider_config_version_config_id_fkey FOREIGN KEY (config_id) REFERENCES public.payment_provider_config(id);



ALTER TABLE ONLY public.pick_line_order_line
    ADD CONSTRAINT pick_line_order_line_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES public.order_line(id);



ALTER TABLE ONLY public.pick_line_order_line
    ADD CONSTRAINT pick_line_order_line_pick_line_id_fkey FOREIGN KEY (pick_line_id) REFERENCES public.pick_task_line(id);



ALTER TABLE ONLY public.pick_task
    ADD CONSTRAINT pick_task_fulfillment_order_id_fkey FOREIGN KEY (fulfillment_order_id) REFERENCES public.fulfillment_order(id);



ALTER TABLE ONLY public.pick_task_line
    ADD CONSTRAINT pick_task_line_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES public.order_line(id);



ALTER TABLE ONLY public.pick_task_line
    ADD CONSTRAINT pick_task_line_pick_task_id_fkey FOREIGN KEY (pick_task_id) REFERENCES public.pick_task(id);



ALTER TABLE ONLY public.pick_task_line
    ADD CONSTRAINT pick_task_line_suggested_batch_id_fkey FOREIGN KEY (suggested_batch_id) REFERENCES public.inventory_batch(id);



ALTER TABLE ONLY public.pick_task
    ADD CONSTRAINT pick_task_store_id_fkey FOREIGN KEY (store_id) REFERENCES public.store(id);



ALTER TABLE ONLY public.pickup
    ADD CONSTRAINT pickup_fulfillment_order_id_fkey FOREIGN KEY (fulfillment_order_id) REFERENCES public.fulfillment_order(id);



ALTER TABLE ONLY public.pickup
    ADD CONSTRAINT pickup_store_id_fkey FOREIGN KEY (store_id) REFERENCES public.store(id);



ALTER TABLE ONLY public.points_ledger
    ADD CONSTRAINT points_ledger_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.pos_cash_movement
    ADD CONSTRAINT pos_cash_movement_shift_id_fkey FOREIGN KEY (shift_id) REFERENCES public.pos_shift(id);



ALTER TABLE ONLY public.pos_reconciliation
    ADD CONSTRAINT pos_reconciliation_store_id_fkey FOREIGN KEY (store_id) REFERENCES public.store(id);



ALTER TABLE ONLY public.pos_refund_allocation
    ADD CONSTRAINT pos_refund_allocation_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.inventory_batch(id);



ALTER TABLE ONLY public.pos_refund_allocation
    ADD CONSTRAINT pos_refund_allocation_refund_line_id_fkey FOREIGN KEY (refund_line_id) REFERENCES public.pos_refund_line(id);



ALTER TABLE ONLY public.pos_refund_line
    ADD CONSTRAINT pos_refund_line_refund_id_fkey FOREIGN KEY (refund_id) REFERENCES public.pos_refund(id);



ALTER TABLE ONLY public.pos_refund_line
    ADD CONSTRAINT pos_refund_line_sale_line_id_fkey FOREIGN KEY (sale_line_id) REFERENCES public.pos_sale_line(id);



ALTER TABLE ONLY public.pos_refund
    ADD CONSTRAINT pos_refund_sale_id_fkey FOREIGN KEY (sale_id) REFERENCES public.pos_sale(id);



ALTER TABLE ONLY public.pos_refund_tender
    ADD CONSTRAINT pos_refund_tender_refund_id_fkey FOREIGN KEY (refund_id) REFERENCES public.pos_refund(id);



ALTER TABLE ONLY public.pos_refund_tender
    ADD CONSTRAINT pos_refund_tender_tender_id_fkey FOREIGN KEY (tender_id) REFERENCES public.pos_tender(id);



ALTER TABLE ONLY public.pos_sale_allocation
    ADD CONSTRAINT pos_sale_allocation_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.inventory_batch(id);



ALTER TABLE ONLY public.pos_sale_allocation
    ADD CONSTRAINT pos_sale_allocation_sale_line_id_fkey FOREIGN KEY (sale_line_id) REFERENCES public.pos_sale_line(id);



ALTER TABLE ONLY public.pos_sale
    ADD CONSTRAINT pos_sale_coupon_id_fkey FOREIGN KEY (coupon_id) REFERENCES public.coupon(id);



ALTER TABLE ONLY public.pos_sale_line
    ADD CONSTRAINT pos_sale_line_sale_id_fkey FOREIGN KEY (sale_id) REFERENCES public.pos_sale(id);



ALTER TABLE ONLY public.pos_sale
    ADD CONSTRAINT pos_sale_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.pos_sale
    ADD CONSTRAINT pos_sale_shift_id_fkey FOREIGN KEY (shift_id) REFERENCES public.pos_shift(id);



ALTER TABLE ONLY public.pos_sale
    ADD CONSTRAINT pos_sale_store_id_fkey FOREIGN KEY (store_id) REFERENCES public.store(id);



ALTER TABLE ONLY public.pos_shift
    ADD CONSTRAINT pos_shift_store_id_fkey FOREIGN KEY (store_id) REFERENCES public.store(id);



ALTER TABLE ONLY public.pos_tender
    ADD CONSTRAINT pos_tender_sale_id_fkey FOREIGN KEY (sale_id) REFERENCES public.pos_sale(id);



ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.category(id);



ALTER TABLE ONLY public.product_media
    ADD CONSTRAINT product_media_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.product_media
    ADD CONSTRAINT product_media_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.product_option
    ADD CONSTRAINT product_option_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.product(id);



ALTER TABLE ONLY public.product_option_value
    ADD CONSTRAINT product_option_value_option_id_fkey FOREIGN KEY (option_id) REFERENCES public.product_option(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.product_review
    ADD CONSTRAINT product_review_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.product_review
    ADD CONSTRAINT product_review_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.product_review
    ADD CONSTRAINT product_review_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_sku_matrix_template_id_fkey FOREIGN KEY (sku_matrix_template_id) REFERENCES public.sku_matrix_template(id);



ALTER TABLE ONLY public.product_translation
    ADD CONSTRAINT product_translation_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.product(id);



ALTER TABLE ONLY public.promotion_ladder
    ADD CONSTRAINT promotion_ladder_gift_sku_id_fkey FOREIGN KEY (gift_sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.promotion_redemption
    ADD CONSTRAINT promotion_redemption_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.promotion_redemption
    ADD CONSTRAINT promotion_redemption_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES public.promotion(id);



ALTER TABLE ONLY public.purchase_order_line
    ADD CONSTRAINT purchase_order_line_purchase_id_fkey FOREIGN KEY (purchase_id) REFERENCES public.purchase_order(id);



ALTER TABLE ONLY public.purchase_order_line
    ADD CONSTRAINT purchase_order_line_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.purchase_order
    ADD CONSTRAINT purchase_order_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.supplier(id);



ALTER TABLE ONLY public.region
    ADD CONSTRAINT region_parent_code_fkey FOREIGN KEY (parent_code) REFERENCES public.region(code);



ALTER TABLE ONLY public.reservation
    ADD CONSTRAINT reservation_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.inventory_batch(id);



ALTER TABLE ONLY public.role_permission
    ADD CONSTRAINT role_permission_permission_code_fkey FOREIGN KEY (permission_code) REFERENCES public.permission(code);



ALTER TABLE ONLY public.shipping_rate
    ADD CONSTRAINT shipping_rate_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code) ON DELETE CASCADE;



ALTER TABLE ONLY public.sku_cost
    ADD CONSTRAINT sku_cost_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.sku_matrix_dimension
    ADD CONSTRAINT sku_matrix_dimension_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.sku_matrix_template(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.sku_matrix_dimension_value
    ADD CONSTRAINT sku_matrix_dimension_value_dimension_id_fkey FOREIGN KEY (dimension_id) REFERENCES public.sku_matrix_dimension(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.sku_matrix_disabled
    ADD CONSTRAINT sku_matrix_disabled_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.sku_matrix_template(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.sku_option_value
    ADD CONSTRAINT sku_option_value_option_id_fkey FOREIGN KEY (option_id) REFERENCES public.product_option(id);



ALTER TABLE ONLY public.sku_option_value
    ADD CONSTRAINT sku_option_value_option_value_id_fkey FOREIGN KEY (option_value_id) REFERENCES public.product_option_value(id);



ALTER TABLE ONLY public.sku_option_value
    ADD CONSTRAINT sku_option_value_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.sku_price
    ADD CONSTRAINT sku_price_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code) ON DELETE CASCADE;



ALTER TABLE ONLY public.sku_price
    ADD CONSTRAINT sku_price_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.sku
    ADD CONSTRAINT sku_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.product(id);



ALTER TABLE ONLY public.sku_serial
    ADD CONSTRAINT sku_serial_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.inventory_batch(id);



ALTER TABLE ONLY public.sku_serial
    ADD CONSTRAINT sku_serial_order_line_id_fkey FOREIGN KEY (order_line_id) REFERENCES public.order_line(id);



ALTER TABLE ONLY public.sku_serial
    ADD CONSTRAINT sku_serial_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.staff_session
    ADD CONSTRAINT staff_session_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff_user(id);



ALTER TABLE ONLY public.staff_store
    ADD CONSTRAINT staff_store_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff_user(id);



ALTER TABLE ONLY public.staff_store
    ADD CONSTRAINT staff_store_store_id_fkey FOREIGN KEY (store_id) REFERENCES public.store(id);



ALTER TABLE ONLY public.stock_cost_movement
    ADD CONSTRAINT stock_cost_movement_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.stock_move
    ADD CONSTRAINT stock_move_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.inventory_batch(id);



ALTER TABLE ONLY public.stock_transfer_allocation
    ADD CONSTRAINT stock_transfer_allocation_source_batch_id_fkey FOREIGN KEY (source_batch_id) REFERENCES public.inventory_batch(id);



ALTER TABLE ONLY public.stock_transfer_allocation
    ADD CONSTRAINT stock_transfer_allocation_transfer_line_id_fkey FOREIGN KEY (transfer_line_id) REFERENCES public.stock_transfer_line(id);



ALTER TABLE ONLY public.stock_transfer_line
    ADD CONSTRAINT stock_transfer_line_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.stock_transfer_line
    ADD CONSTRAINT stock_transfer_line_transfer_id_fkey FOREIGN KEY (transfer_id) REFERENCES public.stock_transfer(id);



ALTER TABLE ONLY public.stocktake_line
    ADD CONSTRAINT stocktake_line_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.inventory_batch(id);



ALTER TABLE ONLY public.stocktake_line
    ADD CONSTRAINT stocktake_line_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.stocktake_line
    ADD CONSTRAINT stocktake_line_stocktake_id_fkey FOREIGN KEY (stocktake_id) REFERENCES public.stocktake(id);



ALTER TABLE ONLY public.store
    ADD CONSTRAINT store_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code);



ALTER TABLE ONLY public.stored_value_ledger
    ADD CONSTRAINT stored_value_ledger_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);



ALTER TABLE ONLY public.supplier_credential
    ADD CONSTRAINT supplier_credential_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.supplier(id);



ALTER TABLE ONLY public.supplier_return
    ADD CONSTRAINT supplier_return_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);



ALTER TABLE ONLY public.supplier_settlement
    ADD CONSTRAINT supplier_settlement_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.supplier(id);



ALTER TABLE ONLY public.supplier_sku
    ADD CONSTRAINT supplier_sku_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.supplier_sku
    ADD CONSTRAINT supplier_sku_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.supplier(id);



ALTER TABLE ONLY public.supplier_stock_feed
    ADD CONSTRAINT supplier_stock_feed_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.sku(id);



ALTER TABLE ONLY public.supplier_stock_feed
    ADD CONSTRAINT supplier_stock_feed_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.supplier(id);



ALTER TABLE ONLY public.support_message
    ADD CONSTRAINT support_message_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.support_conversation(id);



ALTER TABLE ONLY public.support_transfer
    ADD CONSTRAINT support_transfer_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.support_conversation(id);



ALTER TABLE ONLY public.warehouse
    ADD CONSTRAINT warehouse_market_code_fkey FOREIGN KEY (market_code) REFERENCES public.market(code);



ALTER TABLE ONLY public.webhook_delivery
    ADD CONSTRAINT webhook_delivery_webhook_id_fkey FOREIGN KEY (webhook_id) REFERENCES public.supplier_webhook(id) ON DELETE CASCADE;




ALTER TABLE ONLY public.staff_user
    ADD CONSTRAINT staff_user_role_fk FOREIGN KEY (role) REFERENCES public.role(code);

CREATE UNIQUE INDEX asset_sha256_key ON public.asset USING btree (sha256) WHERE (sha256 IS NOT NULL);

CREATE TABLE public.asset_object (
    id uuid NOT NULL,
    asset_id uuid NOT NULL,
    storage character varying(32) NOT NULL,
    object_key character varying(512) NOT NULL,
    url character varying(1024) NOT NULL,
    bytes bigint NOT NULL,
    content_type character varying(128),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY public.asset_object
    ADD CONSTRAINT asset_object_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.asset_object
    ADD CONSTRAINT asset_object_asset_fk FOREIGN KEY (asset_id) REFERENCES public.asset(id) ON DELETE CASCADE;

CREATE INDEX asset_object_asset_idx ON public.asset_object USING btree (asset_id);
