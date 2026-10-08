package com.novaserver.skill.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.dto.SkillProjectilePathRequest;
import com.novaserver.skill.dto.SkillProjectilePathResponse;
import com.novaserver.skill.entity.SkillProjectilePath;
import com.novaserver.skill.repository.SkillProjectilePathRepository;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 투사체 경로 관리 로직. */
@Service
@RequiredArgsConstructor
public class SkillProjectilePathService {
    private final SkillProjectilePathRepository projectilePathRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<SkillProjectilePathResponse> getSkillProjectilePaths() {
        return projectilePathRepository.findAllByOrderByIdAsc().stream()
                .map(SkillProjectilePathResponse::new)
                .toList();
    }

    @Transactional
    public SkillProjectilePathResponse createSkillProjectilePath(
            SkillProjectilePathRequest request) {
        if (projectilePathRepository.existsByName(request.getName())) {
            throw ErrorCode.SKILL_PROJECTILE_PATH_DUPLICATED.exception(request.getName());
        }
        try {
            SkillProjectilePath saved =
                    projectilePathRepository.save(
                            new SkillProjectilePath(request.getName(), request.getLabel()));
            return new SkillProjectilePathResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_PROJECTILE_PATH_DUPLICATED.exception(request.getName());
        }
    }

    @Transactional
    public SkillProjectilePathResponse updateSkillProjectilePath(
            Long id, SkillProjectilePathRequest request) {
        SkillProjectilePath projectilePath = findProjectilePath(id);
        if (projectilePathRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ErrorCode.SKILL_PROJECTILE_PATH_DUPLICATED.exception(request.getName());
        }
        projectilePath.update(request.getName(), request.getLabel());
        try {
            projectilePathRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SKILL_PROJECTILE_PATH_DUPLICATED.exception(request.getName());
        }
        return new SkillProjectilePathResponse(projectilePath);
    }

    @Transactional
    public void deleteSkillProjectilePath(Long id) {
        SkillProjectilePath projectilePath = findProjectilePath(id);
        if (skillRepository.existsByProjectilePathId(id)) {
            throw ErrorCode.SKILL_PROJECTILE_PATH_IN_USE.exception(projectilePath.getName());
        }
        projectilePathRepository.delete(projectilePath);
    }

    public SkillProjectilePath findProjectilePath(Long id) {
        return projectilePathRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.SKILL_PROJECTILE_PATH_NOT_FOUND.exception(id));
    }
}
