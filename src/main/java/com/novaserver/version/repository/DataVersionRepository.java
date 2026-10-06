package com.novaserver.version.repository;

import com.novaserver.version.entity.DataVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 데이터 버전 조회, 증가 */
public interface DataVersionRepository extends JpaRepository<DataVersion, String> {
    @Modifying(clearAutomatically = true)
    @Query(
            "UPDATE DataVersion v SET v.version = v.version + 1"
                    + " WHERE v.tableName IN (:tableName, :revisionKey)")
    int increaseVersion(
            @Param("tableName") String tableName, @Param("revisionKey") String revisionKey);
}
