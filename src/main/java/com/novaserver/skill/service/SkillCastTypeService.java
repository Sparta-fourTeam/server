package com.novaserver.skill.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.dto.SkillCastTypeRequest;
import com.novaserver.skill.dto.SkillCastTypeResponse;
import com.novaserver.skill.entity.SkillCastType;
import com.novaserver.skill.repository.SkillCastTypeRepository;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public SkillCastTypeResponse createCastType(SkillCastTypeRequest request) {
        if (castTypeRepository.existsByName(request.getName())) {
            throw ErrorCode.SKILL_CAST_TYPE_DUPLICATED.exception(request.getName());
        }
        SkillCastType saved =
                castTypeRepository.save(new SkillCastType(request.getName(), request.getLabel()));

        return new SkillCastTypeResponse(saved);
    }

    @Transactional
    public SkillCastTypeResponse updateCastType(Long id, SkillCastTypeRequest request) {
        SkillCastType castType = findCastType(id);
        if (castTypeRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ErrorCode.SKILL_CAST_TYPE_DUPLICATED.exception(request.getName());
        }
        castType.update(request.getName(), request.getLabel());
        castTypeRepository.flush();
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

    public SkillCastType findCastType(Long id) {
        return castTypeRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.SKILL_CAST_TYPE_NOT_FOUND.exception(id));
    }
}
