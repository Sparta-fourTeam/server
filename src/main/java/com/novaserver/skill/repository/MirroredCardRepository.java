package com.novaserver.skill.repository;

import com.novaserver.skill.entity.MirroredCard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 카드 효과 적용 조건 저장소. */
public interface MirroredCardRepository extends JpaRepository<MirroredCard, Long> {

    List<MirroredCard> findAllBySkill_Id(Long skillId);

    boolean existsBySkill_IdAndCard_Id(Long skillId, Long cardId);
}
