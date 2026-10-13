package com.novaserver.cards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.cards.dto.CardDataResponse;
import com.novaserver.cards.dto.CardDetailResponse;
import com.novaserver.cards.dto.CardRequest;
import com.novaserver.cards.dto.CardSummaryResponse;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.UpgradeCardRepository;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private UpgradeCardRepository upgradeCardRepository;
    @Mock private SkillRepository skillRepository;

    @InjectMocks private CardService cardService;

    private Skill skill(Long id, String name, int maxLevel) {
        Skill skill = mock(Skill.class);
        lenient().when(skill.getId()).thenReturn(id);
        lenient().when(skill.getName()).thenReturn(name);
        lenient().when(skill.getMaxLevel()).thenReturn(maxLevel);
        return skill;
    }

    private UpgradeCard card(Long id, String name, Skill skill) {
        UpgradeCard card = mock(UpgradeCard.class);
        lenient().when(card.getId()).thenReturn(id);
        lenient().when(card.getName()).thenReturn(name);
        lenient().when(card.isEnabled()).thenReturn(true);
        lenient().when(card.getMaxPickCount()).thenReturn(1);
        lenient().when(card.getSkill()).thenReturn(skill);
        lenient().when(card.getRequiredCardCounts()).thenReturn(List.of());
        lenient().when(card.getExclusions()).thenReturn(List.of());
        lenient().when(card.getEffects()).thenReturn(List.of());
        lenient().when(card.getVariants()).thenReturn(List.of());
        return card;
    }

    private CardRequest request(Long skillId, String name) {
        CardRequest request = new CardRequest();
        ReflectionTestUtils.setField(request, "skillId", skillId);
        ReflectionTestUtils.setField(request, "name", name);
        return request;
    }

    private CardRequest request(Long skillId, String name, Integer minBattleLevel) {
        CardRequest request = request(skillId, name);
        ReflectionTestUtils.setField(request, "minBattleLevel", minBattleLevel);
        return request;
    }

    @Test
    void returnsCardList() {
        Skill skill = skill(1L, "화염검", 10);
        UpgradeCard card = card(1L, "카드", skill);
        when(upgradeCardRepository.findAll()).thenReturn(List.of(card));

        List<CardSummaryResponse> responses = cardService.getCards();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("카드");
    }

    @Test
    void throwsNotFoundWhenGettingMissingCard() {
        when(upgradeCardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCard(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 강화 카드입니다");
    }

    @Test
    void returnsCardDataGroupedBySkillId() {
        Skill skill = skill(1L, "화염검", 10);
        UpgradeCard card = card(1L, "카드", skill);
        when(upgradeCardRepository.findAllBySkill_IdIn(List.of(1L))).thenReturn(List.of(card));

        Map<Long, List<CardDataResponse>> result = cardService.getCardDataBySkillIds(List.of(1L));

        assertThat(result).containsOnlyKeys(1L);
        assertThat(result.get(1L)).hasSize(1);
    }

    @Test
    void throwsNotFoundWhenSkillMissingOnCreate() {
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());
        CardRequest request = request(1L, "카드");

        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 스킬입니다");
    }

    @Test
    void createsCardSuccessfully() {
        Skill skill = skill(1L, "화염검", 10);
        UpgradeCard saved = card(1L, "카드", skill);
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        when(upgradeCardRepository.save(any())).thenReturn(saved);
        CardRequest request = request(1L, "카드");

        CardDetailResponse response = cardService.createCard(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("카드");
    }

    @Test
    void throwsBadRequestWhenMinBattleLevelExceedsSkillMaxLevelPlusOne() {
        Skill skill = skill(1L, "화염검", 10);
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        CardRequest request = request(1L, "카드", 12);

        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("minBattleLevel은 1~11 사이여야 합니다");

        verify(upgradeCardRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenUpdatingMissingCard() {
        when(upgradeCardRepository.findById(1L)).thenReturn(Optional.empty());
        CardRequest request = request(1L, "카드");

        assertThatThrownBy(() -> cardService.updateCard(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 강화 카드입니다");
    }

    @Test
    void updatesCardSuccessfully() {
        Skill oldSkill = skill(1L, "화염검", 10);
        UpgradeCard existing = card(1L, "기존", oldSkill);
        when(upgradeCardRepository.findById(1L)).thenReturn(Optional.of(existing));
        Skill newSkill = skill(2L, "번개검", 10);
        when(skillRepository.findById(2L)).thenReturn(Optional.of(newSkill));
        CardRequest request = request(2L, "변경된 카드");

        cardService.updateCard(1L, request);

        verify(existing).update(newSkill, "변경된 카드", null, 1, true, null, 1, 0, null);
    }

    @Test
    void deletesCardSuccessfully() {
        Skill skill = skill(1L, "화염검", 10);
        UpgradeCard existing = card(1L, "카드", skill);
        when(upgradeCardRepository.findById(1L)).thenReturn(Optional.of(existing));

        cardService.deleteCard(1L);

        verify(upgradeCardRepository).delete(existing);
    }
}
