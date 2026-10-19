package com.novaserver.version.repository;

import com.novaserver.version.entity.DataVersion;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 데이터 버전 조회, 증가. */
public interface DataVersionRepository extends JpaRepository<DataVersion, String> {
    /** 게시자가 잡는 전체 버전 행의 쓰기 락과 충돌하는 공유 읽기 락이다. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select v from DataVersion v where v.tableName = :tableName")
    Optional<DataVersion> findByTableNameForRead(@Param("tableName") String tableName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from DataVersion v where v.tableName = :tableName")
    Optional<DataVersion> findByTableNameForUpdate(@Param("tableName") String tableName);

    @Modifying(clearAutomatically = true)
    @Query(
            "UPDATE DataVersion v SET v.version = v.version + 1"
                    + " WHERE v.tableName IN (:tableName, :revisionKey)")
    int increaseVersion(
            @Param("tableName") String tableName, @Param("revisionKey") String revisionKey);
}
