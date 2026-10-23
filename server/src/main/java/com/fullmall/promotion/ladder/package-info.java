/**
 * 阶梯促销（满赠 / 第二件半价）：对外只暴露 {@link com.fullmall.promotion.ladder.PromotionLadderService}，
 * 供 order 模块在结算时调用。
 */
@org.springframework.modulith.NamedInterface("promotion-ladder")
package com.fullmall.promotion.ladder;
