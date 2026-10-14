package com.novaserver.cards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.cards.dto.CardVariantRequest;
import com.novaserver.cards.dto.CardVariantResponse;
import com.novaserver.cards.entity.CardVariant;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.CardVariantRepository;
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
class CardVariantServiceTest {

    @Mock private CardVariantRepository cardVariantRepository;
    @Mock private CardService cardService;

    @InjectMocks private CardVariantService cardVariantService;

    private UpgradeCard card(Long id) {
        UpgradeCard card = mock(UpgradeCard.class);
        lenient().when(card.getId()).thenReturn(id);
        return card;
    }

    private CardVariant variant(Long id, UpgradeCard card, int minPermanentLevel, String name) {
        CardVariant variant = mock(CardVariant.class);
        lenient().when(variant.getId()).thenReturn(id);
        lenient().when(variant.getCard()).thenReturn(card);
        lenient().when(variant.getMinPermanentLevel()).thenReturn(minPermanentLevel);
        lenient().when(variant.getName()).thenReturn(name);
        lenient().when(variant.getEffects()).thenReturn(List.of());
        return variant;
    }

    private CardVariantRequest request(int minPermanentLevel, String name) {
        CardVariantRequest request = new CardVariantRequest();
        ReflectionTestUtils.setField(request, "minPermanentLevel", minPermanentLevel);
        ReflectionTestUtils.setField(request, "name", name);
        return request;
    }

    @Test
    void returnsVariantListOrderedByLevel() {
        UpgradeCard card = card(1L);
        CardVariant variant = variant(1L, card, 5, "상위형");
        when(cardService.findCard(1L)).thenReturn(card);
        when(cardVariantRepository.findAllByCardIdOrderByMinPermanentLevelAsc(1L))
                .thenReturn(List.of(variant));

        List<CardVariantResponse> responses = cardVariantService.getVariants(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("상위형");
    }

    @Test
    void throwsConflictWhenMinPermanentLevelDuplicatedOnCreate() {
        UpgradeCard card = card(1L);
        when(cardService.findCard(1L)).thenReturn(card);
        when(cardVariantRepository.existsByCardIdAndMinPermanentLevel(1L, 5)).thenReturn(true);
        CardVariantRequest request = request(5, "상위형");

        assertThatThrownBy(() -> cardVariantService.createVariant(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 변형 조건입니다");

        verify(cardVariantRepository, never()).save(any());
    }

    @Test
    void createsVariantSuccessfully() {
        UpgradeCard card = card(1L);
        CardVariant saved = variant(1L, card, 5, "상위형");
        when(cardService.findCard(1L)).thenReturn(card);
        when(cardVariantRepository.existsByCardIdAndMinPermanentLevel(1L, 5)).thenReturn(false);
        when(cardVariantRepository.save(any())).thenReturn(saved);
        CardVariantRequest request = request(5, "상위형");

        CardVariantResponse response = cardVariantService.createVariant(1L, request);

        assertThat(response.name()).isEqualTo("상위형");
    }

    @Test
    void throwsNotFoundWhenUpdatingMissingVariant() {
        when(cardVariantRepository.findById(1L)).thenReturn(Optional.empty());
        CardVariantRequest request = request(5, "상위형");

        assertThatThrownBy(() -> cardVariantService.updateVariant(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 카드 변형입니다");
    }

    @Test
    void throwsConflictWhenUpdateDuplicatesAnotherVariant() {
        UpgradeCard card = card(1L);
        CardVariant existing = variant(1L, card, 5, "상위형");
        when(cardVariantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cardVariantRepository.existsByCardIdAndMinPermanentLevelAndIdNot(1L, 9, 1L))
                .thenReturn(true);
        CardVariantRequest request = request(9, "변경됨");

        assertThatThrownBy(() -> cardVariantService.updateVariant(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 변형 조건입니다");
    }

    @Test
    void updatesVariantSuccessfully() {
        UpgradeCard card = card(1L);
        CardVariant existing = variant(1L, card, 5, "상위형");
        when(cardVariantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cardVariantRepository.existsByCardIdAndMinPermanentLevelAndIdNot(1L, 9, 1L))
                .thenReturn(false);
        CardVariantRequest request = request(9, "변경됨");

        cardVariantService.updateVariant(1L, request);

        verify(existing).update(9, "변경됨", null);
    }

    @Test
    void deletesVariantSuccessfully() {
        UpgradeCard card = card(1L);
        CardVariant existing = variant(1L, card, 5, "상위형");
        when(cardVariantRepository.findById(1L)).thenReturn(Optional.of(existing));

        cardVariantService.deleteVariant(1L);

        verify(cardVariantRepository).delete(existing);
    }
}
