package com.novaserver.user.controller;

import com.novaserver.user.dto.UserSummaryResponse;
import com.novaserver.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** WEB 관리자 전용 유저 검색 API. */
@Tag(name = "유저", description = "관리자 유저 검색 API")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserService userService;

    @Operation(summary = "유저 검색", description = "닉네임 부분 일치로 유저를 검색합니다. keyword가 비면 최근 가입순으로 반환합니다.")
    @GetMapping
    public List<UserSummaryResponse> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "20") int size) {
        return userService.searchUsers(keyword, size);
    }
}
