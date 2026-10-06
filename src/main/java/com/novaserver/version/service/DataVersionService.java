package com.novaserver.version.service;

import com.novaserver.version.dto.DataVersionResponse;
import com.novaserver.version.entity.DataTable;
import com.novaserver.version.entity.DataVersion;
import com.novaserver.version.repository.DataVersionRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 게임 데이터 버전을 조회 및 버전 업데이트 */
@Service
@RequiredArgsConstructor
public class DataVersionService {
    private final DataVersionRepository dataVersionRepository;

    /** 전체 리비전과 테이블별 버전 조회 */
    @Transactional(readOnly = true)
    public DataVersionResponse getVersions() {
        Map<String, Integer> saved =
                dataVersionRepository.findAll().stream()
                        .collect(
                                Collectors.toMap(
                                        DataVersion::getTableName, DataVersion::getVersion));

        Map<String, Integer> tables = new LinkedHashMap<>();
        for (DataTable table : DataTable.values()) {
            tables.put(table.getKey(), saved.getOrDefault(table.getKey(), 1));
        }

        int revision = saved.getOrDefault(DataVersion.REVISION_KEY, 1);

        return new DataVersionResponse(revision, tables);
    }

    /** 테이블 버전과 전체 리비전을 1 올림 */
    @Transactional
    public void increase(DataTable table) {
        int updated =
                dataVersionRepository.increaseVersion(table.getKey(), DataVersion.REVISION_KEY);

        // 누군가 해당 테이블의 값을 없앴을 때
        if (updated != 2) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "데이터 버전 행이 없습니다: " + table.getKey());
        }
    }
}
