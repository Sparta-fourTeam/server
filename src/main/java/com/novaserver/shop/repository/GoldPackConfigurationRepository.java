package com.novaserver.shop.repository;

import com.novaserver.shop.entity.GoldPackConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

/** 골드팩 카탈로그 단일 스냅샷 저장소. */
public interface GoldPackConfigurationRepository
        extends JpaRepository<GoldPackConfiguration, Long> {}
