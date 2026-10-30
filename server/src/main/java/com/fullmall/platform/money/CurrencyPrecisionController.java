package com.fullmall.platform.money;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 币种小数位下发：前端换算金额时按币种处理，禁止写死 ×100。 */
@RestController
public class CurrencyPrecisionController {

    @GetMapping("/api/v1/currencies/precision")
    public Map<String, Object> precision() {
        return CurrencyPrecision.view();
    }
}
