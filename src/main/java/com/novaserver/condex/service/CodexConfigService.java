package com.novaserver.condex.service;

import com.novaserver.condex.dto.CodexGradeRewardRequest;
import com.novaserver.condex.dto.CodexGradeRewardResponse;
import com.novaserver.condex.dto.CodexMonsterRequest;
import com.novaserver.condex.dto.CodexMonsterResponse;
import com.novaserver.condex.entity.Codex;
import com.novaserver.condex.entity.CodexGradeReward;
import com.novaserver.condex.entity.MonsterGrade;
import com.novaserver.condex.repository.CodexGradeRewardRepository;
import com.novaserver.condex.repository.CodexRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.stage.service.RewardRulesService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 명시적 서버 데이터만 저장한다. 클라이언트 MockData·테스트 fixture를 운영 데이터로 주입하지 않는다. */
@Service
@RequiredArgsConstructor
public class CodexConfigService {
    private final CodexRepository monsters;
    private final CodexGradeRewardRepository grades;
    private final RewardDefinitionService definitions;
    private final RewardRulesService references;

    /** 현재 서버 도감 카탈로그를 ID 순으로 읽는다. */
    @Transactional(readOnly = true)
    public List<CodexMonsterResponse> monsters() {
        return monsters.findAll().stream()
                .sorted(Comparator.comparing(Codex::getId))
                .map(CodexMonsterResponse::from)
                .toList();
    }

    /** 설정된 등급과 공통 보상의 현재 골드를 함께 읽는다. */
    @Transactional(readOnly = true)
    public List<CodexGradeRewardResponse> rewards() {
        return grades.findAll().stream()
                .sorted(Comparator.comparing(CodexGradeReward::getGrade))
                .map(CodexGradeRewardResponse::from)
                .toList();
    }

    /** 몬스터 ID는 int 양수 범위다. 새 행은 버전 생략, 교체는 정확한 현재 버전이 필요하다. */
    @Transactional
    public CodexMonsterResponse saveMonster(Long id, CodexMonsterRequest request) {
        if (id == null || id <= 0 || id > Integer.MAX_VALUE || request == null) {
            throw ErrorCode.CODEX_CONFIG_INVALID.exception();
        }
        var grade = grade(request.grade());
        references.lockItemReferences();
        var existing = monsters.findById(id);
        checkVersion(request.expectedVersion(), existing.map(Codex::getVersion).orElse(null));
        Codex row;
        if (existing.isPresent()) {
            row = existing.get();
            row.changeGrade(grade);
        } else {
            row = new Codex(id, grade);
        }
        return CodexMonsterResponse.from(monsters.saveAndFlush(row));
    }

    /** 기존 양수 골드 전용 공통 보상을 등급에 연결한다. 별도 밸런스 기본값은 없다. */
    @Transactional
    public CodexGradeRewardResponse saveReward(String name, CodexGradeRewardRequest request) {
        var grade = grade(name);
        if (request == null
                || request.rewardDefinitionId() == null
                || request.rewardDefinitionId() <= 0) {
            throw ErrorCode.CODEX_CONFIG_INVALID.exception();
        }
        references.lockItemReferences();
        var existing = grades.findById(grade);
        checkVersion(
                request.expectedVersion(), existing.map(CodexGradeReward::getVersion).orElse(null));
        var reward = definitions.findDefinition(request.rewardDefinitionId());
        if (reward.getGold() <= 0 || !reward.getEntries().isEmpty()) {
            throw ErrorCode.CODEX_CONFIG_INVALID.exception();
        }
        CodexGradeReward row;
        if (existing.isPresent()) {
            row = existing.get();
            row.changeReward(reward);
        } else {
            row = new CodexGradeReward(grade, reward);
        }
        return CodexGradeRewardResponse.from(grades.saveAndFlush(row));
    }

    /** 공급을 중단해도 과거 지급 영수증은 남는다. 공통 정의 삭제와 같은 참조 잠금을 쓴다. */
    @Transactional
    public void deleteReward(String name) {
        var grade = grade(name);
        references.lockItemReferences();
        grades.deleteById(grade);
        grades.flush();
    }

    private MonsterGrade grade(String name) {
        return MonsterGrade.fromName(name).orElseThrow(ErrorCode.CODEX_CONFIG_INVALID::exception);
    }

    private void checkVersion(Integer expected, Integer actual) {
        if (actual == null ? expected != null : !actual.equals(expected)) {
            throw ErrorCode.CODEX_CONFIG_CONFLICT.exception();
        }
        if (actual != null && actual == Integer.MAX_VALUE) {
            throw ErrorCode.CODEX_CONFIG_INVALID.exception();
        }
    }
}
