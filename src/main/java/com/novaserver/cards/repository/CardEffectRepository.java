package com.novaserver.cards.repository;

import com.novaserver.cards.entity.CardEffect;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 카드 효과 저장소. */
public interface CardEffectRepository extends JpaRepository<CardEffect, Long> {

    List<CardEffect> findAllByCardIdOrderBySortOrderAsc(Long cardId);

    List<CardEffect> findAllByVariantIdOrderBySortOrderAsc(Long variantId);
}
