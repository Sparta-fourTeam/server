package com.novaserver.global.config;

import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 서버 시작 시 스테이지 비어 있으면 테스트용으로 1건씩 만든다. */
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class CatalogDataSeeder implements ApplicationRunner {
    private final StageRepository stageRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (stageRepository.count() == 0) {
            stageRepository.save(Stage.create());
        }
    }
}
