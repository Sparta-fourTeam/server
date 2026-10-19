package com.novaserver.user.repository;

import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.user.entity.UserQuestPoint;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저별 주기 누적 퀘스트 포인트 저장소. */
public interface UserQuestPointRepository extends JpaRepository<UserQuestPoint, Long> {
    List<UserQuestPoint> findByUserId(Long userId);

    /** 포인트 적립·보상 수령 시 동시 요청으로 중복 반영되지 않도록 락을 잡고 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserQuestPoint p where p.user.id = :userId and p.period = :period")
    Optional<UserQuestPoint> findByUserIdAndPeriodForUpdate(
            @Param("userId") Long userId, @Param("period") QuestPeriod period);

    @Modifying
    @Query("delete from UserQuestPoint p where p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
