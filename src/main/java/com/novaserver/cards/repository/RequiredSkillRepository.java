package com.novaserver.cards.repository;

import com.novaserver.cards.entity.RequiredSkill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 필요 스킬 조건 저장소. */
public interface RequiredSkillRepository extends JpaRepository<RequiredSkill, Long> {

    List<RequiredSkill> findAllByCardId(Long cardId);

    boolean existsByCardIdAndSkillId(Long cardId, Long skillId);
}
