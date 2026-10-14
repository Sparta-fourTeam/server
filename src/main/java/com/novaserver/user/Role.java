package com.novaserver.user;

/** 유저 권한. ADMIN만 관리자 API(/api/admin/**)에 접근할 수 있다. */
public enum Role {
    USER,
    ADMIN
}
