package com.novaserver.condex.repository;

import com.novaserver.condex.entity.Codex;
import org.springframework.data.jpa.repository.JpaRepository;

/** 서버 몬스터 식별자 저장소. 몬스터 상세·등급 기준은 #50에서 제공한다. */
public interface CodexRepository extends JpaRepository<Codex, Long> {}
