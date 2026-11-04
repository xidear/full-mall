package com.fullmall.platform.channel.api;

import com.fullmall.platform.channel.ChannelSendService;
import com.fullmall.platform.channel.IntegrationRouteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通道路由（ADR-0018）：哪个业务动作用哪家供应商。
 *
 * <p>候选（能力/用途/供应商）全部来自后端清单：后台只渲染勾选矩阵，运营不需要知道服务商标识。
 * 写入只允许 ADMIN；读取允许 OPERATOR（排障要看「哪个通道没绑」）。
 */
@RestController
@RequestMapping("/admin/v1/channel-routes")
public class ChannelRouteController {

    private final IntegrationRouteService routes;
    private final ChannelSendService channels;

    public ChannelRouteController(IntegrationRouteService routes, ChannelSendService channels) {
        this.routes = routes;
        this.channels = channels;
    }

    public record BindRequest(String provider, String recipient) {
    }

    public record TestRequest(String capability, String usageKey, String recipient) {
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<IntegrationRouteService.UsageView> matrix() {
        return routes.matrix();
    }

    @PutMapping("/{capability}/{usageKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public IntegrationRouteService.Route bind(@PathVariable String capability,
                                              @PathVariable String usageKey,
                                              @RequestBody BindRequest body) {
        return routes.bind(capability, usageKey, body.provider(), body.recipient());
    }

    @DeleteMapping("/{capability}/{usageKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public void unbind(@PathVariable String capability, @PathVariable String usageKey) {
        routes.unbind(capability, usageKey);
    }

    /**
     * 发一条测试消息：运营改完配置必须能当场验证「到底能不能发出去」。
     * 返回实际使用的供应商；演示模式会标明 mock（不会真的发出去）。
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ChannelSendService.Result test(@RequestBody TestRequest body) {
        String usageKey = body.usageKey();
        String recipient = body.recipient();
        var route = routes.find(body.capability(), usageKey).orElse(null);
        if (recipient == null || recipient.isBlank()) {
            recipient = route == null ? null : route.recipient();
        }
        return channels.send(body.capability(), usageKey, recipient,
                "通道连通性测试", "这是一条测试消息，收到说明该通道可用。");
    }
}
