package com.fullmall.analytics.api;

import com.fullmall.analytics.tool.AiReadOnlyTools;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** AI 只读工具：列清单与调用（仅后台，全部只读）。 */
@RestController
@RequestMapping("/admin/v1/ai/tools")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AiToolController {

    private final AiReadOnlyTools tools;

    public AiToolController(AiReadOnlyTools tools) {
        this.tools = tools;
    }

    @GetMapping
    public List<AiReadOnlyTools.ToolSpec> list() {
        return tools.list();
    }

    @PostMapping("/{name}")
    public Map<String, Object> invoke(@PathVariable String name,
                                      @RequestBody(required = false) Map<String, String> args) {
        return tools.invoke(name, args);
    }
}
