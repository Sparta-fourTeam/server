package com.novaserver.mail.repository;

import com.novaserver.mail.entity.Mail;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 우편 저장소. */
public interface MailRepository extends JpaRepository<Mail, Long> {
    @EntityGraph(attributePaths = {"items", "items.item"})
    List<Mail> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 수령 처리용으로 우편 행에 쓰기 잠금을 걸고 가져온다. 잠금 없이 isClaimed()를 확인하고 보상을 지급하면, 같은 우편에 대한 동시 수령 요청 두 개가 서로
     * 커밋 전 상태를 읽어 둘 다 통과해 보상이 중복 지급될 수 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.item"})
    @Query("select m from Mail m where m.id = :id and m.user.id = :userId")
    Optional<Mail> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 정리 대상 우편을 첨부 아이템과 함께 가져온다. 삭제는 {@link #deleteAll(Iterable)}로 엔티티 단위로 해야 {@code
     * cascade}·{@code orphanRemoval}이 적용되어 첨부 아이템도 같이 지워진다 — 벌크 {@code delete} 쿼리는 자식 행을 그대로 남긴다.
     */
    @EntityGraph(attributePaths = {"items"})
    List<Mail> findAllByCreatedAtBefore(Instant cutoff);
}
