package com.fullmall.platform.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 启动时把 `app.timezone` 应用到订单号生成器。
 *
 * <p>不配则沿用系统时区（服务器 `TZ`）；配了非法值会启动失败——宁可不启动，
 * 也不要生成时间跳变的订单号。
 */
@Component
public class IdZoneConfiguration {

    public IdZoneConfiguration(@Value("${app.timezone:}") String zoneId) {
        Ids.configureZone(zoneId);
    }
}
