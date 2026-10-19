package com.novaserver.user.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.dto.UserProfileRequest;
import com.novaserver.user.dto.UserProfileResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개 프로필의 영속화와 닉네임 유일성을 관리한다. */
@Service
@RequiredArgsConstructor
public class UserProfileService {
    private static final Pattern NICKNAME = Pattern.compile("[가-힣A-Za-z0-9_]{2,20}");
    private final UserRepository users;

    /** 내 공개 프로필을 조회하며 이전 계정의 기본값을 채운다. */
    @Transactional
    public UserProfileResponse getProfile(Long userId) {
        User user =
                users.findByIdForUpdate(userId)
                        .orElseThrow(() -> ErrorCode.UNAUTHORIZED.exception());
        user.initializeProfile();
        return UserProfileResponse.from(user);
    }

    /** 지정한 필드만 수정하고 중복 이름은 충돌로 거절한다. */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileRequest request) {
        if (request.nickname() == null && request.avatarId() == null) {
            throw ErrorCode.INVALID_REQUEST.exception();
        }
        String nickname =
                request.nickname() == null
                        ? null
                        : Normalizer.normalize(request.nickname().strip(), Normalizer.Form.NFKC);
        if (nickname != null && !NICKNAME.matcher(nickname).matches()) {
            throw ErrorCode.INVALID_NICKNAME.exception();
        }
        if (request.avatarId() != null && (request.avatarId() < 0 || request.avatarId() > 4)) {
            throw ErrorCode.INVALID_AVATAR.exception();
        }
        User user =
                users.findByIdForUpdate(userId)
                        .orElseThrow(() -> ErrorCode.UNAUTHORIZED.exception());
        if (nickname != null
                && users.existsByNicknameKeyAndIdNot(nickname.toLowerCase(Locale.ROOT), userId)) {
            throw ErrorCode.NICKNAME_DUPLICATED.exception();
        }
        user.updateProfile(nickname, request.avatarId());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException error) {
            // 사전 검사 직후 다른 계정이 같은 이름을 저장한 경쟁도 DB unique 제약으로 거절한다.
            throw ErrorCode.NICKNAME_DUPLICATED.exception();
        }
        return UserProfileResponse.from(user);
    }
}
