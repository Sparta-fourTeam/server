package com.novaserver.cards.repository;

import com.novaserver.cards.entity.EffectTrigger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 효과 발동 시점 저장소. */
public interface EffectTriggerRepository extends JpaRepository<EffectTrigger, Long> {

    List<EffectTrigger> findAllByOrderByIdAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
