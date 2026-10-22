package com.fullmall.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 列表深翻保护：`page` 超过上限直接 400。
 *
 * <p>列表接口用 `OFFSET`，页深越大扫描越多（H22）。先加一道明确拒绝的上限，
 * 避免脚本翻到第 10 万页把库拖垮。这是保护，不是修复——keyset 分页仍待做。
 */
@Component
@Order(-40)
public class PaginationDepthGuardFilter extends OncePerRequestFilter {

    private final int maxPage;

    public PaginationDepthGuardFilter(@Value("${app.pagination.max-page:500}") int maxPage) {
        if (maxPage <= 0) {
            throw new IllegalArgumentException("app.pagination.max-page 必须大于 0，实际=" + maxPage);
        }
        this.maxPage = maxPage;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String rawPage = request.getParameter("page");
        if (rawPage != null && !rawPage.isBlank()) {
            int page;
            try {
                page = Integer.parseInt(rawPage.trim());
            } catch (NumberFormatException error) {
                reject(response, "page 必须是整数：" + rawPage);
                return;
            }
            if (page < 0) {
                reject(response, "page 不能为负：" + page);
                return;
            }
            if (page > maxPage) {
                reject(response, "页深超过上限 " + maxPage + "（当前 " + page + "）"
                        + "，请缩小筛选范围或按时间倒序分批导出");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(400);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"PAGE_DEPTH_EXCEEDED\",\"message\":\""
                + message.replace("\"", "'") + "\"}");
    }
}
