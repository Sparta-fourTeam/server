package com.novaserver.cards.repository;

import com.novaserver.cards.entity.UpgradeCard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 강화 카드 저장소. */
public interface UpgradeCardRepository extends JpaRepository<UpgradeCard, Long> {

    List<UpgradeCard> findAllBySkillIdIn(List<Long> skillIds);
}
