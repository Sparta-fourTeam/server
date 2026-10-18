package com.novaserver.playerlevel.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.novaserver.playerlevel.dto.PlayerLevelConfigRequest;
import com.novaserver.playerlevel.repository.PlayerLevelConfigRepository;
import com.novaserver.stage.service.RewardRulesService;
import com.novaserver.tutorial.repository.TutorialRepository;
import com.novaserver.version.repository.DataVersionRepository;
import com.novaserver.version.service.DataVersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PlayerLevelServiceTest {
    @Mock private PlayerLevelConfigRepository repository;
    @Mock private DataVersionRepository versions;
    @Mock private DataVersionService versionService;
    @Mock private TutorialRepository tutorials;
    @Mock private RewardRulesService referenceLocks;
    @InjectMocks private PlayerLevelService service;

    @ParameterizedTest
    @CsvSource({
        ",5,4",
        "10,,4",
        "10,5,",
        "0,5,4",
        "10,-1,4",
        "10,5,1",
        "2147483647,1,3",
        "1,0,2147483647",
        "1500000000,0,3"
    })
    @DisplayName("누락·잘못된 범위·클라이언트 정수 오버플로를 저장 전에 거절한다")
    void shouldRejectInvalidConfigBeforeDatabaseAccess(Integer base, Integer step, Integer max) {
        assertThatThrownBy(() -> service.saveConfig(new PlayerLevelConfigRequest(base, step, max)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        verifyNoInteractions(repository, versions, versionService);
    }

    @Test
    @DisplayName("버전 기준 행이 없으면 설정을 저장하지 않는다")
    void shouldNotSaveWhenVersionRowIsMissing() {
        assertThatThrownBy(() -> service.saveConfig(new PlayerLevelConfigRequest(10, 0, 4)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("버전 행이 없습니다");
        verifyNoInteractions(repository, versionService);
    }
}
