package com.novaserver.version.config;

import com.novaserver.version.entity.DataTable;
import com.novaserver.version.entity.DataVersion;
import com.novaserver.version.repository.DataVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 서버 시작 시 DB에 없는 버전 행을 버전 1로 만든다. */
@Component
@RequiredArgsConstructor
public class DataVersionInitializer implements ApplicationRunner {
    private final DataVersionRepository dataVersionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfAbsent(DataVersion.REVISION_KEY);
        for (DataTable table : DataTable.values()) {
            createIfAbsent(table.getKey());
        }
    }

    private void createIfAbsent(String key) {
        if (!dataVersionRepository.existsById(key)) {
            dataVersionRepository.save(new DataVersion(key));
        }
    }
}
