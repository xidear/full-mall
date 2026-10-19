package com.fullmall.platform.importing;

import java.util.List;

/**
 * CSV 导入的行级错误（SUP-021）：**行号 + 字段 + 原因**。
 *
 * <p>行号按文件真实行号（含表头，从 1 开始），便于运营直接定位。
 */
public record CsvRowError(int rowNumber, String field, String reason) {
}