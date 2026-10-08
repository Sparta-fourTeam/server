package com.novaserver.skill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.skill.dto.SkillCastTypeRequest;
import com.novaserver.skill.dto.SkillCastTypeResponse;
import com.novaserver.skill.entity.SkillCastType;
import com.novaserver.skill.repository.SkillCastTypeRepository;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SkillCastTypeServiceTest {

    @Mock private SkillCastTypeRepository castTypeRepository;
    @Mock private SkillRepository skillRepository;

    @InjectMocks private SkillCastTypeService skillCastTypeService;

    private SkillCastType castType(Long id, String name) {
        SkillCastType castType = mock(SkillCastType.class);
        lenient().when(castType.getId()).thenReturn(id);
        lenient().when(castType.getName()).thenReturn(name);
        return castType;
    }

    @Test
    void returnsListOrderedById() {
        SkillCastType melee = castType(1L, "Melee");
        when(castTypeRepository.findAllByOrderByIdAsc()).thenReturn(List.of(melee));

        List<SkillCastTypeResponse> responses = skillCastTypeService.getCastTypes();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Melee");
    }

    @Test
    void throwsConflictWhenNameDuplicated() {
        SkillCastTypeRequest request = new SkillCastTypeRequest();
        setFields(request, "Melee", "근접");
        when(castTypeRepository.existsByName("Melee")).thenReturn(true);

        assertThatThrownBy(() -> skillCastTypeService.createCastType(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 시전 방식 이름입니다");

        verify(castTypeRepository, never()).save(any());
    }

    @Test
    void convertsUniqueConstraintToConflictOnConcurrentCreate() {
        SkillCastTypeRequest request = new SkillCastTypeRequest();
        setFields(request, "Melee", "근접");
        when(castTypeRepository.existsByName("Melee")).thenReturn(false);
        when(castTypeRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> skillCastTypeService.createCastType(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 시전 방식 이름입니다");
    }

    @Test
    void createsCastTypeSuccessfully() {
        SkillCastTypeRequest request = new SkillCastTypeRequest();
        setFields(request, "Melee", "근접");
        SkillCastType saved = castType(1L, "Melee");
        when(castTypeRepository.existsByName("Melee")).thenReturn(false);
        when(castTypeRepository.save(any())).thenReturn(saved);

        SkillCastTypeResponse response = skillCastTypeService.createCastType(request);

        assertThat(response.getName()).isEqualTo("Melee");
    }

    @Test
    void throwsNotFoundWhenUpdatingMissingId() {
        SkillCastTypeRequest request = new SkillCastTypeRequest();
        setFields(request, "Melee", "근접");
        when(castTypeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillCastTypeService.updateCastType(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 시전 방식입니다");
    }

    @Test
    void throwsConflictWhenNameDuplicatesAnotherCastType() {
        SkillCastTypeRequest request = new SkillCastTypeRequest();
        setFields(request, "Ranged", "원거리");
        SkillCastType existing = castType(1L, "Melee");
        when(castTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(castTypeRepository.existsByNameAndIdNot("Ranged", 1L)).thenReturn(true);

        assertThatThrownBy(() -> skillCastTypeService.updateCastType(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 시전 방식 이름입니다");
    }

    @Test
    void throwsConflictWhenDeletingCastTypeInUse() {
        SkillCastType existing = castType(1L, "Melee");
        when(castTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.existsByCastTypeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> skillCastTypeService.deleteCastType(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("사용 중인 스킬이 있어 삭제할 수 없습니다");

        verify(castTypeRepository, never()).delete(any());
    }

    @Test
    void deletesUnusedCastType() {
        SkillCastType existing = castType(1L, "Melee");
        when(castTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.existsByCastTypeId(1L)).thenReturn(false);

        skillCastTypeService.deleteCastType(1L);

        verify(castTypeRepository).delete(existing);
    }

    private void setFields(SkillCastTypeRequest request, String name, String label) {
        org.springframework.test.util.ReflectionTestUtils.setField(request, "name", name);
        org.springframework.test.util.ReflectionTestUtils.setField(request, "label", label);
    }
}
