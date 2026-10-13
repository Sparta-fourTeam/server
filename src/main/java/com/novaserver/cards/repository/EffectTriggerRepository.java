package com.novaserver.cards.repository;

import com.novaserver.cards.entity.EffectTrigger;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 효과 발동 시점 저장소. */
public interface EffectTriggerRepository extends JpaRepository<EffectTrigger, Long> {

    List<EffectTrigger> findAllByOrderByIdAsc();

    Optional<EffectTrigger> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
