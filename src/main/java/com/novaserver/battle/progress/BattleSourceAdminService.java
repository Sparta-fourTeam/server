package com.novaserver.battle.progress;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.version.entity.DataTable;
import com.novaserver.version.entity.DataVersion;
import com.novaserver.version.repository.DataVersionRepository;
import com.novaserver.version.service.DataVersionService;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 원본을 검증한 뒤 데이터와 기존 테이블 버전을 같은 트랜잭션으로 게시한다. */
@Service
@RequiredArgsConstructor
public class BattleSourceAdminService {
    private final BattleSourceRepository sources;
    private final DataVersionRepository versionRows;
    private final DataVersionService versions;

    /** 최초 게시에는 빈 expectedRevision을, 수정에는 현재 내용 주소를 요구한다. */
    @Transactional
    public BattlePolicySource.Manifest publish(Publication request) {
        final BattlePolicySource parsed;
        try {
            parsed =
                    new BattlePolicySource(
                            request.stagesJson(), request.monstersJson(), request.combatJson());
        } catch (IOException | IllegalArgumentException invalid) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
        versionRows
                .findByTableNameForUpdate(DataVersion.REVISION_KEY)
                .orElseThrow(() -> ErrorCode.DATA_VERSION_ROW_MISSING.exception("revision"));
        var previous = sources.findById(1).orElse(null);
        if (!Objects.equals(
                previous == null ? null : previous.getSourceRevision(),
                request.expectedRevision())) {
            throw ErrorCode.BATTLE_PROGRESS_CONFLICT.exception();
        }
        if (previous == null || !previous.getStagesJson().equals(request.stagesJson())) {
            versions.increase(DataTable.STAGES.getKey());
        }
        if (previous == null || !previous.getMonstersJson().equals(request.monstersJson())) {
            versions.increase(DataTable.MONSTERS.getKey());
        }
        if (previous == null || !previous.getCombatJson().equals(request.combatJson())) {
            versions.increase(DataTable.COMBAT.getKey());
        }
        sources.saveAndFlush(new BattleSourceEntity(parsed));
        return parsed.manifest();
    }

    /** JSON은 HTTP 데이터로만 받는다. 파일 업로드·파일 생성은 하지 않는다. */
    public record Publication(
            @jakarta.validation.constraints.NotBlank String stagesJson,
            @jakarta.validation.constraints.NotBlank String monstersJson,
            @jakarta.validation.constraints.NotBlank String combatJson,
            String expectedRevision) {}
}
