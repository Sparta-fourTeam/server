package com.novaserver.cards.controller;

import com.novaserver.cards.dto.EffectTriggerRequest;
import com.novaserver.cards.dto.EffectTriggerResponse;
import com.novaserver.cards.service.EffectTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 효과 발동 시점 관리 API. */
@Tag(name = "효과 발동 시점", description = "발동 시점 목록·등록·수정·삭제")
@RestController
@RequestMapping("/admin/effect-triggers")
@RequiredArgsConstructor
public class EffectTriggerController {
    private final EffectTriggerService effectTriggerService;

    @Operation(summary = "발동 시점 목록", description = "등록된 발동 시점을 id 순으로 조회합니다.")
    @GetMapping
    public List<EffectTriggerResponse> getTriggers() {
        return effectTriggerService.getTriggers();
    }

    @Operation(summary = "발동 시점 등록", description = "이름이 이미 있으면 409를 반환합니다.")
    @PostMapping
    public EffectTriggerResponse createTrigger(@Valid @RequestBody EffectTriggerRequest request) {
        return effectTriggerService.createTrigger(request);
    }

    @Operation(summary = "발동 시점 수정", description = "없는 id면 404, 다른 발동 시점과 이름이 겹치면 409 반환합니다.")
    @PutMapping("/{id}")
    public EffectTriggerResponse updateTrigger(
            @PathVariable Long id, @Valid @RequestBody EffectTriggerRequest request) {
        return effectTriggerService.updateTrigger(id, request);
    }

}
