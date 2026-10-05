package com.novaserver.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 공통 설정.
 *
 * <p>모든 {@code @RestController}의 경로 앞에 {@code /api}를 자동으로 붙인다. 컨트롤러에는 {@code /api}를 직접 쓰지 않는다.
 * Actuator 같은 다른 엔드포인트에는 적용되지 않는다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
