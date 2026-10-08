package com.novaserver.skill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.skill.dto.SkillDetailResponse;
import com.novaserver.skill.dto.SkillRequest;
import com.novaserver.skill.dto.SkillSummaryResponse;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.entity.SkillCastType;
import com.novaserver.skill.entity.SkillProjectilePath;
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
class SkillServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private SkillCastTypeService skillCastTypeService;
    @Mock private SkillProjectilePathService skillProjectilePathService;

    @InjectMocks private SkillService skillService;

    private SkillCastType castType() {
        SkillCastType castType = mock(SkillCastType.class);
        lenient().when(castType.getId()).thenReturn(10L);
        lenient().when(castType.getName()).thenReturn("Melee");
        return castType;
    }

    private SkillProjectilePath path() {
        SkillProjectilePath path = mock(SkillProjectilePath.class);
        lenient().when(path.getId()).thenReturn(20L);
        lenient().when(path.getName()).thenReturn("Straight");
        return path;
    }

    private Skill skill(Long id, String name, SkillCastType castType, SkillProjectilePath path) {
        Skill skill = mock(Skill.class);
        lenient().when(skill.getId()).thenReturn(id);
        lenient().when(skill.getName()).thenReturn(name);
        lenient().when(skill.getCastType()).thenReturn(castType);
        lenient().when(skill.getProjectilePath()).thenReturn(path);
        lenient().when(skill.getMaxLevel()).thenReturn(5);
        lenient().when(skill.isChildOnly()).thenReturn(false);
        lenient().when(skill.getUnlockLevel()).thenReturn(1);
        return skill;
    }

    private SkillRequest request(String name, Long castTypeId, Long projectilePathId) {
        SkillRequest request = new SkillRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "castTypeId", castTypeId);
        ReflectionTestUtils.setField(request, "projectilePathId", projectilePathId);
        ReflectionTestUtils.setField(request, "maxLevel", 5);
        return request;
    }

    @Test
    void returnsSkillSummaryList() {
        Skill skill = skill(1L, "파이어볼", castType(), path());
        when(skillRepository.findAll()).thenReturn(List.of(skill));

        List<SkillSummaryResponse> responses = skillService.getSkillList();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("파이어볼");
    }

    @Test
    void throwsNotFoundWhenSkillMissing() {
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.getSkillDetail(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 스킬입니다");
    }

    @Test
    void returnsSkillDetail() {
        Skill skill = skill(1L, "파이어볼", castType(), path());
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));

        SkillDetailResponse response = skillService.getSkillDetail(1L);

        assertThat(response.name()).isEqualTo("파이어볼");
        assertThat(response.castType().name()).isEqualTo("Melee");
    }

    @Test
    void throwsConflictWhenNameDuplicated() {
        SkillRequest request = request("파이어볼", 10L, 20L);
        when(skillRepository.existsByName("파이어볼")).thenReturn(true);

        assertThatThrownBy(() -> skillService.createSkill(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 존재하는 스킬명입니다");

        verify(skillRepository, never()).save(any());
    }

    @Test
    void convertsUniqueConstraintToConflictOnConcurrentCreate() {
        SkillRequest request = request("파이어볼", 10L, 20L);
        SkillCastType castType = castType();
        SkillProjectilePath path = path();
        when(skillRepository.existsByName("파이어볼")).thenReturn(false);
        when(skillCastTypeService.findCastType(10L)).thenReturn(castType);
        when(skillProjectilePathService.findProjectilePath(20L)).thenReturn(path);
        when(skillRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> skillService.createSkill(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 존재하는 스킬명입니다");
    }

    @Test
    void createsSkillSuccessfully() {
        SkillRequest request = request("파이어볼", 10L, 20L);
        SkillCastType castType = castType();
        SkillProjectilePath path = path();
        Skill saved = skill(1L, "파이어볼", castType, path);
        when(skillRepository.existsByName("파이어볼")).thenReturn(false);
        when(skillCastTypeService.findCastType(10L)).thenReturn(castType);
        when(skillProjectilePathService.findProjectilePath(20L)).thenReturn(path);
        when(skillRepository.save(any())).thenReturn(saved);

        SkillDetailResponse response = skillService.createSkill(request);

        assertThat(response.name()).isEqualTo("파이어볼");
    }

    @Test
    void throwsNotFoundWhenUpdatingMissingSkill() {
        SkillRequest request = request("파이어볼", 10L, 20L);
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.updateSkill(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 스킬입니다");
    }

    @Test
    void throwsConflictWhenNameDuplicatesAnotherSkill() {
        SkillRequest request = request("아이스볼", 10L, 20L);
        Skill existing = skill(1L, "파이어볼", castType(), path());
        when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.existsByNameAndIdNot("아이스볼", 1L)).thenReturn(true);

        assertThatThrownBy(() -> skillService.updateSkill(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 존재하는 스킬명입니다");
    }

    @Test
    void updatesSkillSuccessfully() {
        SkillRequest request = request("파이어볼2", 10L, 20L);
        SkillCastType castType = castType();
        SkillProjectilePath path = path();
        Skill existing = skill(1L, "파이어볼", castType, path);
        when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.existsByNameAndIdNot("파이어볼2", 1L)).thenReturn(false);
        when(skillCastTypeService.findCastType(10L)).thenReturn(castType);
        when(skillProjectilePathService.findProjectilePath(20L)).thenReturn(path);

        SkillDetailResponse response = skillService.updateSkill(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        verify(skillRepository).flush();
    }

    @Test
    void deletesSkill() {
        Skill existing = skill(1L, "파이어볼", castType(), path());
        when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));

        skillService.deleteSkill(1L);

        verify(skillRepository).delete(existing);
    }
}
