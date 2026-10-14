package com.novaserver.skill.repository;

import com.novaserver.skill.entity.SkillElement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스킬·몬스터 속성 저장소. */
public interface SkillElementRepository extends JpaRepository<SkillElement, Long> {

    List<SkillElement> findAllByOrderByIdAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
