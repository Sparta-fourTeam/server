package com.novaserver.cards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.cards.dto.CardEffectRequest;
import com.novaserver.cards.dto.CardEffectResponse;
import com.novaserver.cards.entity.CardEffect;
import com.novaserver.cards.entity.CardVariant;
import com.novaserver.cards.entity.EffectKind;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.CardEffectRepository;
import com.novaserver.cards.repository.EffectKindRepository;
import com.novaserver.cards.repository.EffectTriggerRepository;
import com.novaserver.cards.repository.SkillFormRepository;
import com.novaserver.skill.repository.SkillRepository;
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
class CardEffectServiceTest {

    @Mock private CardEffectRepository cardEffectRepository;
    @Mock private CardService cardService;
    @Mock private CardVariantService cardVariantService;
    @Mock private EffectKindRepository effectKindRepository;
    @Mock private SkillFormRepository skillFormRepository;
    @Mock private EffectTriggerRepository effectTriggerRepository;
    @Mock private SkillRepository skillRepository;

    @InjectMocks private CardEffectService cardEffectService;

    private UpgradeCard card(Long id) {
        UpgradeCard card = mock(UpgradeCard.class);
        lenient().when(card.getId()).thenReturn(id);
        return card;
    }

    private CardVariant variant(Long id) {
        CardVariant variant = mock(CardVariant.class);
        lenient().when(variant.getId()).thenReturn(id);
        return variant;
    }

    private EffectKind kind(Long id, String name) {
        EffectKind kind = mock(EffectKind.class);
        lenient().when(kind.getId()).thenReturn(id);
        lenient().when(kind.getName()).thenReturn(name);
        return kind;
    }

    private CardEffect effect(Long id, EffectKind kind) {
        CardEffect effect = mock(CardEffect.class);
        lenient().when(effect.getId()).thenReturn(id);
        lenient().when(effect.getSortOrder()).thenReturn(0);
        lenient().when(effect.getKind()).thenReturn(kind);
        lenient().when(effect.getValue()).thenReturn(10.0);
        lenient().when(effect.getChance()).thenReturn(1.0);
        lenient().when(effect.getDamageScale()).thenReturn(1.0);
        lenient().when(effect.getCount()).thenReturn(1);
        lenient().when(effect.isExcludeHit()).thenReturn(false);
        return effect;
    }

    private CardEffectRequest request(String kind) {
        CardEffectRequest request = new CardEffectRequest();
        ReflectionTestUtils.setField(request, "kind", kind);
        return request;
    }

    @Test
    void returnsCardEffectsOrderedBySortOrder() {
        UpgradeCard card = card(1L);
        EffectKind kind = kind(1L, "damage");
        CardEffect effect = effect(1L, kind);
        when(cardService.findCard(1L)).thenReturn(card);
        when(cardEffectRepository.findAllByCard_IdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(effect));

        List<CardEffectResponse> responses = cardEffectService.getCardEffects(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).kind()).isEqualTo("damage");
    }

    @Test
    void returnsVariantEffectsOrderedBySortOrder() {
        CardVariant variant = variant(1L);
        EffectKind kind = kind(1L, "damage");
        CardEffect effect = effect(1L, kind);
        when(cardVariantService.findVariant(1L)).thenReturn(variant);
        when(cardEffectRepository.findAllByVariant_IdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(effect));

        List<CardEffectResponse> responses = cardEffectService.getVariantEffects(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).kind()).isEqualTo("damage");
    }

    @Test
    void throwsNotFoundWhenKindMissingOnCreate() {
        UpgradeCard card = card(1L);
        when(cardService.findCard(1L)).thenReturn(card);
        when(effectKindRepository.findByName("unknown")).thenReturn(Optional.empty());
        CardEffectRequest request = request("unknown");

        assertThatThrownBy(() -> cardEffectService.createCardEffect(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 효과 종류입니다");

        verify(cardEffectRepository, never()).save(any());
    }

    @Test
    void createsCardEffectSuccessfully() {
        UpgradeCard card = card(1L);
        EffectKind kind = kind(1L, "damage");
        CardEffect saved = effect(1L, kind);
        when(cardService.findCard(1L)).thenReturn(card);
        when(effectKindRepository.findByName("damage")).thenReturn(Optional.of(kind));
        when(cardEffectRepository.save(any())).thenReturn(saved);
        CardEffectRequest request = request("damage");

        CardEffectResponse response = cardEffectService.createCardEffect(1L, request);

        assertThat(response.kind()).isEqualTo("damage");
    }

    @Test
    void createsVariantEffectByDelegatingToVariantLookup() {
        CardVariant variant = variant(1L);
        EffectKind kind = kind(1L, "damage");
        CardEffect saved = effect(1L, kind);
        when(cardVariantService.findVariant(1L)).thenReturn(variant);
        when(effectKindRepository.findByName("damage")).thenReturn(Optional.of(kind));
        when(cardEffectRepository.save(any())).thenReturn(saved);
        CardEffectRequest request = request("damage");

        CardEffectResponse response = cardEffectService.createVariantEffect(1L, request);

        assertThat(response.kind()).isEqualTo("damage");
        verify(cardService, never()).findCard(any());
    }

    @Test
    void throwsNotFoundWhenUpdatingMissingEffect() {
        when(cardEffectRepository.findById(1L)).thenReturn(Optional.empty());
        CardEffectRequest request = request("damage");

        assertThatThrownBy(() -> cardEffectService.updateEffect(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 카드 효과입니다");
    }

    @Test
    void updatesEffectSuccessfully() {
        EffectKind kind = kind(1L, "damage");
        CardEffect existing = effect(1L, kind);
        when(cardEffectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(effectKindRepository.findByName("damage")).thenReturn(Optional.of(kind));
        CardEffectRequest request = request("damage");

        CardEffectResponse response = cardEffectService.updateEffect(1L, request);

        assertThat(response.kind()).isEqualTo("damage");
        verify(existing)
                .update(
                        eq(0), eq(kind), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                        isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void deletesEffectSuccessfully() {
        EffectKind kind = kind(1L, "damage");
        CardEffect existing = effect(1L, kind);
        when(cardEffectRepository.findById(1L)).thenReturn(Optional.of(existing));

        cardEffectService.deleteEffect(1L);

        verify(cardEffectRepository).delete(existing);
    }
}
