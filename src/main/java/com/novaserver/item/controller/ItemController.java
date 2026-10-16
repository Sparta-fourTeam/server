package com.novaserver.item.controller;

import com.novaserver.item.dto.ItemResponse;
import com.novaserver.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 아이템 조회 API. */
@Tag(name = "아이템 조회", description = "아이템 조회")
@RestController
@RequestMapping("/item")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @Operation(summary = "(유니티) 아이템 데이터 조회", description = "아이템 데이터를 전체 조회합니다.")
    @GetMapping
    public List<ItemResponse> getItemData() {
        return itemService.getItemList();
    }
}
