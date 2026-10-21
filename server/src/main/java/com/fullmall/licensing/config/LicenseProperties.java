package com.fullmall.licensing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 许可配置。默认关闭：开源/开发环境不校验，打包时按客户启用。 */
@ConfigurationProperties(prefix = "app.license")
public class LicenseProperties {

    private boolean enabled = false;
    /** 许可证文件路径（license.json），支持 file: 前缀 */
    private String file;
    /** Ed25519 公钥（Base64，X.509 SubjectPublicKeyInfo） */
    private String publicKey;
    /** 是否校验域名 */
    private boolean domainCheck = true;
    /** 时钟回拨容忍小时数 */
    private int clockSkewHours = 24;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isDomainCheck() {
        return domainCheck;
    }

    public void setDomainCheck(boolean domainCheck) {
        this.domainCheck = domainCheck;
    }

    public int getClockSkewHours() {
        return clockSkewHours;
    }

    public void setClockSkewHours(int clockSkewHours) {
        this.clockSkewHours = clockSkewHours;
    }
}
