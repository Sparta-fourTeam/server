package com.novaserver.cards.repository;

import com.novaserver.cards.entity.SkillForm;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스킬 형태 저장소. */
public interface SkillFormRepository extends JpaRepository<SkillForm, Long> {

    List<SkillForm> findAllByOrderByIdAsc();

    Optional<SkillForm> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
