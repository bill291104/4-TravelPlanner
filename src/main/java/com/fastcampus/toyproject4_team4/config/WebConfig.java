package com.fastcampus.toyproject4_team4.config;

import com.fastcampus.toyproject4_team4.Domains;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 아래에 정의한 StringToDomainsConverter를 포메터 레지스트리에 추가합니다.
        registry.addConverter(new StringToDomainsConverter());
    }


    /**
     * URL 경로의 문자열을 Domains Enum으로 변환하는 역할을 하는 Converter 클래스입니다.
     * WebConfig 내부에 static 중첩 클래스로 정의하여 관련 코드를 한 곳에서 관리합니다.
     */
    private static class StringToDomainsConverter implements Converter<String, Domains> {

        @Override
        public Domains convert(@NonNull String source) {

            //모든 Enum 상수를 순회하면서
            for (Domains domain : Domains.values()) {
                if (domain.getName().equalsIgnoreCase(source)) {
                    // 일치하는 경우 해당 Enum 상수를 반환합니다.
                    return domain;
                }
            }
            // for문을 모두 통과했는데도 일치하는 Enum이 없으면 예외 발생
            throw new IllegalArgumentException("'" + source + "'에 해당하는 도메인을 찾을 수 없습니다.");

        }
    }
}
