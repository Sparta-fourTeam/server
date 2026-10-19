package com.novaserver.user.dto;

/** 이름 또는 초상을 변경한다. 생략한 필드는 유지한다. */
public record UserProfileRequest(String nickname, Integer avatarId) {}
