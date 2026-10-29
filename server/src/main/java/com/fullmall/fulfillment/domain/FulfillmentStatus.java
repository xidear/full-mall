package com.fullmall.fulfillment.domain;

public enum FulfillmentStatus {
    PENDING,
    ALLOCATED,
    ACCEPTED,
    PICKING,
    PACKED,
    SHIPPED,
    /** 跨境清关中：已交承运商、尚未完成目的国清关（跨境订单才有；国内订单不会进入这两个状态） */
    CUSTOMS_CLEARING,
    /** 清关完成：可继续走签收/完成 */
    CUSTOMS_CLEARED,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED
}