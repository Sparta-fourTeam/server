package com.novaserver.cards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.cards.dto.RequiredCardCountRequest;
import com.novaserver.cards.dto.RequiredCardCountResponse;
import com.novaserver.cards.entity.RequiredCardCount;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.RequiredCardCountRepository;
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
class RequiredCardCountServiceTest {

    @Mock private RequiredCardCountRepository requiredCardCountRepository;
    @Mock private CardService cardService;
    @Mock private SkillRepository skillRepository;

    @InjectMocks private RequiredCardCountService requiredCardCountService;

    private UpgradeCard card(Long id, String name) {
        UpgradeCard card = mock(UpgradeCard.class);
        lenient().when(card.getId()).thenReturn(id);
        lenient().when(card.getName()).thenReturn(name);
        return card;
    }

    private RequiredCardCount requiredCardCount(Long id, UpgradeCard requiredCard, int count) {
        RequiredCardCount requiredCardCount = mock(RequiredCardCount.class);
        lenient().when(requiredCardCount.getId()).thenReturn(id);
        lenient().when(requiredCardCount.getRequiredCard()).thenReturn(requiredCard);
        lenient().when(requiredCardCount.getSkill()).thenReturn(null);
        lenient().when(requiredCardCount.getCount()).thenReturn(count);
        return requiredCardCount;
    }

    private RequiredCardCountRequest request(Long requiredCardId, Integer count) {
        RequiredCardCountRequest request = new RequiredCardCountRequest();
        ReflectionTestUtils.setField(request, "requiredCardId", requiredCardId);
        ReflectionTestUtils.setField(request, "count", count);
        return request;
    }

    @Test
    void returnsRequiredCardCountList() {
        UpgradeCard card = card(1L, "카드");
        UpgradeCard requiredCard = card(2L, "선행카드");
        RequiredCardCount requiredCardCount = requiredCardCount(1L, requiredCard, 2);
        when(cardService.findCard(1L)).thenReturn(card);
        when(requiredCardCountRepository.findAllByCardId(1L))
                .thenReturn(List.of(requiredCardCount));

        List<RequiredCardCountResponse> responses =
                requiredCardCountService.getRequiredCardCounts(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).count()).isEqualTo(2);
    }

    @Test
    void throwsBadRequestWhenSelfReferenced() {
        RequiredCardCountRequest request = request(1L, 2);

        assertThatThrownBy(() -> requiredCardCountService.createRequiredCardCount(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("자기 자신을 선행 카드로 지정할 수 없습니다");

        verify(requiredCardCountRepository, never()).save(any());
    }

    @Test
    void throwsConflictWhenRequiredCardAlreadyRegistered() {
        UpgradeCard card = card(1L, "카드");
        UpgradeCard requiredCard = card(2L, "선행카드");
        when(cardService.findCard(1L)).thenReturn(card);
        when(cardService.findCard(2L)).thenReturn(requiredCard);
        when(requiredCardCountRepository.existsByCardIdAndRequiredCardId(1L, 2L)).thenReturn(true);
        RequiredCardCountRequest request = request(2L, 2);

        assertThatThrownBy(() -> requiredCardCountService.createRequiredCardCount(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 등록된 선행 카드 조건입니다");

        verify(requiredCardCountRepository, never()).save(any());
    }

    @Test
    void createsRequiredCardCountSuccessfully() {
        UpgradeCard card = card(1L, "카드");
        UpgradeCard requiredCard = card(2L, "선행카드");
        RequiredCardCount saved = requiredCardCount(1L, requiredCard, 2);
        when(cardService.findCard(1L)).thenReturn(card);
        when(cardService.findCard(2L)).thenReturn(requiredCard);
        when(requiredCardCountRepository.existsByCardIdAndRequiredCardId(1L, 2L)).thenReturn(false);
        when(requiredCardCountRepository.save(any())).thenReturn(saved);
        RequiredCardCountRequest request = request(2L, 2);

        RequiredCardCountResponse response =
                requiredCardCountService.createRequiredCardCount(1L, request);

        assertThat(response.count()).isEqualTo(2);
        assertThat(response.requiredCard().name()).isEqualTo("선행카드");
    }

    @Test
    void throwsNotFoundWhenDeletingMissingId() {
        when(requiredCardCountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requiredCardCountService.deleteRequiredCardCount(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 선행 카드 조건입니다");
    }

    @Test
    void deletesRequiredCardCountSuccessfully() {
        UpgradeCard requiredCard = card(2L, "선행카드");
        RequiredCardCount existing = requiredCardCount(1L, requiredCard, 2);
        when(requiredCardCountRepository.findById(1L)).thenReturn(Optional.of(existing));

        requiredCardCountService.deleteRequiredCardCount(1L);

        verify(requiredCardCountRepository).delete(existing);
    }
}
