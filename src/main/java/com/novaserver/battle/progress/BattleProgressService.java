package com.novaserver.battle.progress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.quest.service.UserQuestService;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.service.EnergyPurchasePolicy;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 누적 진행만 중간 반영한다. 동일 보고 재전송과 역전은 보상·진행을 중복 지급하지 않는다. */
@Service
@RequiredArgsConstructor
public class BattleProgressService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final UserRepository users;
    private final BattleRepository sessions;
    private final UserStageProgressRepository stages;
    private final BattleProgressRepository progress;
    private final BattleProgressReceiptRepository receipts;
    private final BattleProgressPolicyService policies;
    private final UserQuestService quests;
    private final EnergyPurchasePolicy days;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    /** 새 차감 없이 기존 활성 전투에 스테이지·정책을 한 번만 고정한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public State open(Long userId, String battleId, Integer stageId, String sourceRevision) {
        final var user =
                users.findByIdForUpdate(userId).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
        var session = sessions.findById(userId).orElseThrow(ErrorCode.BATTLE_NOT_FOUND::exception);
        if (!session.getBattleId().equals(battleId)) {
            throw ErrorCode.BATTLE_ID_MISMATCH.exception();
        }
        var prior = progress.findById(battleId).orElse(null);
        if (prior != null) {
            if (!prior.getUser().getId().equals(userId)
                    || !prior.getStageId().equals(stageId)
                    || prior.isClosed()
                    || !prior.getSourceRevision().equals(sourceRevision)) {
                throw ErrorCode.BATTLE_PROGRESS_CONFLICT.exception();
            }
            return state(prior);
        }
        if (!policies.sourceRevision().equals(sourceRevision)) {
            throw ErrorCode.BATTLE_PROGRESS_CONFLICT.exception();
        }
        var settled = stages.findByUserId(userId);
        var order = policies.stageOrder();
        int index = order.indexOf(stageId);
        boolean tried = settled.stream().anyMatch(p -> p.getStage().getStageId().equals(stageId));
        boolean first = index == 0;
        boolean next =
                index > 0
                        && settled.stream()
                                .anyMatch(
                                        p ->
                                                p.getStage()
                                                                .getStageId()
                                                                .equals(order.get(index - 1))
                                                        && p.getClearRating() > 0);
        if (!tried && !first && !next) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
        var definition = policies.require(stageId);
        if (!definition.getSourceRevision().equals(sourceRevision)) {
            throw ErrorCode.BATTLE_PROGRESS_CONFLICT.exception();
        }
        var row =
                progress.saveAndFlush(
                        new BattleProgress(
                                user, battleId, stageId, definition, session.getCreatedAt()));
        return state(row);
    }

    /**
     * 게시된 스테이지 순서에서 stageId 다음 스테이지를 구한다. 마지막이거나 순서에 없거나 서버 원본이 아직 준비되지 않았으면 비어 있다. 전투 결과 저장을 막지
     * 않도록 원본 미준비는 예외 대신 빈 값으로 돌려준다.
     */
    public java.util.Optional<Integer> nextStageId(Integer stageId) {
        java.util.List<Integer> order;
        try {
            order = policies.stageOrder();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return java.util.Optional.empty();
        }
        int index = order.indexOf(stageId);
        return index >= 0 && index + 1 < order.size()
                ? java.util.Optional.of(order.get(index + 1))
                : java.util.Optional.empty();
    }

    /** 전투 실행 상태를 기기간 복원하는 API가 아니라 이미 인정한 누적 기준 조회다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public State snapshot(Long userId, String battleId) {
        users.findByIdForUpdate(userId).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
        return state(owned(userId, battleId));
    }

    /** 최초 접수 시각의 기간에 차이만 집계한다. 같은 보고 ID는 포기·날짜 경계 뒤에도 원래 ACK다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ProgressAck report(Long userId, ProgressReport request) {
        users.findByIdForUpdate(userId).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
        if (request == null
                || request.encounteredMonsterIds() == null
                || request.killedMonsters() < 0
                || request.completedWaves() < 0
                || request.reachedWave() < 0
                || request.expectedRevision() < 0
                || request.sourceRevision() == null
                || !request.sourceRevision().matches("[0-9a-f]{64}")
                || request.reportId() == null
                || !request.reportId()
                        .matches(
                                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-"
                                        + "[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")
                || request.encounteredMonsterIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw ErrorCode.BATTLE_PROGRESS_INVALID.exception();
        }
        var row = owned(userId, request.battleId());
        var normalized =
                new ProgressReport(
                        request.battleId(),
                        request.reportId(),
                        request.expectedRevision(),
                        request.killedMonsters(),
                        request.completedWaves(),
                        request.reachedWave(),
                        new TreeSet<>(request.encounteredMonsterIds()),
                        request.sourceRevision());
        String payload = BattleProgressPolicyService.encode(normalized);
        var prior = receipts.findById(request.reportId()).orElse(null);
        if (prior != null) {
            if (!prior.getProgress().getBattleId().equals(row.getBattleId())
                    || !prior.getPayload().equals(payload)) {
                throw ErrorCode.BATTLE_PROGRESS_CONFLICT.exception();
            }
            return ack(prior.getAck());
        }
        if (!row.getSourceRevision().equals(request.sourceRevision())
                || row.isClosed()
                || row.getRevision() != request.expectedRevision()
                || sessions.findById(userId)
                        .filter(s -> s.getBattleId().equals(row.getBattleId()))
                        .isEmpty()) {
            throw ErrorCode.BATTLE_PROGRESS_CONFLICT.exception();
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        policies.check(row, normalized, now);
        var previousIds = encounters(row);
        if (!request.encounteredMonsterIds().containsAll(previousIds)) {
            throw ErrorCode.BATTLE_PROGRESS_CONFLICT.exception();
        }
        int delta = request.killedMonsters() - row.getKilledMonsters();
        var newIds = new TreeSet<>(request.encounteredMonsterIds());
        newIds.removeAll(previousIds);
        row.accept(
                normalized, BattleProgressPolicyService.encode(normalized.encounteredMonsterIds()));
        if (delta > 0) {
            quests.recordAcceptedKills(userId, delta, now);
        }
        if (!newIds.isEmpty()) {
            events.publishEvent(
                    new AcceptedBattleEncounters(
                            userId,
                            row.getBattleId(),
                            request.reportId(),
                            Set.copyOf(newIds),
                            now));
        }
        var result = currentAck(row, delta, now);
        receipts.saveAndFlush(
                new BattleProgressReceipt(
                        request.reportId(),
                        row,
                        payload,
                        BattleProgressPolicyService.encode(result)));
        return result;
    }

    /** 이어하지 않는 포기는 마지막 누적 보고와 세션 닫기를 원자적으로 처리한다. 결과 보상은 없다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ProgressAck abandon(Long userId, ProgressReport request) {
        var ack = report(userId, request);
        close(userId, request.battleId(), null);
        sessions.deleteByUserIdAndBattleId(userId, request.battleId());
        return ack;
    }

    /** 전투 종료·취소·새 발급과 같은 계정 잠금/트랜잭션에서 호출한다. 인정한 퀘스트 진행은 유지한다. */
    @Transactional
    public void close(Long userId, String battleId, Integer stageId) {
        progress.findById(battleId)
                .ifPresent(
                        row -> {
                            if (!row.getUser().getId().equals(userId)
                                    || (stageId != null && !stageId.equals(row.getStageId()))) {
                                throw ErrorCode.BATTLE_PROGRESS_CONFLICT.exception();
                            }
                            row.close();
                        });
    }

    private BattleProgress owned(Long userId, String battleId) {
        return progress.findById(battleId)
                .filter(row -> row.getUser().getId().equals(userId))
                .orElseThrow(ErrorCode.BATTLE_NOT_FOUND::exception);
    }

    private State state(BattleProgress row) {
        return new State(
                currentAck(row, 0, clock.instant().truncatedTo(ChronoUnit.MICROS)), row.isClosed());
    }

    private ProgressAck currentAck(BattleProgress row, int delta, Instant now) {
        var day = days.purchaseDay(now);
        return new ProgressAck(
                row.getBattleId(),
                row.getStageId(),
                row.getPolicyVersion(),
                row.getSourceRevision(),
                row.getRevision(),
                row.getKilledMonsters(),
                row.getCompletedWaves(),
                row.getReachedWave(),
                encounters(row),
                delta,
                now.toString(),
                day.format(DateTimeFormatter.BASIC_ISO_DATE),
                day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    private Set<Long> encounters(BattleProgress row) {
        try {
            return JSON.readValue(row.getEncounters(), new TypeReference<TreeSet<Long>>() {});
        } catch (JsonProcessingException e) {
            throw ErrorCode.BATTLE_PROGRESS_UNAVAILABLE.exception();
        }
    }

    private ProgressAck ack(String json) {
        try {
            return JSON.readValue(json, ProgressAck.class);
        } catch (JsonProcessingException e) {
            throw ErrorCode.BATTLE_PROGRESS_UNAVAILABLE.exception();
        }
    }

    /** 현재 서버 원장과 닫힘 여부. progress의 조회 시각은 새 집계 이벤트가 아니다. */
    public record State(ProgressAck progress, boolean closed) {}
}
