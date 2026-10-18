package com.novaserver.stage.repository;

import com.novaserver.stage.entity.RewardRulesConfig;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** 보상 기준 스냅샷 저장소. */
public interface RewardRulesConfigRepository extends JpaRepository<RewardRulesConfig, Long> {
    /** 계산 중 기준 교체만 막고 다른 전투의 조회는 허용한다. */
    @Override
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<RewardRulesConfig> findById(Long id);
}
