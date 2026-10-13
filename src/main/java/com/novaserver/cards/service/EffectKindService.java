package com.novaserver.cards.service;

import com.novaserver.cards.dto.EffectKindRequest;
import com.novaserver.cards.dto.EffectKindResponse;
import com.novaserver.cards.entity.EffectKind;
import com.novaserver.cards.repository.EffectKindRepository;
import com.novaserver.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 효과 종류 관리 로직. */
@Service
@RequiredArgsConstructor
public class EffectKindService {
    private final EffectKindRepository effectKindRepository;

    @Transactional(readOnly = true)
    public List<EffectKindResponse> getKinds() {
        return effectKindRepository.findAllByOrderByIdAsc().stream()
                .map(EffectKindResponse::new)
                .toList();
    }

    /** 종류를 등록한다. */
    @Transactional
    public EffectKindResponse createKind(EffectKindRequest request) {
        if (effectKindRepository.existsByName(request.getName())) {
            throw ErrorCode.EFFECT_KIND_DUPLICATED.exception(request.getName());
        }
        try {
            EffectKind saved =
                    effectKindRepository.save(
                            new EffectKind(request.getName(), request.getLabel()));
            return new EffectKindResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.EFFECT_KIND_DUPLICATED.exception(request.getName());
        }
    }

    /** 종류를 수정한다. */
    @Transactional
    public EffectKindResponse updateKind(Long id, EffectKindRequest request) {
        EffectKind kind = findKind(id);
        if (effectKindRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ErrorCode.EFFECT_KIND_DUPLICATED.exception(request.getName());
        }
        kind.update(request.getName(), request.getLabel());
        try {
            effectKindRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.EFFECT_KIND_DUPLICATED.exception(request.getName());
        }
        return new EffectKindResponse(kind);
    }

    private EffectKind findKind(Long id) {
        return effectKindRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.EFFECT_KIND_NOT_FOUND.exception(id));
    }
}
