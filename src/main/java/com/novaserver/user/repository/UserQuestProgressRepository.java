package com.novaserver.user.repository;

import com.novaserver.user.entity.UserQuestProgress;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저별 퀘스트 진행도 저장소. */
public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, Long> {
    /** 퀘스트를 함께 가져온다. */
    @EntityGraph(attributePaths = {"quest"})
    List<UserQuestProgress> findByUserId(Long userId);

    /** 진행도 갱신 시 동시 요청으로 중복 반영되지 않도록 락을 잡고 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"quest"})
    @Query(
            "select p from UserQuestProgress p "
                    + "where p.user.id = :userId and p.quest.questId = :questId")
    Optional<UserQuestProgress> findByUserIdAndQuestIdForUpdate(
            @Param("userId") Long userId, @Param("questId") String questId);

    /** 같은 종류의 퀘스트 여러 건을 한 번에 락 걸고 조회한다. increaseProgress에서 퀘스트별 개별 조회를 피하기 위해 쓴다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"quest"})
    @Query(
            "select p from UserQuestProgress p "
                    + "where p.user.id = :userId and p.quest.questId in :questIds")
    List<UserQuestProgress> findByUserIdAndQuestIdInForUpdate(
            @Param("userId") Long userId, @Param("questIds") List<String> questIds);

    boolean existsByQuestQuestId(String questId);

    @Modifying
    @Query("delete from UserQuestProgress p where p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
