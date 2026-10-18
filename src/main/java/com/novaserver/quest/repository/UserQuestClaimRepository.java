package com.novaserver.quest.repository;

import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.entity.UserQuestClaim;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** 자연 멱등 키와 과거 실제 지급 결과 저장소. */
public interface UserQuestClaimRepository extends JpaRepository<UserQuestClaim, Long> {
    Optional<UserQuestClaim> findByUserIdAndPeriodAndPeriodDayAndDefinitionId(
            Long userId, QuestPeriod period, LocalDate day, String definitionId);

    List<UserQuestClaim> findByUserIdAndPeriodAndPeriodDay(
            Long userId, QuestPeriod period, LocalDate day);

    boolean existsByDefinitionId(String definitionId);

    @Modifying
    @Query("delete from UserQuestClaim c where c.user.id = :userId")
    void deleteByUserId(Long userId);
}
