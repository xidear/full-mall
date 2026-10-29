package com.fullmall.capability.api;

import com.fullmall.capability.app.MarketCapabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 市场能力查询（门禁的"可展示"版本）。
 *
 * <p>前台用它决定"这个国家能不能选、结算按钮能不能点"，后台用它看"这个市场还缺什么配置"——
 * 两边读同一份判断，避免前台能选、下单才报错。
 */
@RestController
public class CapabilityController {

    private final MarketCapabilityService capabilities;

    public CapabilityController(MarketCapabilityService capabilities) {
        this.capabilities = capabilities;
    }

    @GetMapping({"/api/v1/markets/{code}/capabilities", "/admin/v1/markets/{code}/capabilities"})
    public MarketCapabilityService.Capabilities evaluate(@PathVariable String code,
                                                       @RequestParam(required = false) String destination) {
        return capabilities.evaluate(code, destination);
    }
}
