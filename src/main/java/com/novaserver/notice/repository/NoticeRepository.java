package com.novaserver.notice.repository;

import com.novaserver.notice.entity.Notice;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 공지 저장소. */
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Query("delete from Notice n where n.createdAt < :cutoff")
    void deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
