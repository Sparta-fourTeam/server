package com.novaserver.clientpolicy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/** Actual Boot environment and @Value injection; no database, Redis or production config. */
class ClientPolicyEnvironmentBindingTest {
    @TempDir Path directory;

    @Test
    void composeEnvironmentNameBindsAndEnforcesMaintenance() throws Exception {
        Path policy = maintenanceFile();
        try (var context = start(Map.of("APP_CLIENT_POLICY_FILE", policy.toString()))) {
            assertBoundMaintenance(context, policy);
        }
    }

    @Test
    void canonicalEnvironmentNameAlsoBindsAndEnforcesMaintenance() throws Exception {
        Path policy = maintenanceFile();
        try (var context = start(Map.of("APP_CLIENTPOLICY_FILE", policy.toString()))) {
            assertBoundMaintenance(context, policy);
        }
    }

    @Test
    void absentEnvironmentVariablePreservesDisabledLegacyDefault() {
        try (var context = start(Map.of())) {
            var policy = context.getBean(ClientPolicySource.class).current();
            assertThat(policy.enforcementEnabled()).isFalse();
            assertThat(policy.allowLegacy()).isTrue();
            assertThat(policy.revision()).isEqualTo("initial-off");
        }
    }

    @Test
    void explicitMissingFileFailsClosedInsteadOfSilentlyDisabling() {
        try (var context =
                start(
                        Map.of(
                                "APP_CLIENT_POLICY_FILE",
                                directory.resolve("missing.json").toString()))) {
            var policy = context.getBean(ClientPolicySource.class).current();
            assertThat(policy.isMaintenance()).isTrue();
            assertThat(policy.revision()).isEqualTo("policy-unavailable");
        }
    }

    private Path maintenanceFile() throws Exception {
        return Files.writeString(
                directory.resolve("policy.json"),
                """
                {"schemaVersion":1,"revision":"binding-probe","enforcementEnabled":true,
                 "mode":"MAINTENANCE","message":"synthetic test","estimatedEnd":null,
                 "retryAfterSeconds":30,"allowLegacy":true,"platforms":{}}
                """);
    }

    private void assertBoundMaintenance(ConfigurableApplicationContext context, Path file) {
        // Do not replace Boot's ApplicationEnvironment with a StandardEnvironment test double.
        assertThat(context.getEnvironment().getClass().getName())
                .isEqualTo("org.springframework.boot.ApplicationEnvironment");
        assertThat(context.getEnvironment().getProperty("app.client-policy.file"))
                .isEqualTo(file.toString());
        var policy = context.getBean(ClientPolicySource.class).current();
        assertThat(policy.revision()).isEqualTo("binding-probe");
        assertThat(policy.isMaintenance()).isTrue();
        assertThat(ClientPolicyFilter.rejection(policy, "/api/battle/start", "android", "1"))
                .isEqualTo("MAINTENANCE");
    }

    private ConfigurableApplicationContext start(Map<String, Object> environment) {
        SpringApplication application = new SpringApplication(PolicyOnlyConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        String sourceName = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
        application.addListeners(
                (ApplicationListener<ApplicationEnvironmentPreparedEvent>)
                        event ->
                                event.getEnvironment()
                                        .getPropertySources()
                                        .replace(
                                                sourceName,
                                                new SystemEnvironmentPropertySource(
                                                        sourceName, environment)));
        // Skip application.yaml and its .env import: only synthetic input is allowed here.
        return application.run(
                "--spring.config.location=optional:classpath:/client-policy-binding-empty.yaml");
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import(ClientPolicySource.class)
    static class PolicyOnlyConfiguration {}
}
