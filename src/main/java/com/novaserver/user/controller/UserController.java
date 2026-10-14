package com.novaserver.user.controller;

import com.novaserver.user.dto.UserDataResponse;
import com.novaserver.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 유저 상태 조회 API. */
@Tag(name = "유저", description = "유저 상태 조회 API")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "토큰의 유저 id로 유저 상태 정보를 조회합니다.")
    @GetMapping("/me")
    public UserDataResponse getMe(@AuthenticationPrincipal Long userId) {
        return userService.getMe(userId);
    }
}
