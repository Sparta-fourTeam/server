package com.novaserver.quest.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.quest.dto.QuestBoardResponse;
import com.novaserver.quest.dto.QuestReceiptResponse;
import com.novaserver.quest.entity.QuestChest;
import com.novaserver.quest.entity.QuestDefinition;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.entity.UserQuestClaim;
import com.novaserver.quest.entity.UserQuestPeriod;
import com.novaserver.quest.repository.QuestChestRepository;
import com.novaserver.quest.repository.QuestDefinitionRepository;
import com.novaserver.quest.repository.UserQuestClaimRepository;
import com.novaserver.quest.repository.UserQuestPeriodRepository;
import com.novaserver.stage.service.RewardRulesService;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserEnergyRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.WalletRepository;
import com.novaserver.user.service.EnergyPurchasePolicy;
import com.novaserver.user.service.UserRewardService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 서버 행동 집계와 계정·기간별 점수/상자 수령을 원자적으로 처리한다. */
@Service
@RequiredArgsConstructor
public class UserQuestService {
    private final UserRepository accounts;
    private final UserQuestPeriodRepository periods;
    private final UserQuestClaimRepository claims;
    private final QuestDefinitionRepository quests;
    private final QuestChestRepository chests;
    private final WalletRepository wallets;
    private final UserEnergyRepository energies;
    private final UserRewardService rewards;
    private final EnergyPurchasePolicy days;
    private final RewardRulesService locks;
    private final Clock clock;

    /** 게임 토큰 발급 경로에서만 호출한다. 일일 행을 이용해 주간 로그인도 하루 한 번 센다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void recordLogin(Long userId) {
        final var user = lock(userId);
        var now = now();
        var day = days.purchaseDay(now);
        var daily = state(user, QuestPeriod.DAILY, day, now);
        if (daily.getLoginCount() > 0) {
            return;
        }
        daily.login(day, now);
        state(user, QuestPeriod.WEEKLY, periodDay(QuestPeriod.WEEKLY, now), now).login(day, now);
    }

    /** 최초 차감 분기에서 확정한 서버 시각을 받아 같은 트랜잭션·기간으로 집계한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void recordBattleEnergy(Long userId, int amount, Instant eventAt) {
        if (amount <= 0 || eventAt == null) {
            throw ErrorCode.QUEST_INVALID.exception();
        }
        final var user = lock(userId);
        var now = eventAt.truncatedTo(ChronoUnit.MICROS);
        for (var period : QuestPeriod.values()) {
            state(user, period, periodDay(period, now), now).spend(amount, now);
        }
    }

    /** 스폰 범위 검사와 전투 원장 갱신을 마친 같은 트랜잭션에서 차이만 받는다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void recordAcceptedKills(Long userId, int amount, Instant eventAt) {
        if (amount <= 0 || eventAt == null) {
            throw ErrorCode.QUEST_INVALID.exception();
        }
        final var user = lock(userId);
        var now = eventAt.truncatedTo(ChronoUnit.MICROS);
        for (var period : QuestPeriod.values()) {
            state(user, period, periodDay(period, now), now).kill(amount, now);
        }
    }

    /** 현재 서버 기간의 저장된 진행과 수령 여부를 복원한다. 조회는 로그인을 추가 집계하지 않는다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<QuestBoardResponse> snapshot(Long userId) {
        lock(userId);
        var now = now();
        var definitions = quests.findAll();
        var boxes = chests.findAll();
        return Arrays.stream(QuestPeriod.values())
                .map(period -> board(userId, period, now, definitions, boxes))
                .toList();
    }

    /** 명시한 기간의 최초 결과만 확정한다. 과거 수령 재전송은 원래 영수증을 반환한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuestReceiptResponse claim(
            Long userId, String id, QuestPeriod period, String key, boolean chest) {
        final var user = lock(userId);
        var now = now();
        var day = parseDay(key);
        if (period == null || id == null) {
            throw ErrorCode.QUEST_INVALID.exception();
        }
        var prior =
                claims.findByUserIdAndPeriodAndPeriodDayAndDefinitionId(userId, period, day, id);
        if (prior.isPresent()) {
            if (prior.get().isChest() != chest || !prior.get().getDefinitionId().equals(id)) {
                throw ErrorCode.QUEST_INVALID.exception();
            }
            return QuestReceiptResponse.from(prior.get());
        }
        if (!periodDay(period, now).equals(day)) {
            throw ErrorCode.QUEST_PERIOD_EXPIRED.exception();
        }
        locks.lockItemReferences();
        int points = 0;
        int gold = 0;
        int energy = 0;
        int gems = 0;
        Map<String, Integer> items = Map.of();
        if (chest) {
            if (chests.findCanonicalId(id).filter(id::equals).isEmpty()) {
                throw ErrorCode.QUEST_NOT_FOUND.exception(id);
            }
            var box =
                    chests.findById(id).orElseThrow(() -> ErrorCode.QUEST_NOT_FOUND.exception(id));
            if (box.getPeriod() != period || !box.getId().equals(id)) {
                throw ErrorCode.QUEST_INVALID.exception();
            }
            long earned =
                    claims.findByUserIdAndPeriodAndPeriodDay(userId, period, day).stream()
                            .mapToLong(UserQuestClaim::getPoints)
                            .sum();
            if (earned < box.getPoints()) {
                throw ErrorCode.QUEST_NOT_COMPLETE.exception();
            }
            gold = box.getRewardDefinition().getGold();
            items = box.getRewardDefinition().itemAmounts();
            energy = box.getEnergy();
            gems = box.getGems();
            rewards.grant(userId, 0, gold, items);
            grantExtra(userId, energy, gems, now);
        } else {
            if (quests.findCanonicalId(id).filter(id::equals).isEmpty()) {
                throw ErrorCode.QUEST_NOT_FOUND.exception(id);
            }
            var q = quests.findById(id).orElseThrow(() -> ErrorCode.QUEST_NOT_FOUND.exception(id));
            if (q.getPeriod() != period || !q.getId().equals(id)) {
                throw ErrorCode.QUEST_INVALID.exception();
            }
            var progress =
                    periods.findByUserIdAndPeriodAndPeriodDay(userId, period, day).orElse(null);
            if (count(progress, q.getKind()) < q.getTarget()) {
                throw ErrorCode.QUEST_NOT_COMPLETE.exception();
            }
            points = q.getPoints();
        }
        var receipt =
                claims.saveAndFlush(
                        new UserQuestClaim(
                                user, period, day, id, chest, points, gold, energy, gems, items,
                                now));
        return QuestReceiptResponse.from(receipt);
    }

    private void grantExtra(Long userId, int amount, int gems, Instant now) {
        if (gems > 0) {
            var wallet =
                    wallets.findByUserIdForUpdate(userId)
                            .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));
            if ((long) wallet.getGem() + gems > Integer.MAX_VALUE) {
                throw ErrorCode.REWARD_GEM_OVERFLOW.exception();
            }
            wallet.addGem(gems);
        }
        if (amount > 0) {
            var energy =
                    energies.findByUserIdForUpdate(userId)
                            .orElseThrow(() -> ErrorCode.ENERGY_NOT_FOUND.exception(userId));
            energy.recover(now);
            if ((long) energy.getEnergyStored() + amount > Integer.MAX_VALUE) {
                throw ErrorCode.ENERGY_STORAGE_LIMIT.exception();
            }
            energy.addEnergy(amount, now);
        }
    }

    private QuestBoardResponse board(
            Long userId,
            QuestPeriod period,
            Instant now,
            List<QuestDefinition> definitions,
            List<QuestChest> boxes) {
        var day = periodDay(period, now);
        var progress = periods.findByUserIdAndPeriodAndPeriodDay(userId, period, day).orElse(null);
        var receipts =
                claims.findByUserIdAndPeriodAndPeriodDay(userId, period, day).stream()
                        .collect(Collectors.toMap(UserQuestClaim::getDefinitionId, c -> c));
        var rows =
                definitions.stream()
                        .filter(q -> q.getPeriod() == period)
                        .sorted(Comparator.comparing(QuestDefinition::getId))
                        .map(
                                q -> {
                                    var receipt = receipts.get(q.getId());
                                    return new QuestBoardResponse.Row(
                                            q.getId(),
                                            q.getKind(),
                                            Math.min(q.getTarget(), count(progress, q.getKind())),
                                            q.getTarget(),
                                            q.getPoints(),
                                            receipt != null,
                                            receipt == null ? null : receipt.getClaimedAt());
                                })
                        .toList();
        var chestRows =
                boxes.stream()
                        .filter(c -> c.getPeriod() == period)
                        .sorted(
                                Comparator.comparingInt(QuestChest::getPoints)
                                        .thenComparing(QuestChest::getId))
                        .map(
                                c -> {
                                    var receipt = receipts.get(c.getId());
                                    return new QuestBoardResponse.Chest(
                                            c.getId(),
                                            c.getPoints(),
                                            receipt != null,
                                            receipt == null ? null : receipt.getClaimedAt());
                                })
                        .toList();
        return new QuestBoardResponse(
                period,
                key(day),
                days.nextResetAt(day.plusDays(period == QuestPeriod.WEEKLY ? 6 : 0)),
                progress == null || progress.getLastLoginDay() == null
                        ? null
                        : key(progress.getLastLoginDay()),
                count(progress, QuestKind.LOGIN),
                count(progress, QuestKind.SPEND_ENERGY),
                receipts.values().stream().mapToLong(UserQuestClaim::getPoints).sum(),
                rows,
                chestRows);
    }

    private int count(UserQuestPeriod state, QuestKind kind) {
        if (state == null) {
            return 0;
        }
        return switch (kind) {
            case LOGIN -> state.getLoginCount();
            case SPEND_ENERGY -> state.getSpentEnergy();
            case KILL_MONSTERS -> state.getKilledMonsters();
        };
    }

    private UserQuestPeriod state(User user, QuestPeriod period, LocalDate day, Instant now) {
        return periods.findByUserIdAndPeriodAndPeriodDay(user.getId(), period, day)
                .orElseGet(() -> periods.saveAndFlush(new UserQuestPeriod(user, period, day, now)));
    }

    private User lock(Long id) {
        return accounts.findByIdForUpdate(id).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private LocalDate periodDay(QuestPeriod period, Instant now) {
        var day = days.purchaseDay(now);
        return period == QuestPeriod.WEEKLY
                ? day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : day;
    }

    private String key(LocalDate day) {
        return day.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private LocalDate parseDay(String key) {
        try {
            return LocalDate.parse(key, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (java.time.DateTimeException | NullPointerException e) {
            throw ErrorCode.QUEST_INVALID.exception();
        }
    }
}
