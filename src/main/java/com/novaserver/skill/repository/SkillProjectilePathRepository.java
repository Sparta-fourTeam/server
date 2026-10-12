package com.novaserver.skill.repository;

import com.novaserver.skill.entity.SkillProjectilePath;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스킬 투사체 경로 저장소. */
public interface SkillProjectilePathRepository extends JpaRepository<SkillProjectilePath, Long> {

    List<SkillProjectilePath> findAllByOrderByIdAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
