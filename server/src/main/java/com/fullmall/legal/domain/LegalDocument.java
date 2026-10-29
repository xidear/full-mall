package com.fullmall.legal.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 法律文件（条款/隐私/退款政策）的"已发布版本快照"（domain 层，纯数据，无行为、线程安全）。
 *
 * <p>身份是 {@code (type, locale, market, version)}：同一份条款在不同市场/语言可以同时有多个版本，
 * 下单时记下的 acceptance 必须能唯一定位到用户当时看到的这一份，所以版本号与生效时间一起输出。
 *
 * <p>{@code contentHash} 是正文摘要，用于证明"用户接受的就是这段文字"：正文只做格式修正也要换 hash，
 * 否则事后无法区分"改过错别字"与"改过条款"。
 */
public record LegalDocument(
        UUID id,
        String type,
        String version,
        String locale,
        String market,
        String title,
        String content,
        String contentHash,
        Instant effectiveFrom,
        /** null 表示长期有效；有值时代表该版本的失效时间。 */
        Instant effectiveTo,
        String pdfUrl
) {
}