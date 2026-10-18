package com.novaserver.quest.repository;

import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.entity.UserQuestPeriod;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** 계정·기간별 검증 행동 집계 저장소. */
public interface UserQuestPeriodRepository extends JpaRepository<UserQuestPeriod, Long> {
    Optional<UserQuestPeriod> findByUserIdAndPeriodAndPeriodDay(
            Long userId, QuestPeriod period, LocalDate day);

    @Modifying
    @Query("delete from UserQuestPeriod p where p.user.id = :userId")
    void deleteByUserId(Long userId);
}
