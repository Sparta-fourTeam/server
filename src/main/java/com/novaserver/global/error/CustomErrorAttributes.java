package com.novaserver.global.error;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

/** 기본 에러 응답을 팀 공통 형식(status, error, message, path)으로 맞춘다. */
@Slf4j
@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(
            WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attrs =
                super.getErrorAttributes(webRequest, options.including(Include.MESSAGE));
        attrs.remove("timestamp");

        Throwable error = getError(webRequest);
        int status = (int) attrs.get("status");

        if (error instanceof MethodArgumentNotValidException e) {
            FieldError fieldError = e.getBindingResult().getFieldError();
            if (fieldError != null) {
                attrs.put("message", fieldError.getDefaultMessage());
            }
        } else if (error instanceof HttpMessageNotReadableException) {
            attrs.put("message", ErrorCode.INVALID_REQUEST.getMessage());
        } else if (status >= 500) {
            log.error("서버 오류: {}", attrs.get("path"), error);
            if (!(error instanceof ResponseStatusException)) {
                attrs.put("message", ErrorCode.INTERNAL_ERROR.getMessage());
            }
        }
        return attrs;
    }
}
