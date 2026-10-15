package com.novaserver.skill.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.dto.SkillElementRequest;
import com.novaserver.skill.dto.SkillElementResponse;
import com.novaserver.skill.entity.SkillElement;
import com.novaserver.skill.repository.SkillElementRepository;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 속성 관리 로직. */
@Service
@RequiredArgsConstructor
public class SkillElementService {
    private final SkillElementRepository skillElementRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<SkillElementResponse> getElements() {
        return skillElementRepository.findAllByOrderByIdAsc().stream()
                .map(SkillElementResponse::new)
                .toList();
    }

    /** 속성을 등록한다. 이름이 이미 있으면 409를 던진다. */
    @Transactional
    public SkillElementResponse createElement(SkillElementRequest request) {
        if (skillElementRepository.existsByName(request.getName())) {
            throw ErrorCode.SKILL_ELEMENT_DUPLICATED.exception(request.getName());
        }
        try {
            SkillElement saved =
                    skillElementRepository.save(
                            new SkillElement(request.getName(), request.getLabel()));
            return new SkillElementResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_ELEMENT_DUPLICATED.exception(request.getName());
        }
    }

    /** 속성을 수정한다. 없는 id면 404, 이름이 겹치면 409를 던진다. */
    @Transactional
    public SkillElementResponse updateElement(Long id, SkillElementRequest request) {
        SkillElement element = findElement(id);
        if (skillElementRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ErrorCode.SKILL_ELEMENT_DUPLICATED.exception(request.getName());
        }
        element.update(request.getName(), request.getLabel());
        try {
            skillElementRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_ELEMENT_DUPLICATED.exception(request.getName());
        }
        return new SkillElementResponse(element);
    }

    @Transactional
    public void deleteElement(Long id) {
        SkillElement element = findElement(id);
        if (skillRepository.existsByElementId(id)) {
            throw ErrorCode.SKILL_ELEMENT_IN_USE.exception(element.getName());
        }
        skillElementRepository.delete(element);
    }

    /** 속성을 id로 찾는다. 없으면 404를 던진다. */
    public SkillElement findElement(Long id) {
        return skillElementRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.SKILL_ELEMENT_NOT_FOUND.exception(id));
    }
}
