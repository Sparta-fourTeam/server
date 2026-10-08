package com.novaserver.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ErrorCodeTest {

    @Test
    void buildsExceptionWithStatusAndMessage() {
        ResponseStatusException exception = ErrorCode.BATTLE_NOT_FOUND.exception();

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("진행 중인 배틀이 없습니다");
    }

    @Test
    void fillsMessagePlaceholdersInOrder() {
        ResponseStatusException exception = ErrorCode.SKILL_NOT_FOUND.exception();
        assertThat(exception.getReason()).isEqualTo("존재하지 않는 스킬입니다");

        ResponseStatusException duplicated = ErrorCode.SKILL_DUPLICATED.exception("파이어볼");
        assertThat(duplicated.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicated.getReason()).isEqualTo("이미 존재하는 스킬명입니다 : 파이어볼");
    }
}
