package com.novaserver.monster.controller;

import com.novaserver.monster.dto.MonsterRequest;
import com.novaserver.monster.dto.MonsterResponse;
import com.novaserver.monster.service.MonsterService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 몬스터 등록(관리자)과 전체 조회 API. 경로 앞에는 WebConfig가 /api를 붙인다. */
@RestController
@RequiredArgsConstructor
public class MonsterController {
    private final MonsterService monsterService;

    @PostMapping("/admin/monsters")
    public MonsterResponse createMonster(@Valid @RequestBody MonsterRequest request) {
        return monsterService.createMonster(request);
    }

    @GetMapping("/monsters")
    public List<MonsterResponse> getMonsters() {
        return monsterService.getMonsters();
    }
}
