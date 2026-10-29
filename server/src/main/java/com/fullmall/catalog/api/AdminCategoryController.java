package com.fullmall.catalog.api;

import com.fullmall.catalog.domain.Category;
import com.fullmall.catalog.infra.CategoryRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台类目下拉（与顾客端 /api/v1/categories 同一数据源）。
 *
 * <p>为什么要单独开一个后台路径：admin 的请求会带 staff token，而 /api/v1/** 由 MemberAuthFilter
 * 处理 Bearer 头——staff token 不是会员 token，会被判为无效并返回 401
 * （"登录已失效，请重新登录"）。后台用后台接口，避免"公共接口 + 员工 token"这种混用。
 */
@RestController
@RequestMapping("/admin/v1/categories")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;

    public AdminCategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<Category> list() {
        return categoryRepository.findActive();
    }
}
