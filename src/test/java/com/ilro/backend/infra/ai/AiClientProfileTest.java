package com.ilro.backend.infra.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 운영(prod)에서 가짜 AI가 실제 고객에게 답하지 않게 - 설정 고정 + 가짜 클라이언트 자체를 만들지 않음
class AiClientProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
            .withUserConfiguration(FakeAiAnalyzeClient.class);

    @Test
    @DisplayName("prod 프로필에선 mode를 fake로 바꿔도 가짜 AI 클라이언트를 만들지 않음 (서버 기동 실패)")
    void noFakeClientInProd() {
        runner.withPropertyValues("app.ai.mode=fake", "spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(AiAnalyzeClient.class));
        runner.withPropertyValues("app.ai.mode=fake", "spring.profiles.active=dev")
                .run(context -> assertThat(context).hasSingleBean(FakeAiAnalyzeClient.class));
    }

    @Test
    @DisplayName("application-prod.yml의 AI 모드는 환경변수 없이 http로 고정")
    void prodModeIsPinned() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("prod", new ClassPathResource("application-prod.yml"));

        assertThat(sources.getFirst().getProperty("app.ai.mode")).hasToString("http");
    }
}
