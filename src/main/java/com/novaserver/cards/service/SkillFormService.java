package com.novaserver.cards.service;

import com.novaserver.cards.dto.SkillFormRequest;
import com.novaserver.cards.dto.SkillFormResponse;
import com.novaserver.cards.entity.SkillForm;
import com.novaserver.cards.repository.SkillFormRepository;
import com.novaserver.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 스킬 형태 관리 로직. */
@Service
@RequiredArgsConstructor
public class SkillFormService {
    private final SkillFormRepository skillFormRepository;

    @Transactional(readOnly = true)
    public List<SkillFormResponse> getForms() {
        return skillFormRepository.findAllByOrderByIdAsc().stream()
                .map(SkillFormResponse::new)
                .toList();
    }

    /** 형태를 등록한다. */
    @Transactional
    public SkillFormResponse createForm(SkillFormRequest request) {
        if (skillFormRepository.existsByName(request.getName())) {
            throw ErrorCode.SKILL_FORM_DUPLICATED.exception(request.getName());
        }
        try {
            SkillForm saved =
                    skillFormRepository.save(new SkillForm(request.getName(), request.getLabel()));
            return new SkillFormResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_FORM_DUPLICATED.exception(request.getName());
        }
    }

    /** 형태를 수정한다. */
    @Transactional
    public SkillFormResponse updateForm(Long id, SkillFormRequest request) {
        SkillForm form = findForm(id);
        if (skillFormRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ErrorCode.SKILL_FORM_DUPLICATED.exception(request.getName());
        }
        form.update(request.getName(), request.getLabel());
        try {
            skillFormRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_FORM_DUPLICATED.exception(request.getName());
        }
        return new SkillFormResponse(form);
    }

    private SkillForm findForm(Long id) {
        return skillFormRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.SKILL_FORM_NOT_FOUND.exception(id));
    }
}
