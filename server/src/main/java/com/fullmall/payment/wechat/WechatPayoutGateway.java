package com.fullmall.payment.wechat;

import com.fullmall.distribution.app.PayoutGateway;

/** 微信商家转账适配器（实现分布模块的打款端口）。 */
public class WechatPayoutGateway implements PayoutGateway {

    private final WechatTransferClient client;

    public WechatPayoutGateway(WechatTransferClient client) {
        this.client = client;
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public Result transfer(String batchNo, String openId, long amountFen, String remark) {
        var transfer = client.transfer(batchNo, openId, amountFen, remark);
        return new Result(transfer.batchId(), transfer.status());
    }
}
