package com.novaserver.cards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.cards.dto.EffectTriggerRequest;
import com.novaserver.cards.dto.EffectTriggerResponse;
import com.novaserver.cards.entity.EffectTrigger;
import com.novaserver.cards.repository.EffectTriggerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EffectTriggerServiceTest {

    @Mock private EffectTriggerRepository effectTriggerRepository;

    @InjectMocks private EffectTriggerService effectTriggerService;

    private EffectTrigger trigger(Long id, String name, String label) {
        EffectTrigger trigger = mock(EffectTrigger.class);
        lenient().when(trigger.getId()).thenReturn(id);
        lenient().when(trigger.getName()).thenReturn(name);
        lenient().when(trigger.getLabel()).thenReturn(label);
        return trigger;
    }

    private EffectTriggerRequest request(String name, String label) {
        EffectTriggerRequest request = new EffectTriggerRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "label", label);
        return request;
    }

    @Test
    void returnsListOrderedById() {
        EffectTrigger onHit = trigger(1L, "onHit", "피격 시");
        when(effectTriggerRepository.findAllByOrderByIdAsc()).thenReturn(List.of(onHit));

        List<EffectTriggerResponse> responses = effectTriggerService.getTriggers();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("onHit");
    }

    @Test
    void throwsConflictWhenNameDuplicated() {
        when(effectTriggerRepository.existsByName("onHit")).thenReturn(true);
        EffectTriggerRequest request = request("onHit", "피격 시");

        assertThatThrownBy(() -> effectTriggerService.createTrigger(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 발동 시점 이름입니다");

        verify(effectTriggerRepository, never()).save(any());
    }

    @Test
    void createsTriggerSuccessfully() {
        EffectTrigger saved = trigger(1L, "onHit", "피격 시");
        when(effectTriggerRepository.existsByName("onHit")).thenReturn(false);
        when(effectTriggerRepository.save(any())).thenReturn(saved);
        EffectTriggerRequest request = request("onHit", "피격 시");

        EffectTriggerResponse response = effectTriggerService.createTrigger(request);

        assertThat(response.getName()).isEqualTo("onHit");
    }

    @Test
    void throwsNotFoundWhenUpdatingMissingId() {
        when(effectTriggerRepository.findById(1L)).thenReturn(Optional.empty());
        EffectTriggerRequest request = request("onHit", "피격 시");

        assertThatThrownBy(() -> effectTriggerService.updateTrigger(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 발동 시점입니다");
    }

    @Test
    void throwsConflictWhenNameDuplicatesAnotherTrigger() {
        EffectTrigger existing = trigger(1L, "onHit", "피격 시");
        when(effectTriggerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(effectTriggerRepository.existsByNameAndIdNot("onDeath", 1L)).thenReturn(true);
        EffectTriggerRequest request = request("onDeath", "사망 시");

        assertThatThrownBy(() -> effectTriggerService.updateTrigger(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 발동 시점 이름입니다");
    }

    @Test
    void updatesTriggerSuccessfully() {
        EffectTrigger existing = trigger(1L, "onHit", "피격 시");
        when(effectTriggerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(effectTriggerRepository.existsByNameAndIdNot("onDeath", 1L)).thenReturn(false);
        EffectTriggerRequest request = request("onDeath", "사망 시");

        effectTriggerService.updateTrigger(1L, request);

        verify(existing).update("onDeath", "사망 시");
    }
}
