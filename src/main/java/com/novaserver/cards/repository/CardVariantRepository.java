package com.novaserver.cards.repository;

import com.novaserver.cards.entity.CardVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 카드 변형 저장소. */
public interface CardVariantRepository extends JpaRepository<CardVariant, Long> {

    List<CardVariant> findAllByCard_IdOrderByMinPermanentLevelAsc(Long cardId);

    boolean existsByCard_IdAndMinPermanentLevel(Long cardId, int minPermanentLevel);

    boolean existsByCard_IdAndMinPermanentLevelAndIdNot(
            Long cardId, int minPermanentLevel, Long id);
}
