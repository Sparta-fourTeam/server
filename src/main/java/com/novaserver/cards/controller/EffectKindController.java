package com.novaserver.cards.controller;

import com.novaserver.cards.dto.EffectKindRequest;
import com.novaserver.cards.dto.EffectKindResponse;
import com.novaserver.cards.service.EffectKindService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 효과 종류 관리 API. */
@Tag(name = "효과 종류", description = "종류 목록·등록·수정·삭제")
@RestController
@RequestMapping("/admin/effect-kinds")
@RequiredArgsConstructor
public class EffectKindController {
    private final EffectKindService effectKindService;

    @Operation(summary = "종류 목록", description = "등록된 효과 종류를 id 순으로 조회합니다.")
    @GetMapping
    public List<EffectKindResponse> getKinds() {
        return effectKindService.getKinds();
    }

    @Operation(summary = "종류 등록", description = "이름이 이미 있으면 409를 반환합니다.")
    @PostMapping
    public EffectKindResponse createKind(@Valid @RequestBody EffectKindRequest request) {
        return effectKindService.createKind(request);
    }

    @Operation(summary = "종류 수정", description = "없는 id면 404, 다른 종류와 이름이 겹치면 409 반환합니다.")
    @PutMapping("/{id}")
    public EffectKindResponse updateKind(
            @PathVariable Long id, @Valid @RequestBody EffectKindRequest request) {
        return effectKindService.updateKind(id, request);
    }
}
