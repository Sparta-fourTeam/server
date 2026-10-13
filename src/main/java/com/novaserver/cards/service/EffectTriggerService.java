package com.novaserver.cards.service;

import com.novaserver.cards.dto.EffectTriggerRequest;
import com.novaserver.cards.dto.EffectTriggerResponse;
import com.novaserver.cards.entity.EffectTrigger;
import com.novaserver.cards.repository.EffectTriggerRepository;
import com.novaserver.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 효과 발동 시점 관리 로직. */
@Service
@RequiredArgsConstructor
public class EffectTriggerService {
    private final EffectTriggerRepository effectTriggerRepository;

    @Transactional(readOnly = true)
    public List<EffectTriggerResponse> getTriggers() {
        return effectTriggerRepository.findAllByOrderByIdAsc().stream()
                .map(EffectTriggerResponse::new)
                .toList();
    }

    /** 발동 시점을 등록한다. */
    @Transactional
    public EffectTriggerResponse createTrigger(EffectTriggerRequest request) {
        if (effectTriggerRepository.existsByName(request.getName())) {
            throw ErrorCode.EFFECT_TRIGGER_DUPLICATED.exception(request.getName());
        }
        try {
            EffectTrigger saved =
                    effectTriggerRepository.save(
                            new EffectTrigger(request.getName(), request.getLabel()));
            return new EffectTriggerResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.EFFECT_TRIGGER_DUPLICATED.exception(request.getName());
        }
    }

    /** 발동 시점을 수정한다. */
    @Transactional
    public EffectTriggerResponse updateTrigger(Long id, EffectTriggerRequest request) {
        EffectTrigger trigger = findTrigger(id);
        if (effectTriggerRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ErrorCode.EFFECT_TRIGGER_DUPLICATED.exception(request.getName());
        }
        trigger.update(request.getName(), request.getLabel());
        try {
            effectTriggerRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.EFFECT_TRIGGER_DUPLICATED.exception(request.getName());
        }
        return new EffectTriggerResponse(trigger);
    }

    private EffectTrigger findTrigger(Long id) {
        return effectTriggerRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.EFFECT_TRIGGER_NOT_FOUND.exception(id));
    }
}
