package com.novaserver.battle.progress;

import org.springframework.data.jpa.repository.JpaRepository;

/** 보고 ID별 최초 접수 저장소. 소유 계정은 진행 원장으로 확인한다. */
public interface BattleProgressReceiptRepository
        extends JpaRepository<BattleProgressReceipt, String> {}
