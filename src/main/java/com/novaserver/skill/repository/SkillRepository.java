package com.novaserver.skill.repository;

import com.novaserver.skill.entity.Skill;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스킬 저장소. */
public interface SkillRepository extends JpaRepository<Skill, Long> {
    /** 시전 방식·투사체 경로를 함께 가져온다. 응답에서 두 이름을 쓰므로 지연 로딩 추가 쿼리를 막는다. */
    @EntityGraph(attributePaths = {"castType", "projectilePath"})
    List<Skill> findAllBy();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByCastTypeId(Long castTypeId);

    boolean existsByProjectilePathId(Long projectilePathId);
}
