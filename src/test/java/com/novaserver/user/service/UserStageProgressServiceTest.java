package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.entity.StageRatingReward;
import com.novaserver.stage.entity.StageRewardItem;
import com.novaserver.stage.repository.StageRatingRewardRepository;
import com.novaserver.user.dto.UserStageProgressResponse;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.repository.UserStageProgressRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserStageProgressServiceTest {

    @Mock private UserStageProgressRepository userStageProgressRepository;
    @Mock private StageRatingRewardRepository stageRatingRewardRepository;
    @Mock private UserRewardService userRewardService;

    @InjectMocks private UserStageProgressService userStageProgressService;

    @Test
    void throwsNotFoundWhenNoProgress() {
        when(userStageProgressRepository.findByUserIdAndStageIdForUpdate(1L, 10))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userStageProgressService.claimRatingReward(1L, 10))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("스테이지 진행도가 없습니다");
    }

    @Test
    void throwsConflictWhenNothingLeftToClaim() {
        Stage stage = mock(Stage.class);
        UserStageProgress progress = new UserStageProgress(null, stage);
        progress.updateClearRating(2);
        progress.updateClaimedRating(2);
        when(userStageProgressRepository.findByUserIdAndStageIdForUpdate(1L, 10))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> userStageProgressService.claimRatingReward(1L, 10))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("받을 수 있는 보상이 없습니다");
        verifyNoInteractions(userRewardService);
    }

    @Test
    void claimsOnlyUnclaimedTiersAndAdvancesClaimedRating() {
        Stage stage = mock(Stage.class);
        when(stage.getStageId()).thenReturn(10);
        UserStageProgress progress = new UserStageProgress(null, stage);
        progress.updateClearRating(2);
        when(userStageProgressRepository.findByUserIdAndStageIdForUpdate(1L, 10))
                .thenReturn(Optional.of(progress));

        StageRatingReward rating1 =
                new StageRatingReward(
                        stage, 1, 10, 100, List.of(new StageRewardItem("mat.wood", 2)));
        StageRatingReward rating2 =
                new StageRatingReward(
                        stage, 2, 20, 200, List.of(new StageRewardItem("mat.wood", 3)));
        when(stageRatingRewardRepository.findByStageIdAndRatingBetween(10, 1, 2))
                .thenReturn(List.of(rating1, rating2));

        UserStageProgressResponse response = userStageProgressService.claimRatingReward(1L, 10);

        verify(userRewardService).grant(eq(1L), eq(30), eq(300), eq(Map.of("mat.wood", 5)));
        assertThat(response.getClaimedRating()).isEqualTo(2);
        assertThat(progress.getClaimedRating()).isEqualTo(2);
    }

    @Test
    void secondClaimAfterFurtherClearOnlyPaysTheNewTier() {
        Stage stage = mock(Stage.class);
        when(stage.getStageId()).thenReturn(10);
        UserStageProgress progress = new UserStageProgress(null, stage);
        progress.updateClearRating(2);
        progress.updateClaimedRating(2);
        progress.updateClearRating(3);
        when(userStageProgressRepository.findByUserIdAndStageIdForUpdate(1L, 10))
                .thenReturn(Optional.of(progress));

        StageRatingReward rating3 = new StageRatingReward(stage, 3, 30, 300, List.of());
        when(stageRatingRewardRepository.findByStageIdAndRatingBetween(10, 3, 3))
                .thenReturn(List.of(rating3));

        userStageProgressService.claimRatingReward(1L, 10);

        verify(userRewardService).grant(eq(1L), eq(30), eq(300), eq(Map.of()));
        assertThat(progress.getClaimedRating()).isEqualTo(3);
    }
}
