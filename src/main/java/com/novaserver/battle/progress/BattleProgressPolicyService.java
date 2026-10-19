package com.novaserver.battle.progress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.stage.entity.Stage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 서버 스폰 정의에서 보수적인 가능 범위만 검사한다. 처치·실제 플레이를 증명하지 않는다. */
@Service
@RequiredArgsConstructor
public class BattleProgressPolicyService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final StageProgressPolicyRepository policies;
    private final BattlePolicySource source;
    private final EntityManager entities;

    /** 내부 정책 저장. 임의 스폰 구성을 받는 외부 API는 제공하지 않는다. */
    @Transactional
    public StageProgressPolicyEntity save(Integer stageId, StageProgressPolicy policy) {
        if (entities.find(Stage.class, stageId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw ErrorCode.STAGE_NOT_FOUND.exception(stageId);
        }
        validate(policy);
        var previous = policies.findById(stageId).orElse(null);
        if ((previous == null ? 0 : previous.getVersion()) != policy.expectedVersion()) {
            throw ErrorCode.BATTLE_PROGRESS_CONFLICT.exception();
        }
        String definition = encode(policy);
        if (previous == null) {
            previous = new StageProgressPolicyEntity(stageId, definition, source.revision());
        } else {
            previous.replace(definition, source.revision());
        }
        return policies.saveAndFlush(previous);
    }

    /** 실제 같은 원본으로 정책을 생성·개정한다. 활성 전투의 동결본은 수정하지 않는다. */
    @Transactional
    public StageProgressPolicyEntity require(Integer stageId) {
        if (entities.find(Stage.class, stageId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw ErrorCode.STAGE_NOT_FOUND.exception(stageId);
        }
        var previous = policies.findById(stageId).orElse(null);
        if (previous != null && source.revision().equals(previous.getSourceRevision())) {
            return previous;
        }
        var derived = source.policy(stageId);
        validate(derived);
        return save(
                stageId,
                new StageProgressPolicy(
                        derived.maxTimeScale(),
                        derived.minSpawnInterval(),
                        derived.waves(),
                        derived.descendants(),
                        previous == null ? 0 : previous.getVersion()));
    }

    /** 클라이언트가 실제로 로드한 원문과 전투 최초 고정을 비교한다. */
    public String sourceRevision() {
        return source.revision();
    }

    /** 원장을 열 때 동결한 스폰 데이터와 서버 경과 시간으로 불가능한 보고만 거절한다. */
    public void check(BattleProgress progress, ProgressReport report, Instant now) {
        StageProgressPolicy policy = decode(progress.getPolicy());
        if (report.completedWaves() > report.reachedWave()
                || report.reachedWave() > policy.waves().size()
                || report.reachedWave() > report.completedWaves() + 1
                || report.killedMonsters() < progress.getKilledMonsters()
                || report.completedWaves() < progress.getCompletedWaves()
                || report.reachedWave() < progress.getReachedWave()) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
        var allowed = new HashSet<Long>();
        long minimum = 0;
        long multiplier = 1;
        var memo = new HashMap<Long, Long>();
        for (int i = 0; i < report.reachedWave(); i++) {
            for (var spawn : policy.waves().get(i).entrySet()) {
                collect(spawn.getKey(), policy, allowed);
                multiplier =
                        Math.max(multiplier, bodies(spawn.getKey(), policy, memo, new HashSet<>()));
                if (i < report.completedWaves()) {
                    minimum = Math.addExact(minimum, spawn.getValue());
                }
            }
        }
        if (!allowed.containsAll(report.encounteredMonsterIds())
                || (report.killedMonsters() > 0 && report.encounteredMonsterIds().isEmpty())
                || report.killedMonsters() < minimum
                || now.isBefore(progress.getStartedAt())) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
        BigDecimal elapsed =
                BigDecimal.valueOf(Duration.between(progress.getStartedAt(), now).toMillis(), 3);
        BigDecimal roots =
                elapsed.multiply(policy.maxTimeScale())
                        .divideToIntegralValue(policy.minSpawnInterval());
        BigDecimal maximum = roots.multiply(BigDecimal.valueOf(multiplier));
        if (BigDecimal.valueOf(report.killedMonsters()).compareTo(maximum) > 0
                || (!report.encounteredMonsterIds().isEmpty() && roots.signum() == 0)) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
    }

    private void validate(StageProgressPolicy policy) {
        if (policy == null
                || policy.expectedVersion() < 0
                || policy.maxTimeScale() == null
                || policy.maxTimeScale().compareTo(source.manifest().maxTimeScale()) != 0
                || policy.minSpawnInterval() == null
                || policy.minSpawnInterval().signum() <= 0
                || policy.waves() == null
                || policy.waves().isEmpty()
                || policy.waves().size() > 20
                || policy.descendants() == null
                || policy.descendants().isEmpty()) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
        for (var monster : policy.descendants().entrySet()) {
            if (monster.getKey() == null
                    || monster.getKey() <= 0
                    || monster.getValue() == null
                    || !source.containsMonster(monster.getKey())) {
                throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
            }
            var seen = new HashSet<Long>();
            for (var child : monster.getValue()) {
                if (child == null
                        || child.monsterId() == null
                        || child.maximum() <= 0
                        || !seen.add(child.monsterId())
                        || !policy.descendants().containsKey(child.monsterId())) {
                    throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
                }
            }
        }
        for (var wave : policy.waves()) {
            if (wave == null
                    || wave.isEmpty()
                    || wave.entrySet().stream()
                            .anyMatch(
                                    s ->
                                            s.getKey() == null
                                                    || !policy.descendants().containsKey(s.getKey())
                                                    || s.getValue() == null
                                                    || s.getValue() <= 0)) {
                throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
            }
        }
        try {
            var memo = new HashMap<Long, Long>();
            for (var id : policy.descendants().keySet()) {
                bodies(id, policy, memo, new HashSet<>());
            }
        } catch (ArithmeticException e) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
    }

    private long bodies(
            Long id, StageProgressPolicy policy, Map<Long, Long> memo, Set<Long> visiting) {
        if (memo.containsKey(id)) {
            return memo.get(id);
        }
        if (!visiting.add(id)) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
        long total = 1;
        for (var child : policy.descendants().get(id)) {
            total =
                    Math.addExact(
                            total,
                            Math.multiplyExact(
                                    child.maximum(),
                                    bodies(child.monsterId(), policy, memo, visiting)));
        }
        visiting.remove(id);
        memo.put(id, total);
        return total;
    }

    private void collect(Long id, StageProgressPolicy policy, Set<Long> allowed) {
        if (!allowed.add(id)) {
            return;
        }
        for (var child : policy.descendants().get(id)) {
            collect(child.monsterId(), policy, allowed);
        }
    }

    static String encode(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
    }

    static StageProgressPolicy decode(String value) {
        try {
            return JSON.readValue(value, StageProgressPolicy.class);
        } catch (JsonProcessingException e) {
            throw ErrorCode.BATTLE_PROGRESS_UNAVAILABLE.exception();
        }
    }
}
