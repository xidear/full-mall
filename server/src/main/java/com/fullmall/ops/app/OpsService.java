package com.fullmall.ops.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运维聚合：主机/容器由各机器上的 ops-agent 提供，SSL 由 ssl-helper 提供。
 *
 * <p>设计原则：多台机器各自跑一个只读 agent，admin 只做聚合，不假设“所有容器都在本机”。
 * 单个 agent 故障必须显式返回 ok=false，不能悄悄从列表消失。
 */
@Service
public class OpsService {

    public record Agent(String name, String url) {
    }

    public record CallResult(boolean ok, JsonNode body, String error) {
    }

    private final ObjectMapper mapper;
    private final HttpClient http;
    private final List<Agent> agents;
    private final String token;
    private final String sslHelperUrl;
    private final Duration requestTimeout;

    public OpsService(ObjectMapper mapper,
                      @Value("${app.ops.agents:local=http://127.0.0.1:41287}") String agentsConfig,
                      @Value("${app.ops.token:}") String token,
                      @Value("${app.ssl.helper-url:http://127.0.0.1:41287}") String sslHelperUrl,
                      @Value("${app.ops.request-timeout-ms:15000}") long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("app.ops.request-timeout-ms 必须大于 0，实际=" + timeoutMs);
        }
        this.mapper = mapper;
        this.token = token == null ? "" : token.trim();
        this.sslHelperUrl = stripTrailingSlash(sslHelperUrl);
        this.requestTimeout = Duration.ofMillis(timeoutMs);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 3000)))
                .build();
        this.agents = parseAgents(agentsConfig);
    }

    public List<Agent> agents() {
        return agents;
    }

    public String sslHelperUrl() {
        return sslHelperUrl;
    }

    public Map<String, Object> overview() {
        List<Map<String, Object>> hosts = new ArrayList<>();
        int containers = 0;
        int running = 0;
        int warnings = 0;
        for (Agent agent : agents) {
            CallResult result = call(agent.url(), "/api/ops", "GET", null);
            Map<String, Object> host = new LinkedHashMap<>();
            host.put("name", agent.name());
            host.put("url", agent.url());
            host.put("ok", result.ok());
            if (result.ok()) {
                JsonNode body = result.body();
                host.put("host", json(body.path("host")));
                host.put("docker", json(body.path("docker")));
                host.put("containers", json(body.path("containers")));
                host.put("security", json(body.path("security")));
                containers += body.path("containers").size();
                for (JsonNode c : body.path("containers")) {
                    if ("running".equals(c.path("state").asText())) {
                        running++;
                    }
                    if (c.path("securityWarnings").size() > 0) {
                        warnings += c.path("securityWarnings").size();
                    }
                }
            } else {
                host.put("error", result.error());
            }
            hosts.add(host);
        }
        CallResult ssl = call(sslHelperUrl, "/api/ssl", "GET", null);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hosts", agents.size());
        summary.put("hostsOnline", hosts.stream().filter(h -> Boolean.TRUE.equals(h.get("ok"))).count());
        summary.put("containers", containers);
        summary.put("containersRunning", running);
        summary.put("securityWarnings", warnings);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedAt", Instant.now().toString());
        result.put("hosts", hosts);
        result.put("ssl", ssl.ok() ? json(ssl.body()) : Map.of("ok", false, "error", ssl.error()));
        result.put("summary", summary);
        result.put("stack", stack());
        return result;
    }

    public Map<String, Object> containers() {
        List<Map<String, Object>> hosts = new ArrayList<>();
        for (Agent agent : agents) {
            CallResult result = call(agent.url(), "/api/ops", "GET", null);
            hosts.add(hostEnvelope(agent, result, "containers"));
        }
        return Map.of("hosts", hosts);
    }

    public Map<String, Object> security() {
        List<Map<String, Object>> hosts = new ArrayList<>();
        for (Agent agent : agents) {
            CallResult result = call(agent.url(), "/api/ops", "GET", null);
            hosts.add(hostEnvelope(agent, result, "security"));
        }
        return Map.of("hosts", hosts);
    }

    public Map<String, Object> ssl() {
        CallResult result = call(sslHelperUrl, "/api/ssl", "GET", null);
        return require(result, "SSL 辅助容器 " + sslHelperUrl);
    }

    public Map<String, Object> renewSsl(Map<String, Object> payload) {
        // acme.sh 可能等待 HTTP-01 challenge 数十秒；不能套用 ops 概览的 15s 超时
        CallResult result = call(sslHelperUrl, "/api/ssl/renew", "POST",
                payload == null ? Map.of() : payload, Duration.ofSeconds(180));
        return require(result, "SSL 续签 " + sslHelperUrl);
    }

    public Map<String, Object> containerAction(String host, String id, String action) {
        Agent agent = agents.stream()
                .filter(item -> item.name().equals(host))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知运维主机: " + host));
        if (!List.of("start", "stop", "restart").contains(action)) {
            throw new IllegalArgumentException("不支持的容器操作: " + action);
        }
        CallResult result = call(agent.url(),
                "/api/containers/" + urlEncode(id) + "/" + action, "POST", Map.of());
        return require(result, "容器 " + id + " 操作 " + action);
    }

    private Map<String, Object> hostEnvelope(Agent agent, CallResult result, String section) {
        Map<String, Object> host = new LinkedHashMap<>();
        host.put("name", agent.name());
        host.put("url", agent.url());
        host.put("ok", result.ok());
        if (result.ok()) {
            host.put("host", json(result.body().path("host")));
            host.put("docker", json(result.body().path("docker")));
            host.put(section, json(result.body().path(section)));
        } else {
            host.put("error", result.error());
        }
        return host;
    }

    private Map<String, Object> require(CallResult result, String operation) {
        if (!result.ok()) {
            throw new IllegalStateException(operation + " 失败: " + result.error());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("data", json(result.body()));
        return body;
    }

    private CallResult call(String baseUrl, String path, String method, Object payload) {
        return call(baseUrl, path, method, payload, requestTimeout);
    }

    private CallResult call(String baseUrl, String path, String method, Object payload, Duration timeout) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(timeout)
                    .header("Accept", "application/json");
            if (!token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }
            if ("POST".equals(method)) {
                String json = mapper.writeValueAsString(payload == null ? Map.of() : payload);
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json));
            } else {
                builder.GET();
            }
            HttpResponse<String> response = http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new CallResult(false, null,
                        "HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
            if (response.body() == null || response.body().isBlank()) {
                return new CallResult(false, null, "响应为空");
            }
            return new CallResult(true, mapper.readTree(response.body()), null);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return new CallResult(false, null, "请求被中断: " + error.getMessage());
        } catch (Exception error) {
            return new CallResult(false, null,
                    error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private static List<Agent> parseAgents(String raw) {
        List<Agent> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String item : raw.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            String name;
            String url;
            if (eq < 0) {
                name = "host-" + (result.size() + 1);
                url = trimmed;
            } else {
                name = trimmed.substring(0, eq).trim();
                url = trimmed.substring(eq + 1).trim();
            }
            if (name.isEmpty() || url.isEmpty()) {
                throw new IllegalArgumentException("app.ops.agents 格式应为 name=url，实际=" + raw);
            }
            result.add(new Agent(name, stripTrailingSlash(url)));
        }
        return List.copyOf(result);
    }

    /** Boot 的 Web 层可能使用 Jackson 3，而这里的 ObjectMapper 是 Jackson 2；显式转成 Map/List 再输出。 */
    private Object json(JsonNode node) {
        return mapper.convertValue(node, Object.class);
    }

    private static String stripTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }

    /** 技术栈清单：与 pom / package.json / compose 对齐，便于运维一眼核对。 */
    private List<Map<String, String>> stack() {
        return List.of(
                item("运行时", "Java", "21 (LTS)"),
                item("后端", "Spring Boot", "3.x / Spring Modulith"),
                item("数据库", "PostgreSQL", "17"),
                item("缓存", "Valkey", "8"),
                item("消息", "RabbitMQ", "4"),
                item("搜索", "OpenSearch", "2"),
                item("对象存储", "SeaweedFS", "3.80"),
                item("前端", "Nuxt 3 + Vue 3", "storefront"),
                item("后台", "Vue 3 + Element Plus", "admin"),
                item("边缘", "Nginx + acme.sh", "SSL/反代"),
                item("容器", "Docker Engine", "Compose v2")
        );
    }

    private static Map<String, String> item(String layer, String name, String version) {
        Map<String, String> value = new LinkedHashMap<>();
        value.put("layer", layer);
        value.put("name", name);
        value.put("version", version);
        return value;
    }
}
