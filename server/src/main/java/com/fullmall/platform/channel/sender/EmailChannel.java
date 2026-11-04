package com.fullmall.platform.channel.sender;

/** 邮件发送端口：只认识「收件地址 + 主题 + 正文」。 */
public interface EmailChannel {

    void send(String to, String subject, String body);
}
