package com.novaserver.user.controller;

import com.novaserver.user.dto.UserProfileRequest;
import com.novaserver.user.dto.UserProfileResponse;
import com.novaserver.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증된 계정의 공개 프로필만 수정한다. */
@Tag(name = "프로필")
@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService profiles;

    @GetMapping
    public UserProfileResponse get(@AuthenticationPrincipal Long userId) {
        return profiles.getProfile(userId);
    }

    @PostMapping
    public UserProfileResponse update(
            @AuthenticationPrincipal Long userId, @RequestBody UserProfileRequest request) {
        return profiles.updateProfile(userId, request);
    }
}
