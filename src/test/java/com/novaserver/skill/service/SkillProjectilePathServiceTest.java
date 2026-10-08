package com.novaserver.skill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.skill.dto.SkillProjectilePathRequest;
import com.novaserver.skill.dto.SkillProjectilePathResponse;
import com.novaserver.skill.entity.SkillProjectilePath;
import com.novaserver.skill.repository.SkillProjectilePathRepository;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SkillProjectilePathServiceTest {

    @Mock private SkillProjectilePathRepository projectilePathRepository;
    @Mock private SkillRepository skillRepository;

    @InjectMocks private SkillProjectilePathService skillProjectilePathService;

    private SkillProjectilePath path(Long id, String name) {
        SkillProjectilePath path = mock(SkillProjectilePath.class);
        lenient().when(path.getId()).thenReturn(id);
        lenient().when(path.getName()).thenReturn(name);
        return path;
    }

    private SkillProjectilePathRequest request(String name, String label) {
        SkillProjectilePathRequest request = new SkillProjectilePathRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "label", label);
        return request;
    }

    @Test
    void returnsListOrderedById() {
        SkillProjectilePath straight = path(1L, "Straight");
        when(projectilePathRepository.findAllByOrderByIdAsc()).thenReturn(List.of(straight));

        List<SkillProjectilePathResponse> responses =
                skillProjectilePathService.getSkillProjectilePaths();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Straight");
    }

    @Test
    void throwsConflictWhenNameDuplicated() {
        when(projectilePathRepository.existsByName("Straight")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                skillProjectilePathService.createSkillProjectilePath(
                                        request("Straight", "직선")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 투사체 경로 이름입니다");

        verify(projectilePathRepository, never()).save(any());
    }

    @Test
    void convertsUniqueConstraintToConflictOnConcurrentCreate() {
        when(projectilePathRepository.existsByName("Straight")).thenReturn(false);
        when(projectilePathRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(
                        () ->
                                skillProjectilePathService.createSkillProjectilePath(
                                        request("Straight", "직선")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 투사체 경로 이름입니다");
    }

    @Test
    void createsPathSuccessfully() {
        SkillProjectilePath saved = path(1L, "Straight");
        when(projectilePathRepository.existsByName("Straight")).thenReturn(false);
        when(projectilePathRepository.save(any())).thenReturn(saved);

        SkillProjectilePathResponse response =
                skillProjectilePathService.createSkillProjectilePath(request("Straight", "직선"));

        assertThat(response.getName()).isEqualTo("Straight");
    }

    @Test
    void throwsNotFoundWhenUpdatingMissingId() {
        when(projectilePathRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                skillProjectilePathService.updateSkillProjectilePath(
                                        1L, request("Straight", "직선")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 투사체 경로입니다");
    }

    @Test
    void throwsConflictWhenNameDuplicatesAnotherPath() {
        SkillProjectilePath existing = path(1L, "Straight");
        when(projectilePathRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectilePathRepository.existsByNameAndIdNot("Curve", 1L)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                skillProjectilePathService.updateSkillProjectilePath(
                                        1L, request("Curve", "곡선")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 투사체 경로 이름입니다");
    }

    @Test
    void throwsConflictWhenDeletingPathInUse() {
        SkillProjectilePath existing = path(1L, "Straight");
        when(projectilePathRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.existsByProjectilePathId(1L)).thenReturn(true);

        assertThatThrownBy(() -> skillProjectilePathService.deleteSkillProjectilePath(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("사용 중인 스킬이 있어 삭제할 수 없습니다");

        verify(projectilePathRepository, never()).delete(any());
    }

    @Test
    void deletesUnusedPath() {
        SkillProjectilePath existing = path(1L, "Straight");
        when(projectilePathRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.existsByProjectilePathId(1L)).thenReturn(false);

        skillProjectilePathService.deleteSkillProjectilePath(1L);

        verify(projectilePathRepository).delete(existing);
    }
}
