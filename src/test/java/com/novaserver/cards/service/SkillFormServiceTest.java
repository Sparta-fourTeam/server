package com.novaserver.cards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.cards.dto.SkillFormRequest;
import com.novaserver.cards.dto.SkillFormResponse;
import com.novaserver.cards.entity.SkillForm;
import com.novaserver.cards.repository.SkillFormRepository;
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
class SkillFormServiceTest {

    @Mock private SkillFormRepository skillFormRepository;

    @InjectMocks private SkillFormService skillFormService;

    private SkillForm form(Long id, String name, String label) {
        SkillForm form = mock(SkillForm.class);
        lenient().when(form.getId()).thenReturn(id);
        lenient().when(form.getName()).thenReturn(name);
        lenient().when(form.getLabel()).thenReturn(label);
        return form;
    }

    private SkillFormRequest request(String name, String label) {
        SkillFormRequest request = new SkillFormRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "label", label);
        return request;
    }

    @Test
    void returnsListOrderedById() {
        SkillForm beast = form(1L, "beast", "짐승형");
        when(skillFormRepository.findAllByOrderByIdAsc()).thenReturn(List.of(beast));

        List<SkillFormResponse> responses = skillFormService.getForms();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("beast");
    }

    @Test
    void throwsConflictWhenNameDuplicated() {
        when(skillFormRepository.existsByName("beast")).thenReturn(true);
        SkillFormRequest request = request("beast", "짐승형");

        assertThatThrownBy(() -> skillFormService.createForm(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 스킬 형태 이름입니다");

        verify(skillFormRepository, never()).save(any());
    }

    @Test
    void createsFormSuccessfully() {
        SkillForm saved = form(1L, "beast", "짐승형");
        when(skillFormRepository.existsByName("beast")).thenReturn(false);
        when(skillFormRepository.save(any())).thenReturn(saved);
        SkillFormRequest request = request("beast", "짐승형");

        SkillFormResponse response = skillFormService.createForm(request);

        assertThat(response.getName()).isEqualTo("beast");
    }

    @Test
    void throwsNotFoundWhenUpdatingMissingId() {
        when(skillFormRepository.findById(1L)).thenReturn(Optional.empty());
        SkillFormRequest request = request("beast", "짐승형");

        assertThatThrownBy(() -> skillFormService.updateForm(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 스킬 형태입니다");
    }

    @Test
    void throwsConflictWhenNameDuplicatesAnotherForm() {
        SkillForm existing = form(1L, "beast", "짐승형");
        when(skillFormRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillFormRepository.existsByNameAndIdNot("plant", 1L)).thenReturn(true);
        SkillFormRequest request = request("plant", "식물형");

        assertThatThrownBy(() -> skillFormService.updateForm(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 있는 스킬 형태 이름입니다");
    }

    @Test
    void updatesFormSuccessfully() {
        SkillForm existing = form(1L, "beast", "짐승형");
        when(skillFormRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillFormRepository.existsByNameAndIdNot("plant", 1L)).thenReturn(false);
        SkillFormRequest request = request("plant", "식물형");

        skillFormService.updateForm(1L, request);

        verify(existing).update("plant", "식물형");
    }
}
