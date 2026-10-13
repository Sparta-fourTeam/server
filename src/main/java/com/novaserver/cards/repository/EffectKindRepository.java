package com.novaserver.cards.repository;

import com.novaserver.cards.entity.EffectKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 효과 종류 저장소. */
public interface EffectKindRepository extends JpaRepository<EffectKind, Long> {

    List<EffectKind> findAllByOrderByIdAsc();

    Optional<EffectKind> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
