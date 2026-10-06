package com.novaserver.version.dto;

import java.util.Map;
import lombok.Getter;

/**
 * 데이터 버전 응답.
 *
 * <p>revision 전체 리비전 tables 테이블 키별 버전
 */
@Getter
public class DataVersionResponse {
    private final int revision;
    private final Map<String, Integer> tables;

    public DataVersionResponse(int revision, Map<String, Integer> tables) {
        this.revision = revision;
        this.tables = tables;
    }
}
