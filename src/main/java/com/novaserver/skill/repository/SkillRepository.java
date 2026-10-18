package com.novaserver.skill.repository;

import com.novaserver.skill.entity.Skill;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 스킬 저장소. */
public interface SkillRepository extends JpaRepository<Skill, Long> {
    /** 시전 방식·투사체 경로를 함께 가져온다. 응답에서 두 이름을 쓰므로 지연 로딩 추가 쿼리를 막는다. */
    @EntityGraph(attributePaths = {"castType", "projectilePath", "element"})
    List<Skill> findAllBy();

    List<Skill> findByProgressionId(String progressionId);

    /** 강화별 가장 이른 스킬 해금 레벨만 조회한다. */
    @Query(
            "select s.progressionId as progressionId, min(s.unlockLevel) as unlockLevel "
                    + "from Skill s where s.progressionId is not null group by s.progressionId")
    List<UpgradeUnlock> findUpgradeUnlockLevels();

    /** 스킬 강화의 구매 해금 레벨 투영. */
    interface UpgradeUnlock {
        String getProgressionId();

        int getUnlockLevel();
    }

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByCastTypeId(Long castTypeId);

    boolean existsByProjectilePathId(Long projectilePathId);

    boolean existsByElementId(Long elementId);
}
