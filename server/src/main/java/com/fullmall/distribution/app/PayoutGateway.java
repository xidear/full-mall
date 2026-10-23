package com.fullmall.distribution.app;

/**
 * 打款端口（提现用）。实现放在支付模块，分布模块只依赖本接口，避免模块循环依赖。
 */
public interface PayoutGateway {

    boolean configured();

    record Result(String reference, String status) {
    }

    /** 打款；失败抛错，不返回"成功"。 */
    Result transfer(String batchNo, String openId, long amountFen, String remark);
}
