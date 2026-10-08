package com.novaserver.skill.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.dto.SkillDataResponse;
import com.novaserver.skill.dto.SkillDetailResponse;
import com.novaserver.skill.dto.SkillRequest;
import com.novaserver.skill.dto.SkillSummaryResponse;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.entity.SkillCastType;
import com.novaserver.skill.entity.SkillProjectilePath;
import com.novaserver.skill.repository.SkillRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 스킬 관련 API. */
@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepository skillRepository;

    private final SkillCastTypeService skillCastTypeService;
    private final SkillProjectilePathService skillProjectilePathService;

    /** 스킬 목록을 요약해서 조회한다. */
    @Transactional(readOnly = true)
    public List<SkillSummaryResponse> getSkillList() {
        List<Skill> skills = skillRepository.findAll();
        List<SkillSummaryResponse> responses = new ArrayList<>();

        for (Skill skill : skills) {
            responses.add(SkillSummaryResponse.from(skill));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public SkillDetailResponse getSkillDetail(Long id) {
        return SkillDetailResponse.from(findSkill(id));
    }

    /** 유니티 클라이언트용 스킬 데이터를 전체 조회한다. */
    @Transactional(readOnly = true)
    public List<SkillDataResponse> getSkillData() {
        List<Skill> skills = skillRepository.findAll();
        List<SkillDataResponse> responses = new ArrayList<>();

        for (Skill skill : skills) {
            responses.add(SkillDataResponse.from(skill));
        }

        return responses;
    }

    /** 스킬을 등록한다. 이름이 이미 있으면 409를 던진다. */
    @Transactional
    public SkillDetailResponse createSkill(SkillRequest request) {
        if (skillRepository.existsByName(request.getName())) {
            throw ErrorCode.SKILL_DUPLICATED.exception(request.getName());
        }
        SkillCastType castType = skillCastTypeService.findCastType(request.getCastTypeId());
        SkillProjectilePath path =
                skillProjectilePathService.findProjectilePath(request.getProjectilePathId());
        try {
            Skill saved =
                    skillRepository.save(
                            new Skill(
                                    request.getName(),
                                    request.getDesc(),
                                    castType,
                                    path,
                                    request.getMaxLevel(),
                                    request.isChildOnly(),
                                    request.getBaseStats() == null
                                            ? null
                                            : request.getBaseStats().toEntity(),
                                    request.getUnlockLevel()));
            return SkillDetailResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_DUPLICATED.exception(request.getName());
        }
    }

    /** 스킬을 수정한다. 없는 id면 404, 이름이 겹치면 409를 던진다. */
    @Transactional
    public SkillDetailResponse updateSkill(Long id, SkillRequest request) {
        Skill skill = findSkill(id);
        if (skillRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ErrorCode.SKILL_DUPLICATED.exception(request.getName());
        }
        SkillCastType castType = skillCastTypeService.findCastType(request.getCastTypeId());
        SkillProjectilePath path =
                skillProjectilePathService.findProjectilePath(request.getProjectilePathId());
        skill.update(
                request.getName(),
                request.getDesc(),
                castType,
                path,
                request.getMaxLevel(),
                request.isChildOnly(),
                request.getBaseStats() == null ? null : request.getBaseStats().toEntity(),
                request.getUnlockLevel());
        try {
            skillRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_DUPLICATED.exception(request.getName());
        }
        return SkillDetailResponse.from(skill);
    }

    @Transactional
    public void deleteSkill(Long id) {
        skillRepository.delete(findSkill(id));
    }

    /** 스킬을 id로 찾는다. 없으면 404를 던진다. */
    public Skill findSkill(Long id) {
        return skillRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.SKILL_NOT_FOUND.exception(id));
    }
}
