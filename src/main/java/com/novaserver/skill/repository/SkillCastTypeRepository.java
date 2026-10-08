package com.novaserver.skill.repository;

import com.novaserver.skill.entity.SkillCastType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스킬 시전 방식 저장소. */
public interface SkillCastTypeRepository extends JpaRepository<SkillCastType, Long> {

    List<SkillCastType> findAllByOrderByIdAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
