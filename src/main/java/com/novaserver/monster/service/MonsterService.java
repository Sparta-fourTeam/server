package com.novaserver.monster.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.monster.dto.MonsterPassiveRequest;
import com.novaserver.monster.dto.MonsterRequest;
import com.novaserver.monster.dto.MonsterResponse;
import com.novaserver.monster.entity.Monster;
import com.novaserver.monster.repository.MonsterRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 몬스터 등록과 조회를 담당한다. */
@Service
@RequiredArgsConstructor
public class MonsterService {

    private final MonsterRepository monsterRepository;
    private final MonsterPassiveFactory monsterPassiveFactory;

    /**
     * 몬스터와 패시브를 검증해 함께 저장한다.
     *
     * @param request 몬스터 등록 요청
     * @return 저장된 몬스터
     */
    @Transactional
    public MonsterResponse createMonster(MonsterRequest request) {
        if (monsterRepository.existsById(request.getId())) {
            throw ErrorCode.MONSTER_DUPLICATED.exception(request.getId());
        }

        Monster created =
                new Monster(
                        request.getId(),
                        request.getHp(),
                        request.getDamage(),
                        request.getSpeed(),
                        request.getAttackInterval(),
                        request.getAttackRange(),
                        request.getProjectileSpeed(),
                        request.getGrade(),
                        request.getBurstCount(),
                        request.getBurstInterval(),
                        request.getResists(),
                        request.getCastResists(),
                        request.isBlocksProjectile());

        List<MonsterPassiveRequest> passives = request.getPassives();
        if (passives != null) {
            addPassives(passives, created);
        }

        return MonsterResponse.from(monsterRepository.save(created));
    }

    private void addPassives(List<MonsterPassiveRequest> passives, Monster monster) {
        for (MonsterPassiveRequest passive : passives) {
            monster.addPassive(monsterPassiveFactory.create(passive, monster));
        }
    }

    /**
     * 전체 몬스터를 ID 순으로 조회한다.
     *
     * @return 몬스터 목록
     */
    @Transactional(readOnly = true)
    public List<MonsterResponse> getMonsters() {
        return monsterRepository.findAllByOrderByIdAsc().stream()
                .map(MonsterResponse::from)
                .toList();
    }
}
