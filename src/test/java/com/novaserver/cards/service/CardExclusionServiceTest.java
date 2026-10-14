package com.novaserver.cards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.cards.dto.CardExclusionRequest;
import com.novaserver.cards.dto.CardExclusionResponse;
import com.novaserver.cards.entity.CardExclusion;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.CardExclusionRepository;
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
class CardExclusionServiceTest {

    @Mock private CardExclusionRepository cardExclusionRepository;
    @Mock private CardService cardService;
    @Mock private SkillRepository skillRepository;

    @InjectMocks private CardExclusionService cardExclusionService;

    private UpgradeCard card(Long id, String name) {
        UpgradeCard card = mock(UpgradeCard.class);
        lenient().when(card.getId()).thenReturn(id);
        lenient().when(card.getName()).thenReturn(name);
        return card;
    }

    private CardExclusion exclusion(Long id, UpgradeCard excludingCard, int belowPermanentLevel) {
        CardExclusion exclusion = mock(CardExclusion.class);
        lenient().when(exclusion.getId()).thenReturn(id);
        lenient().when(exclusion.getExcludingCard()).thenReturn(excludingCard);
        lenient().when(exclusion.getBelowPermanentLevel()).thenReturn(belowPermanentLevel);
        return exclusion;
    }

    private CardExclusionRequest request(Long excludingCardId, Integer belowPermanentLevel) {
        CardExclusionRequest request = new CardExclusionRequest();
        ReflectionTestUtils.setField(request, "excludingCardId", excludingCardId);
        ReflectionTestUtils.setField(request, "belowPermanentLevel", belowPermanentLevel);
        return request;
    }

    @Test
    void returnsExclusionList() {
        UpgradeCard card = card(1L, "카드");
        UpgradeCard excludingCard = card(2L, "배제카드");
        CardExclusion exclusion = exclusion(1L, excludingCard, 0);
        when(cardService.findCard(1L)).thenReturn(card);
        when(cardExclusionRepository.findAllByCardId(1L)).thenReturn(List.of(exclusion));

        List<CardExclusionResponse> responses = cardExclusionService.getExclusions(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).excludingCard().name()).isEqualTo("배제카드");
    }

    @Test
    void throwsBadRequestWhenSelfReferenced() {
        CardExclusionRequest request = request(1L, 0);

        assertThatThrownBy(() -> cardExclusionService.createExclusion(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("자기 자신을 배제 카드로 지정할 수 없습니다");

        verify(cardExclusionRepository, never()).save(any());
    }

    @Test
    void throwsConflictWhenExcludingCardAlreadyRegistered() {
        UpgradeCard card = card(1L, "카드");
        UpgradeCard excludingCard = card(2L, "배제카드");
        when(cardService.findCard(1L)).thenReturn(card);
        when(cardService.findCard(2L)).thenReturn(excludingCard);
        when(cardExclusionRepository.existsByCardIdAndExcludingCardId(1L, 2L)).thenReturn(true);
        CardExclusionRequest request = request(2L, 0);

        assertThatThrownBy(() -> cardExclusionService.createExclusion(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 등록된 배제 조건입니다");

        verify(cardExclusionRepository, never()).save(any());
    }

    @Test
    void createsExclusionSuccessfully() {
        UpgradeCard card = card(1L, "카드");
        UpgradeCard excludingCard = card(2L, "배제카드");
        CardExclusion saved = exclusion(1L, excludingCard, 25);
        when(cardService.findCard(1L)).thenReturn(card);
        when(cardService.findCard(2L)).thenReturn(excludingCard);
        when(cardExclusionRepository.existsByCardIdAndExcludingCardId(1L, 2L)).thenReturn(false);
        when(cardExclusionRepository.save(any())).thenReturn(saved);
        CardExclusionRequest request = request(2L, 25);

        CardExclusionResponse response = cardExclusionService.createExclusion(1L, request);

        assertThat(response.belowPermanentLevel()).isEqualTo(25);
        assertThat(response.excludingCard().name()).isEqualTo("배제카드");
    }

    @Test
    void throwsNotFoundWhenDeletingMissingId() {
        when(cardExclusionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardExclusionService.deleteExclusion(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 배제 조건입니다");
    }

    @Test
    void deletesExclusionSuccessfully() {
        UpgradeCard excludingCard = card(2L, "배제카드");
        CardExclusion existing = exclusion(1L, excludingCard, 0);
        when(cardExclusionRepository.findById(1L)).thenReturn(Optional.of(existing));

        cardExclusionService.deleteExclusion(1L);

        verify(cardExclusionRepository).delete(existing);
    }
}
