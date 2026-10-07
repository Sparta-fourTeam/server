package com.novaserver.version.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 게임 데이터 테이블별 버전. 전체 리비전은 REVISION_KEY 행으로 관리한다. */
@Entity
@Table(name = "data_version")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataVersion {
    public static final String REVISION_KEY = "_revision";

    @Id
    @Column(length = 50)
    private String tableName;

    @Column(nullable = false)
    private int version;

    /**
     * 버전 1로 시작하는 행을 만든다.
     *
     * @param tableName 테이블 키 또는 REVISION_KEY
     */
    public DataVersion(String tableName) {
        this.tableName = tableName;
        this.version = 1;
    }
}
