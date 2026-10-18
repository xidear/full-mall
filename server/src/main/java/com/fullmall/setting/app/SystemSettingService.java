package com.fullmall.setting.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.setting.infra.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class SystemSettingService {

    private static final Set<String> PUBLIC_KEYS = Set.of(
            "site_name", "company_name", "icp_number", "public_security_record",
            "service_phone", "service_email", "site_status", "maintenance_message",
            "default_locale", "default_currency");

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "site_name", "company_name", "icp_number", "public_security_record",
            "service_phone", "service_email", "site_status", "maintenance_message",
            "default_locale", "default_currency",
            "storage_provider", "sms_provider", "payment_provider", "logistics_provider");

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    public Map<String, String> publicSettings() {
        Map<String, String> all = repository.findAll();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : all.entrySet()) {
            if (PUBLIC_KEYS.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, String> adminSettings() {
        return repository.findAll();
    }

    @Transactional
    public Map<String, String> save(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!ALLOWED_KEYS.contains(entry.getKey())) {
                throw ApiException.badRequest("INVALID_SETTING_KEY", "不支持的配置项: " + entry.getKey());
            }
            repository.upsert(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().trim());
        }
        return adminSettings();
    }
}