package com.novaserver.item.controller;

import com.novaserver.item.dto.ItemCreateRequest;
import com.novaserver.item.dto.ItemRequest;
import com.novaserver.item.dto.ItemResponse;
import com.novaserver.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 아이템 관리 관리자용 API. */
@Tag(name = "아이템 관리", description = "아이템 등록·수정·삭제")
@RestController
@RequestMapping("/admin/item")
@RequiredArgsConstructor
public class ItemAdminController {
    private final ItemService itemService;

    @Operation(summary = "아이템 목록 조회", description = "아이템 목록을 조회합니다.")
    @GetMapping("/list")
    public List<ItemResponse> getItems() {
        return itemService.getItemList();
    }

    @Operation(summary = "아이템 상세 조회", description = "해당 아이템 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ItemResponse getItem(@PathVariable String id) {
        return itemService.getItem(id);
    }

    @Operation(summary = "아이템 등록", description = "아이템 정보를 등록합니다.")
    @PostMapping
    public ItemResponse createItem(@Valid @RequestBody ItemCreateRequest request) {
        return itemService.createItem(request);
    }

    @Operation(summary = "아이템 수정", description = "아이템을 수정합니다.")
    @PutMapping("/{id}")
    public ItemResponse updateItem(
            @PathVariable String id, @Valid @RequestBody ItemRequest request) {
        return itemService.updateItem(id, request);
    }

    @Operation(summary = "아이템 삭제", description = "아이템을 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable String id) {
        itemService.deleteItem(id);
    }
}
