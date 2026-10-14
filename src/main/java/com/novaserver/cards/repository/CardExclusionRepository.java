package com.novaserver.cards.repository;

import com.novaserver.cards.entity.CardExclusion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 카드 배제 조건 저장소. */
public interface CardExclusionRepository extends JpaRepository<CardExclusion, Long> {

    List<CardExclusion> findAllByCardId(Long cardId);

    boolean existsByCardIdAndExcludingCardId(Long cardId, Long excludingCardId);
}
