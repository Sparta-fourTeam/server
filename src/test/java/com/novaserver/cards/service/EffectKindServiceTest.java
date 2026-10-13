package com.novaserver.cards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.cards.dto.EffectKindRequest;
import com.novaserver.cards.dto.EffectKindResponse;
import com.novaserver.cards.entity.EffectKind;
import com.novaserver.cards.repository.EffectKindRepository;
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
class EffectKindServiceTest {

    @Mock private EffectKindRepository effectKindRepository;

    @InjectMocks private EffectKindService effectKindService;

    private EffectKind kind(Long id, String name, String label) {
        EffectKind kind = mock(EffectKind.class);
        lenient().when(kind.getId()).thenReturn(id);
        lenient().when(kind.getName()).thenReturn(name);
        lenient().when(kind.getLabel()).thenReturn(label);
        return kind;
    }

    private EffectKindRequest request(String name, String label) {
        EffectKindRequest request = new EffectKindRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "label", label);
        return request;
    }

    @Test
    void returnsListOrderedById() {
        EffectKind damage = kind(1L, "damage", "피해");
        when(effectKindRepository.findAllByOrderByIdAsc()).thenReturn(List.of(damage));

        List<EffectKindResponse> responses = effectKindService.getKinds();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("damage");
    }

    @Test
    void throwsConflictWhenNameDuplicated() {
        when(effectKindRepository.existsByName("damage")).thenReturn(true);
        EffectKindRequest request = request("damage", "피해");

        assertThatThrownBy(() -> effectKindService.createKind(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 효과 종류 이름입니다");

        verify(effectKindRepository, never()).save(any());
    }

    @Test
    void createsKindSuccessfully() {
        EffectKind saved = kind(1L, "damage", "피해");
        when(effectKindRepository.existsByName("damage")).thenReturn(false);
        when(effectKindRepository.save(any())).thenReturn(saved);
        EffectKindRequest request = request("damage", "피해");

        EffectKindResponse response = effectKindService.createKind(request);

        assertThat(response.getName()).isEqualTo("damage");
    }

    @Test
    void throwsNotFoundWhenUpdatingMissingId() {
        when(effectKindRepository.findById(1L)).thenReturn(Optional.empty());
        EffectKindRequest request = request("damage", "피해");

        assertThatThrownBy(() -> effectKindService.updateKind(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 효과 종류입니다");
    }

    @Test
    void throwsConflictWhenNameDuplicatesAnotherKind() {
        EffectKind existing = kind(1L, "damage", "피해");
        when(effectKindRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(effectKindRepository.existsByNameAndIdNot("heal", 1L)).thenReturn(true);
        EffectKindRequest request = request("heal", "회복");

        assertThatThrownBy(() -> effectKindService.updateKind(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 효과 종류 이름입니다");
    }

    @Test
    void updatesKindSuccessfully() {
        EffectKind existing = kind(1L, "damage", "피해");
        when(effectKindRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(effectKindRepository.existsByNameAndIdNot("heal", 1L)).thenReturn(false);
        EffectKindRequest request = request("heal", "회복");

        effectKindService.updateKind(1L, request);

        verify(existing).update("heal", "회복");
    }
}
