package com.novaserver.cards.repository;

import com.novaserver.cards.entity.RequiredCardCount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 선행 카드 조건 저장소. */
public interface RequiredCardCountRepository extends JpaRepository<RequiredCardCount, Long> {

    List<RequiredCardCount> findAllByCardId(Long cardId);

    boolean existsByCardIdAndRequiredCardId(Long cardId, Long requiredCardId);
}
