package com.novaserver.monster.controller;

import com.novaserver.monster.service.MonsterCatalog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 클라이언트 Monsters 테이블과 동일한 JSON을 반환한다. 전투 조우 증명 API가 아니다. */
@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class MonsterDataController {
    private final MonsterCatalog catalog;

    @Operation(
            summary = "몬스터 전체 데이터 조회",
            description =
                    "클라이언트 Monsters JSON과 같은 행을 반환합니다. "
                            + "버전은 X-Data-Version과 /data/version의 Monsters 값입니다.")
    @GetMapping(value = "/monsters", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> monsters() {
        var download = catalog.download();
        return ResponseEntity.ok()
                .header("X-Data-Version", Integer.toString(download.version()))
                .header("X-Battle-Source-Revision", download.sourceRevision())
                .contentType(MediaType.APPLICATION_JSON)
                .body(download.json());
    }
}
