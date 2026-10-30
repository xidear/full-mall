package com.fullmall.asset.domain;

import com.fullmall.platform.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 素材库条目（P7 能力切片，同时作为 BaseEntity + Scope + CRUD 基类的范式试点）。
 */
@Entity
@Table(name = "asset")
public class AssetEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "kind", nullable = false, length = 32)
    private String kind;

    @Column(name = "owner_username", nullable = false, length = 64)
    private String ownerUsername;

    /** 素材用途：PRODUCT/SPEC/VIDEO/AI_GENERATED 进素材库；OTHER_SYSTEM 给审批附件等。 */
    @Column(name = "purpose", length = 24)
    private String purpose;

    /** 来源：UPLOAD/AI/IMPORT/E2E。E2E 是自动化测试产物，选择器默认不展示。 */
    @Column(name = "source", length = 24)
    private String source;

    protected AssetEntity() {
    }

    public AssetEntity(String name, String url, String kind, String ownerUsername) {
        this.name = name;
        this.url = url;
        this.kind = kind;
        this.ownerUsername = ownerUsername;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source == null ? "UPLOAD" : source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPurpose() {
        return purpose == null ? "PRODUCT" : purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }
}