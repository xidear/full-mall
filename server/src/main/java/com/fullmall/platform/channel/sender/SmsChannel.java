package com.fullmall.platform.channel.sender;

/**
 * 短信发送端口：只认识「手机号 + 文本」，不认识任何云厂商类型，
 * 因此换供应商（或加第三家）不会波及业务代码。
 */
public interface SmsChannel {

    void send(String phone, String text);
}
