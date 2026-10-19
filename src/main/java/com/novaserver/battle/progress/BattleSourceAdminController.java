package com.novaserver.battle.progress;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기존 ADMIN 인증 경로로 전투 원본을 관리한다. */
@RestController
@RequestMapping("/admin/data/battle-source")
@RequiredArgsConstructor
public class BattleSourceAdminController {
    private final BattleSourceAdminService service;

    /** 검증된 원본 세트를 원자적으로 게시한다. */
    @PutMapping
    public BattlePolicySource.Manifest publish(
            @Valid @RequestBody BattleSourceAdminService.Publication request) {
        return service.publish(request);
    }
}
