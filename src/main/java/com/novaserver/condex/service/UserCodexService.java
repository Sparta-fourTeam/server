package com.novaserver.condex.service;

import com.novaserver.condex.dto.CodexReceiptResponse;
import com.novaserver.condex.dto.CodexStateResponse;
import com.novaserver.condex.repository.CodexRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.entity.UserCodex;
import com.novaserver.user.repository.UserCodexRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.service.UserRewardService;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 계정 잠금 아래 최초 발견·수령을 저장하며 클라이언트 조우 보고를 직접 받지 않는다. */
@Service
@RequiredArgsConstructor
public class UserCodexService {
    private final UserRepository users;
    private final CodexRepository monsters;
    private final UserCodexRepository states;
    private final UserRewardService rewards;
    private final ObjectProvider<CodexRewardSource> sources;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CodexStateResponse> list(Long userId) {
        return states.findByUserId(userId).stream().map(CodexStateResponse::from).toList();
    }

    /**
     * 서버가 검증을 마친 조우 이벤트 전용 내부 진입점. 동일 몬스터는 최초 한 번만 저장한다. 현재 전투에는 신뢰할 조우 검증이 없어 호출을 연결하지 않는다. 전투 제출
     * ID 목록·처치 수·클라이언트 주장만으로 호출해서는 안 된다. 검증 이벤트와 같은 트랜잭션에서 호출하면 외부 이벤트 실패 시 함께 롤백된다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CodexStateResponse recordVerifiedEncounter(Long userId, Long monsterId) {
        validateId(monsterId);
        var user =
                users.findByIdForUpdate(userId).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
        var previous = states.findByUserIdAndCodexId(userId, monsterId);
        if (previous.isPresent()) {
            return CodexStateResponse.from(previous.get());
        }
        var monster =
                monsters.findById(monsterId)
                        .orElseThrow(() -> ErrorCode.CODEX_MONSTER_NOT_FOUND.exception(monsterId));
        return CodexStateResponse.from(
                states.saveAndFlush(
                        new UserCodex(
                                user, monster, clock.instant().truncatedTo(ChronoUnit.MICROS))));
    }

    /** 계정+몬스터 ID별 최초 수령만 지급하고 재시도는 최초 영수증을 그대로 반환한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CodexReceiptResponse claim(Long userId, Long monsterId) {
        validateId(monsterId);
        users.findByIdForUpdate(userId).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
        var state = states.findByUserIdAndCodexId(userId, monsterId).orElse(null);
        if (state != null && state.isClaimed()) {
            return CodexReceiptResponse.from(state);
        }
        if (!monsters.existsById(monsterId)) {
            throw ErrorCode.CODEX_MONSTER_NOT_FOUND.exception(monsterId);
        }
        if (state == null) {
            throw ErrorCode.CODEX_NOT_DISCOVERED.exception(monsterId);
        }
        var source = sources.getIfAvailable();
        if (source == null) {
            throw ErrorCode.CODEX_REWARD_UNAVAILABLE.exception();
        }
        var definition =
                source.findForMonster(monsterId)
                        .orElseThrow(ErrorCode.CODEX_REWARD_UNAVAILABLE::exception);
        // LocalCodexApi와 동일한 등급별 골드만 허용한다. 임의 아이템/무보상 설정은 지급하지 않는다.
        if (definition.getGold() <= 0 || !definition.getEntries().isEmpty()) {
            throw ErrorCode.CODEX_REWARD_UNAVAILABLE.exception();
        }
        int gold = definition.getGold();
        rewards.grant(userId, 0, gold, Map.of());
        state.claim(gold, clock.instant().truncatedTo(ChronoUnit.MICROS));
        states.saveAndFlush(state);
        return CodexReceiptResponse.from(state);
    }

    private void validateId(Long monsterId) {
        if (monsterId == null || monsterId <= 0) {
            throw ErrorCode.INVALID_REQUEST.exception();
        }
    }
}
