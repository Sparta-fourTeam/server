package com.novaserver.skill.repository;

import com.novaserver.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스킬 저장소. */
public interface SkillRepository extends JpaRepository<Skill, Long> {

    boolean existsByCastTypeId(Long castTypeId);

    boolean existsByProjectilePathId(Long projectilePathId);
}
