package com.novaserver.skill.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.dto.SkillCastTypeRequest;
import com.novaserver.skill.dto.SkillCastTypeResponse;
import com.novaserver.skill.entity.SkillCastType;
import com.novaserver.skill.repository.SkillCastTypeRepository;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 스킬 시전 방식 관리 로직. */
@Service
@RequiredArgsConstructor
public class SkillCastTypeService {
    private final SkillCastTypeRepository castTypeRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<SkillCastTypeResponse> getCastTypes() {
        return castTypeRepository.findAllByOrderByIdAsc().stream()
                .map(SkillCastTypeResponse::new)
                .toList();
    }

    /** 시전 방식을 등록한다. 이름이 이미 있으면 409를 던진다. */
    @Transactional
    public SkillCastTypeResponse createCastType(SkillCastTypeRequest request) {
        if (castTypeRepository.existsByName(request.getName())) {
            throw ErrorCode.SKILL_CAST_TYPE_DUPLICATED.exception(request.getName());
        }
        try {
            SkillCastType saved =
                    castTypeRepository.save(
                            new SkillCastType(request.getName(), request.getLabel()));
            return new SkillCastTypeResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_CAST_TYPE_DUPLICATED.exception(request.getName());
        }
    }

    /** 시전 방식을 수정한다. 없는 id면 404, 이름이 겹치면 409를 던진다. */
    @Transactional
    public SkillCastTypeResponse updateCastType(Long id, SkillCastTypeRequest request) {
        SkillCastType castType = findCastType(id);
        if (castTypeRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ErrorCode.SKILL_CAST_TYPE_DUPLICATED.exception(request.getName());
        }
        castType.update(request.getName(), request.getLabel());
        try {
            castTypeRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_CAST_TYPE_DUPLICATED.exception(request.getName());
        }
        return new SkillCastTypeResponse(castType);
    }

    @Transactional
    public void deleteCastType(Long id) {
        SkillCastType castType = findCastType(id);
        if (skillRepository.existsByCastTypeId(id)) {
            throw ErrorCode.SKILL_CAST_TYPE_IN_USE.exception(castType.getName());
        }
        castTypeRepository.delete(castType);
    }

    /** 시전 방식을 id로 찾는다. 없으면 404를 던진다. */
    public SkillCastType findCastType(Long id) {
        return castTypeRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.SKILL_CAST_TYPE_NOT_FOUND.exception(id));
    }
}
