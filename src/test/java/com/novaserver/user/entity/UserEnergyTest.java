package com.novaserver.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** 유저 에너지 소모·자동회복·보상 획득 규칙 테스트. */
class UserEnergyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 12, 0);

    @Test
    @DisplayName("시간이 지나면 분 단위로 회복하고 남은 시간은 보존한다")
    void shouldRecoverByMinutes_andKeepRemainder() {
        // given
        UserEnergy energy = energyOf(10, NOW);

        // when
        energy.recover(NOW.plusMinutes(3).plusSeconds(30));

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(13);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW.plusMinutes(3));
    }

    @Test
    @DisplayName("1분이 지나지 않으면 회복하지 않고 기준 시각도 바꾸지 않는다")
    void shouldNotRecover_whenLessThanInterval() {
        // given
        UserEnergy energy = energyOf(10, NOW);

        // when
        energy.recover(NOW.plusSeconds(59));

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(10);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("회복은 최대치를 넘지 않고, 최대치에 도달하면 기준 시각이 현재로 갱신된다")
    void shouldCapAtMax_onRecover() {
        // given
        UserEnergy energy = energyOf(28, NOW);

        // when
        energy.recover(NOW.plusHours(1));

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(UserEnergy.MAX_ENERGY);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW.plusHours(1));
    }

    @Test
    @DisplayName("기준 시각이 미래여도 에너지가 줄거나 변하지 않는다")
    void shouldNotChange_whenUpdatedAtIsInFuture() {
        // given
        UserEnergy energy = energyOf(10, NOW.plusMinutes(10));

        // when
        energy.recover(NOW);

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(10);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW.plusMinutes(10));
    }

    @Test
    @DisplayName("에너지를 소모하면 그만큼 줄어든다")
    void shouldDecrease_onConsume() {
        // given
        UserEnergy energy = energyOf(20, NOW);

        // when
        energy.consume(5, NOW);

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(15);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("최대치에서 소모하면 회복 타이머가 지금부터 시작되어 바로 회복되지 않는다")
    void shouldNotRecoverImmediately_afterConsumeFromFull() {
        // given: 기준 시각이 3일 전인 만땅 상태
        UserEnergy energy = energyOf(UserEnergy.MAX_ENERGY, NOW.minusDays(3));

        // when
        energy.recover(NOW);
        energy.consume(5, NOW);
        energy.recover(NOW.plusSeconds(1));

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(UserEnergy.MAX_ENERGY - 5);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("최대치를 초과한 상태에서 소모해도 바로 회복되지 않는다")
    void shouldNotRecoverImmediately_afterConsumeFromOverMax() {
        // given: 보상으로 최대치를 넘긴 상태, 기준 시각은 오래전
        UserEnergy energy = energyOf(UserEnergy.MAX_ENERGY + 10, NOW.minusDays(3));

        // when: 최대치 아래로 떨어지도록 소모
        energy.consume(15, NOW);
        energy.recover(NOW.plusSeconds(1));

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(UserEnergy.MAX_ENERGY - 5);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("최대치 미만에서 소모해도 회복 타이머는 유지된다")
    void shouldKeepTimer_whenConsumeBelowMax() {
        // given
        UserEnergy energy = energyOf(20, NOW.minusSeconds(30));

        // when
        energy.consume(5, NOW);

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(15);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW.minusSeconds(30));
    }

    @Test
    @DisplayName("에너지가 부족하면 소모에 실패하고 값은 그대로다")
    void shouldThrow_whenConsumeMoreThanStored() {
        // given
        UserEnergy energy = energyOf(3, NOW);

        // when & then
        assertThatThrownBy(() -> energy.consume(5, NOW))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(energy.getEnergyStored()).isEqualTo(3);
    }

    @Test
    @DisplayName("보유량과 같은 양은 소모할 수 있다")
    void shouldConsumeAll_whenAmountEqualsStored() {
        // given
        UserEnergy energy = energyOf(5, NOW);

        // when
        energy.consume(5, NOW);

        // then
        assertThat(energy.getEnergyStored()).isZero();
    }

    @Test
    @DisplayName("0 이하의 양은 소모할 수 없다")
    void shouldThrow_whenConsumeNonPositive() {
        // given
        UserEnergy energy = energyOf(10, NOW);

        // when & then
        assertThatThrownBy(() -> energy.consume(0, NOW))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> energy.consume(-1, NOW))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(energy.getEnergyStored()).isEqualTo(10);
    }

    @Test
    @DisplayName("보상을 받으면 에너지가 늘고, 최대치 미만이면 기준 시각은 그대로다")
    void shouldIncrease_onAddEnergy_andKeepTimerBelowMax() {
        // given
        UserEnergy energy = energyOf(10, NOW.minusSeconds(30));

        // when
        energy.addEnergy(7, NOW);

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(17);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW.minusSeconds(30));
    }

    @Test
    @DisplayName("보상은 최대치를 넘길 수 있고, 이때 기준 시각이 현재로 갱신된다")
    void shouldAllowOverMax_onAddEnergy_andResetTimer() {
        // given
        UserEnergy energy = energyOf(25, NOW.minusDays(1));

        // when
        energy.addEnergy(10, NOW);

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(35);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("최대치를 넘긴 상태에서는 자동회복하지 않는다")
    void shouldNotRecover_whenOverMax() {
        // given
        UserEnergy energy = energyOf(UserEnergy.MAX_ENERGY + 5, NOW);

        // when
        energy.recover(NOW.plusHours(1));

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(UserEnergy.MAX_ENERGY + 5);
    }

    @Test
    @DisplayName("0 이하의 보상은 받을 수 없다")
    void shouldThrow_whenAddNonPositive() {
        // given
        UserEnergy energy = energyOf(10, NOW);

        // when & then
        assertThatThrownBy(() -> energy.addEnergy(0, NOW))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> energy.addEnergy(-1, NOW))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(energy.getEnergyStored()).isEqualTo(10);
    }

    private static UserEnergy energyOf(int stored, LocalDateTime updatedAt) {
        UserEnergy energy = new UserEnergy(null, stored, NOW);
        ReflectionTestUtils.setField(energy, "energyUpdatedAt", updatedAt);
        return energy;
    }
}
