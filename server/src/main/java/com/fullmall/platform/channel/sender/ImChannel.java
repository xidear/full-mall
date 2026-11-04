package com.fullmall.platform.channel.sender;

/** 告警群机器人端口（企微 / 飞书 / 钉钉）：只认识一段文本。 */
public interface ImChannel {

    void send(String text);
}
