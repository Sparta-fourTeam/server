package com.novaserver.battle.progress;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 같은 배포물의 원문과 내용 주소를 함께 제공한다. 기존 Monsters 공급은 해당 API를 쓴다. */
@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class BattleSourceController {
    private final BattlePolicySource source;

    /** 원문 세트 전체의 동일 버전 확인 정보다. */
    @GetMapping("/battle-source")
    public BattlePolicySource.Manifest manifest() {
        return source.manifest();
    }

    /** 서버가 검증·스폰 정책에 사용한 스테이지 원문이다. */
    @GetMapping(value = "/stages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> stages() {
        return raw(source.stagesJson());
    }

    /** 같은 원본 세트의 전투 배속 설정이다. */
    @GetMapping(value = "/combat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> combat() {
        return raw(source.combatJson());
    }

    private ResponseEntity<String> raw(String json) {
        return ResponseEntity.ok()
                .header("X-Battle-Source-Revision", source.revision())
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }
}
