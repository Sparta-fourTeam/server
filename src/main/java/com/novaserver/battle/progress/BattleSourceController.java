package com.novaserver.battle.progress;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** DB 단일 원본과 내용 주소를 제공한다. */
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
        var current = source.snapshot();
        return raw(current.stagesJson(), current.revision());
    }

    /** 같은 원본 세트의 전투 배속 설정이다. */
    @GetMapping(value = "/combat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> combat() {
        var current = source.snapshot();
        return raw(current.combatJson(), current.revision());
    }

    /** 원문과 manifest를 한 DB 스냅샷으로 내려준다. 중간 갱신이 섞이지 않는다. */
    @GetMapping("/battle-source/snapshot")
    public Download download() {
        var current = source.snapshot();
        return new Download(
                current.manifest(),
                current.stagesJson(),
                current.monstersJson(),
                current.combatJson());
    }

    /** 원문 문자열은 내용 주소 계산에 사용한 UTF-8 바이트를 보존한다. */
    public record Download(
            BattlePolicySource.Manifest manifest,
            String stagesJson,
            String monstersJson,
            String combatJson) {}

    private ResponseEntity<String> raw(String json, String revision) {
        return ResponseEntity.ok()
                .header("X-Battle-Source-Revision", revision)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }
}
