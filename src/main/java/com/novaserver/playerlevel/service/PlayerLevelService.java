package com.novaserver.playerlevel.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.playerlevel.dto.PlayerLevelConfigRequest;
import com.novaserver.playerlevel.dto.PlayerLevelConfigResponse;
import com.novaserver.playerlevel.entity.PlayerLevelConfig;
import com.novaserver.playerlevel.repository.PlayerLevelConfigRepository;
import com.novaserver.version.entity.DataTable;
import com.novaserver.version.repository.DataVersionRepository;
import com.novaserver.version.service.DataVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 플레이어 경험치 기준 조회·관리. 초기 값이 없으면 구매용 기본값을 추측하지 않는다. */
@Service
@RequiredArgsConstructor
public class PlayerLevelService {
    private final PlayerLevelConfigRepository repository;
    private final DataVersionRepository versions;
    private final DataVersionService versionService;

    /** DB 기준과 해당 버전을 같은 읽기 트랜잭션에서 조회한다. 미설정이면 503을 반환한다. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PlayerLevelConfigResponse getConfig() {
        PlayerLevelConfig config =
                repository
                        .findById(PlayerLevelConfig.CONFIG_ID)
                        .orElseThrow(ErrorCode.PLAYER_LEVELS_NOT_CONFIGURED::exception);
        int version =
                versions.findById(DataTable.PLAYER_LEVELS.getKey())
                        .orElseThrow(
                                () -> ErrorCode.DATA_VERSION_ROW_MISSING.exception("PlayerLevels"))
                        .getVersion();
        return new PlayerLevelConfigResponse(
                config.getBaseExp(), config.getStepExp(), config.getMaxLevel(), version);
    }

    /** 기준을 등록·교체하고 기존 버전 API와 같은 방식으로 테이블/전체 버전을 함께 증가시킨다. */
    @Transactional
    public PlayerLevelConfigResponse saveConfig(PlayerLevelConfigRequest request) {
        validate(request);
        // 초기 등록을 포함해 동시 관리 요청을 같은 버전 행으로 직렬화한다.
        versions.findByTableNameForUpdate(DataTable.PLAYER_LEVELS.getKey())
                .orElseThrow(() -> ErrorCode.DATA_VERSION_ROW_MISSING.exception("PlayerLevels"));
        PlayerLevelConfig config =
                repository
                        .findById(PlayerLevelConfig.CONFIG_ID)
                        .orElseGet(
                                () ->
                                        new PlayerLevelConfig(
                                                request.baseExp(),
                                                request.stepExp(),
                                                request.maxLevel()));
        config.update(request.baseExp(), request.stepExp(), request.maxLevel());
        repository.saveAndFlush(config);
        versionService.increase(DataTable.PLAYER_LEVELS.getKey());
        return getConfig();
    }

    private void validate(PlayerLevelConfigRequest request) {
        if (request.baseExp() == null
                || request.stepExp() == null
                || request.maxLevel() == null
                || request.baseExp() <= 0
                || request.stepExp() < 0
                || request.maxLevel() < 2
                || request.maxLevel() > PlayerLevelConfig.MAX_SUPPORTED_LEVEL) {
            throw ErrorCode.PLAYER_LEVELS_DATA_INVALID.exception();
        }
        // 누적 경험치는 UserData.exp와 클라이언트 모두 int로 저장한다.
        long count = request.maxLevel() - 1L;
        long requiredExp = count * request.baseExp() + count * (count - 1) / 2 * request.stepExp();
        if (requiredExp > Integer.MAX_VALUE) {
            throw ErrorCode.PLAYER_LEVELS_DATA_INVALID.exception();
        }
    }
}
