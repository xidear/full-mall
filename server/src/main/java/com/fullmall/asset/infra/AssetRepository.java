package com.fullmall.asset.infra;

import com.fullmall.asset.domain.AssetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {

    Page<AssetEntity> findByOwnerUsernameAndDeletedAtIsNull(String ownerUsername, Pageable pageable);
}