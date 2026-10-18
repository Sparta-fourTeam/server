package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.novaserver.playerlevel.dto.PlayerLevelConfigResponse;
import com.novaserver.playerlevel.service.PlayerLevelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlayerLevelPolicyTest {
    @ParameterizedTest
    @CsvSource({
        "0,1,true",
        "9,2,false",
        "10,2,true",
        "24,3,false",
        "25,3,true",
        "45,4,true",
        "2147483647,5,false"
    })
    @DisplayName("테스트 기준 데이터의 누적 경험치 경계와 최고 플레이어 레벨을 따른다")
    void shouldCheckCumulativeThreshold(int exp, int requiredLevel, boolean expected) {
        // Given: 다음 레벨 경험치는 10, 15, 20이고 최고 레벨은 4다.
        var service = mock(PlayerLevelService.class);
        when(service.getConfig()).thenReturn(new PlayerLevelConfigResponse(10, 5, 4, 1));
        var policy = new PlayerLevelPolicy(service);

        // When / Then
        assertThat(policy.hasReached(exp, requiredLevel)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"-1,1", "0,1", "9,1", "10,2", "24,2", "25,3", "44,3", "45,4", "2147483647,4"})
    void levelAtUsesTheSameUnlockBoundaries(int exp, int expected) {
        var service = mock(PlayerLevelService.class);
        when(service.getConfig()).thenReturn(new PlayerLevelConfigResponse(10, 5, 4, 1));
        assertThat(new PlayerLevelPolicy(service).levelAt(exp)).isEqualTo(expected);
    }
}
