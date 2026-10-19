package com.fullmall.platform.importing;

import java.util.List;

/**
 * 导入校验失败（SUP-021）：携带**全部**行级错误，而不是遇到第一个就停。
 *
 * <p>这是受检的业务失败，调用方必须把 `errors` 原样返回给用户；
 * 不允许只回“导入失败”这类无定位信息。
 */
public class CsvImportException extends RuntimeException {

    private final List<CsvRowError> errors;

    public CsvImportException(String message, List<CsvRowError> errors) {
        super(message);
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("CsvImportException 必须携带至少一条行级错误");
        }
        this.errors = List.copyOf(errors);
    }

    public List<CsvRowError> errors() {
        return errors;
    }
}