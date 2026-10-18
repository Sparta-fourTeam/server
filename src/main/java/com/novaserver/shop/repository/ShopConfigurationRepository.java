package com.novaserver.shop.repository;

import com.novaserver.shop.entity.ShopConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

/** 일일 상점 서버 정책 저장소. */
public interface ShopConfigurationRepository extends JpaRepository<ShopConfiguration, Long> {}
