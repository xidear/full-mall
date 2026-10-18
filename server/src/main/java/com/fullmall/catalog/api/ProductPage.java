package com.fullmall.catalog.api;

import com.fullmall.catalog.domain.Product;

import java.util.List;

public record ProductPage(List<Product> items, int page, int size, long total) {
}