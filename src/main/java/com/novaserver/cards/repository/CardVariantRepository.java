package com.novaserver.cards.repository;

import com.novaserver.cards.entity.CardVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 카드 변형 저장소. */
public interface CardVariantRepository extends JpaRepository<CardVariant, Long> {

    List<CardVariant> findAllByCardIdOrderByMinPermanentLevelAsc(Long cardId);

    boolean existsByCardIdAndMinPermanentLevel(Long cardId, int minPermanentLevel);

    boolean existsByCardIdAndMinPermanentLevelAndIdNot(Long cardId, int minPermanentLevel, Long id);
}
